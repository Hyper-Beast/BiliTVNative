package com.kirin.bilitv.ui.player

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.core.player.PlaybackRequest
import com.kirin.bilitv.core.player.PlaybackVideoMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class PlayerSidePanelViewState(
  val videos: List<VideoSummary> = emptyList(),
  val loading: Boolean = false,
  val upVideoOrder: String = UpVideoOrderLatest,
  val upFollowed: Boolean = false,
  val upFollowLoading: Boolean = false,
)

internal class PlayerSidePanelStateHolder {
  var viewState by mutableStateOf(PlayerSidePanelViewState())
    private set

  private var activeLoadToken = 0L
  private var upVideoCache: Map<String, List<VideoSummary>> = emptyMap()

  fun clearVideos() {
    activeLoadToken += 1L
    viewState = viewState.copy(
      videos = emptyList(),
      loading = false,
    )
  }

  fun nextUpVideoOrder(): String {
    return if (viewState.upVideoOrder == UpVideoOrderLatest) UpVideoOrderHot else UpVideoOrderLatest
  }

  fun openVideoListPanel(
    coroutineScope: CoroutineScope,
    panel: PlayerPanel,
    defaultFocusedIndex: Int,
    loader: suspend () -> List<VideoSummary>,
    isActivePanel: (PlayerPanel) -> Boolean,
    updateFocusedPanelIndex: (Int) -> Unit,
    showControls: () -> Unit,
  ) {
    val loadToken = nextLoadToken()
    viewState = viewState.copy(
      videos = emptyList(),
      loading = true,
    )
    updateFocusedPanelIndex(defaultFocusedIndex)
    coroutineScope.launchVideoListPanelLoad(
      panel = panel,
      loadToken = loadToken,
      defaultFocusedIndex = defaultFocusedIndex,
      loader = loader,
      isCurrentLoad = { token, expectedPanel -> activeLoadToken == token && isActivePanel(expectedPanel) },
      applyResult = { videos, focusedIndex ->
        viewState = viewState.copy(
          videos = videos,
          loading = false,
        )
        updateFocusedPanelIndex(focusedIndex)
      },
      showControls = showControls,
    )
  }

  fun openUpVideos(
    coroutineScope: CoroutineScope,
    order: String,
    displayRequest: PlaybackRequest,
    metadata: PlaybackVideoMetadata?,
    videoRepository: VideoRepository,
    resolveDisplayMetadata: suspend () -> PlaybackVideoMetadata?,
    currentRequest: () -> PlaybackRequest,
    isActiveUpVideosPanel: () -> Boolean,
    currentPanelDescription: () -> String,
    currentFocusedPanelIndex: () -> Int,
    updateFocusedPanelIndex: (Int) -> Unit,
    showControls: () -> Unit,
  ) {
    val loadToken = nextLoadToken()
    val knownOwnerMid = displayRequest.ownerMid.takeIf { it > 0L } ?: metadata?.ownerMid ?: 0L
    val cachedVideos = upVideoCache[upVideoCacheKey(knownOwnerMid, order)].orEmpty()
      .withoutCurrentVideo(displayRequest)
    Log.i(
      PlayerUpVideosLogTag,
      "open start token=$loadToken bvid=${displayRequest.bvid} cid=${displayRequest.cid} order=$order " +
        "knownMid=$knownOwnerMid cache=${cachedVideos.size}",
    )
    viewState = viewState.copy(
      videos = cachedVideos,
      loading = cachedVideos.isEmpty(),
      upVideoOrder = order,
    )
    updateFocusedPanelIndex(if (cachedVideos.isNotEmpty()) UpPanelHeaderItemCount else UpFocusSort)
    coroutineScope.launchUpVideosPanelLoad(
      loadToken = loadToken,
      order = order,
      initialRequest = displayRequest,
      videoRepository = videoRepository,
      resolveDisplayMetadata = resolveDisplayMetadata,
      currentRequest = currentRequest,
      isCurrentUpVideosLoad = { token -> activeLoadToken == token && isActiveUpVideosPanel() },
      currentLoadDescription = { "activeToken=$activeLoadToken ${currentPanelDescription()}" },
      readCachedVideos = { key -> upVideoCache[key].orEmpty() },
      cacheVideos = { key, videos ->
        upVideoCache = upVideoCache.withBoundedUpVideoEntry(key, videos)
      },
      applyResolvedCachedVideos = { resolvedCachedVideos ->
        viewState = viewState.copy(
          videos = resolvedCachedVideos,
          loading = false,
        )
        val currentIndex = currentFocusedPanelIndex()
        updateFocusedPanelIndex(if (currentIndex < UpPanelHeaderItemCount) UpPanelHeaderItemCount else currentIndex)
      },
      applyLoadedVideos = { videos ->
        viewState = viewState.copy(
          videos = videos,
          loading = false,
        )
        val currentIndex = currentFocusedPanelIndex()
        updateFocusedPanelIndex(
          if (videos.isNotEmpty() && currentIndex < UpPanelHeaderItemCount) {
            UpPanelHeaderItemCount
          } else {
            currentIndex.coerceIn(0, (UpPanelHeaderItemCount + videos.size - 1).coerceAtLeast(0))
          },
        )
      },
      applyFollowed = { followed ->
        viewState = viewState.copy(upFollowed = followed)
      },
      showControls = showControls,
    )
  }

  fun setUpFollowStatus(
    coroutineScope: CoroutineScope,
    ownerMid: Long,
    follow: Boolean,
    videoRepository: VideoRepository,
    onFinished: () -> Unit,
  ) {
    if (ownerMid <= 0L || viewState.upFollowLoading) return
    viewState = viewState.copy(upFollowLoading = true)
    coroutineScope.launch {
      val success = runCatching {
        videoRepository.setFollowStatus(ownerMid, follow)
      }.getOrDefault(false)
      viewState = viewState.copy(
        upFollowed = if (success) follow else viewState.upFollowed,
        upFollowLoading = false,
      )
      onFinished()
    }
  }

  private fun nextLoadToken(): Long {
    activeLoadToken += 1L
    return activeLoadToken
  }
}
