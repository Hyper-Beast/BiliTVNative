package com.kirin.bilitv.ui.player

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackInfo
import com.kirin.bilitv.core.player.PlaybackQuality
import com.kirin.bilitv.core.player.PlaybackQualityPreference
import com.kirin.bilitv.core.player.PlaybackRepository
import com.kirin.bilitv.core.player.PlaybackRequest
import com.kirin.bilitv.core.player.PlaybackTrack
import com.kirin.bilitv.core.player.PlaybackVideoMetadata
import kotlinx.coroutines.CancellationException

internal data class PlayerLoadViewState(
  val activeRequest: PlaybackRequest,
  val displayRequest: PlaybackRequest,
  val screenState: PlayerScreenState,
  val metadata: PlaybackVideoMetadata?,
  val selectedQuality: PlaybackQuality?,
  val retryToken: Long,
)

internal sealed interface PlayerScreenState {
  data object Loading : PlayerScreenState
  data class Ready(
    val info: PlaybackInfo,
    val startPositionMs: Long,
  ) : PlayerScreenState
  data class Failed(val message: String) : PlayerScreenState
}

internal class PlayerLoadStateHolder(
  initialRequest: PlaybackRequest,
  private val playbackRepository: PlaybackRepository,
) {
  var viewState by mutableStateOf(
    PlayerLoadViewState(
      activeRequest = initialRequest,
      displayRequest = initialRequest,
      screenState = PlayerScreenState.Loading,
      metadata = null,
      selectedQuality = null,
      retryToken = 0L,
    ),
  )
    private set

  suspend fun load(
    codecPreference: PlaybackCodecPreference,
    qualityPreference: PlaybackQualityPreference,
    missingCidMessage: String,
    emptyTracksMessage: String,
  ): PlayerScreenState.Ready? {
    val loadRequest = viewState.activeRequest
    viewState = viewState.copy(screenState = PlayerScreenState.Loading)
    return try {
      val videoMetadata = fetchVideoMetadata(loadRequest)
      if (viewState.activeRequest != loadRequest) return null
      viewState = viewState.copy(metadata = videoMetadata)

      var effectiveRequest = loadRequest.withNextHistoryEpisodeIfNeeded(videoMetadata)
      if (effectiveRequest.canUseLatestSavedProgress()) {
        val latestSavedProgress = playbackRepository.getLatestSavedProgress(effectiveRequest.bvid)
        val latestSavedCid = latestSavedProgress?.cid?.takeIf { savedCid ->
          savedCid > 0L && videoMetadata.hasEpisodeCid(savedCid)
        }
        if (latestSavedCid != null) {
          effectiveRequest = effectiveRequest.copy(cid = latestSavedCid)
        }
      }

      val cid = effectiveRequest.cid.takeIf { it > 0L }
        ?: videoMetadata?.cid?.takeIf { it > 0L }
        ?: playbackRepository.resolveCid(effectiveRequest.bvid)
      if (cid <= 0L) {
        fail(missingCidMessage)
        return null
      }

      val resolvedRequest = effectiveRequest.withResolvedMetadata(
        metadata = videoMetadata,
        cid = cid,
      )
      if (viewState.activeRequest != loadRequest) return null
      viewState = viewState.copy(displayRequest = resolvedRequest)

      val info = playbackRepository.getPlaybackInfo(
        request = resolvedRequest,
        codecPreference = codecPreference,
        qualityPreference = qualityPreference,
      )
      if (info.videoTracks.isEmpty() || info.audioTracks.isEmpty()) {
        fail(emptyTracksMessage)
        return null
      }

      val requestedStartPositionMs = if (resolvedRequest.preferredQualityId != null || resolvedRequest.forceStartPosition) {
        resolvedRequest.startPositionMs
      } else {
        playbackRepository.getSavedProgress(info.bvid, info.cid)?.positionMs
          ?: resolvedRequest.startPositionMs
      }
      val readyState = PlayerScreenState.Ready(
        info = info,
        startPositionMs = requestedStartPositionMs,
      )
      if (viewState.activeRequest == loadRequest) {
        viewState = viewState.copy(
          screenState = readyState,
          selectedQuality = info.selectedQuality,
        )
      }
      readyState
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      fail(error.message.orEmpty())
      null
    }
  }

  fun retry() {
    viewState = viewState.copy(
      screenState = PlayerScreenState.Loading,
      retryToken = viewState.retryToken + 1L,
    )
  }

  fun fail(message: String) {
    viewState = viewState.copy(screenState = PlayerScreenState.Failed(message))
  }

  fun startPlaybackRequest(nextRequest: PlaybackRequest, clearMetadata: Boolean) {
    viewState = viewState.copy(
      activeRequest = nextRequest,
      displayRequest = nextRequest,
      screenState = PlayerScreenState.Loading,
      metadata = if (clearMetadata) null else viewState.metadata,
      selectedQuality = if (nextRequest.preferredQualityId == null) null else viewState.selectedQuality,
    )
  }

  fun selectQuality(quality: PlaybackQuality, startPositionMs: Long) {
    viewState = viewState.copy(
      activeRequest = viewState.activeRequest.copy(
        startPositionMs = startPositionMs,
        preferredQualityId = quality.id,
      ),
      screenState = PlayerScreenState.Loading,
      selectedQuality = quality,
    )
  }

  suspend fun resolveDisplayMetadata(): PlaybackVideoMetadata? {
    viewState.metadata?.let { metadata -> return metadata }
    val request = viewState.displayRequest
    val videoMetadata = try {
      playbackRepository.getVideoMetadata(request)
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Log.w(PlayerUpVideosLogTag, "metadata resolve failed bvid=${request.bvid}: ${error.toLogBrief()}")
      null
    } ?: return null
    if (viewState.displayRequest == request) {
      applyResolvedMetadata(videoMetadata)
    }
    return videoMetadata
  }

  private fun applyResolvedMetadata(videoMetadata: PlaybackVideoMetadata): PlaybackRequest {
    val resolved = viewState.displayRequest.withResolvedMetadata(
      metadata = videoMetadata,
      cid = viewState.displayRequest.cid.takeIf { it > 0L } ?: videoMetadata.cid,
    )
    viewState = viewState.copy(
      metadata = videoMetadata,
      displayRequest = resolved,
    )
    return resolved
  }

  private suspend fun fetchVideoMetadata(request: PlaybackRequest): PlaybackVideoMetadata? {
    return try {
      playbackRepository.getVideoMetadata(request)
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      null
    }
  }
}

private fun PlaybackRequest.withNextHistoryEpisodeIfNeeded(
  metadata: PlaybackVideoMetadata?,
): PlaybackRequest {
  if (!advanceToNextHistoryEpisode || metadata == null) return this
  val currentIndex = metadata.pages.indexOfFirst { episode ->
    (historyPage > 0 && episode.page == historyPage) || (cid > 0L && episode.cid == cid)
  }
  val nextEpisode = metadata.pages.getOrNull(currentIndex + 1) ?: return this
  return copy(
    cid = nextEpisode.cid,
    startPositionMs = 0L,
    forceStartPosition = true,
    historyPage = nextEpisode.page,
    advanceToNextHistoryEpisode = false,
  )
}

private fun PlaybackRequest.canUseLatestSavedProgress(): Boolean {
  return bvid.isNotBlank() && preferredQualityId == null && !forceStartPosition
}

private fun PlaybackVideoMetadata?.hasEpisodeCid(cid: Long): Boolean {
  if (cid <= 0L) return false
  if (this == null) return true
  return pages.isEmpty() || pages.any { episode -> episode.cid == cid }
}

private fun PlaybackRequest.withResolvedMetadata(
  metadata: PlaybackVideoMetadata?,
  cid: Long,
): PlaybackRequest {
  return copy(
    cid = cid,
    aid = aid.takeIf { it > 0L } ?: metadata?.aid ?: 0L,
    title = title.ifBlank { metadata?.title.orEmpty() },
    ownerName = ownerName.ifBlank { metadata?.ownerName.orEmpty() },
    ownerFace = ownerFace.ifBlank { metadata?.ownerFace.orEmpty() },
    ownerMid = ownerMid.takeIf { it > 0L } ?: metadata?.ownerMid ?: 0L,
    viewCount = viewCount.takeIf { it > 0 } ?: metadata?.viewCount ?: 0,
    danmakuCount = danmakuCount.takeIf { it > 0 } ?: metadata?.danmakuCount ?: 0,
    pubdate = pubdate.takeIf { it > 0L } ?: metadata?.pubdate ?: 0L,
  )
}

internal fun PlaybackTrack.codecLabel(): String {
  return codecs.codecLabelFromCodecs().orEmpty()
}

internal fun String.codecLabelFromCodecs(): String? {
  return when {
    contains("av01", ignoreCase = true) -> "AV1"
    contains("hev", ignoreCase = true) || contains("hvc", ignoreCase = true) -> "H.265"
    contains("avc", ignoreCase = true) -> "H.264"
    else -> null
  }
}
