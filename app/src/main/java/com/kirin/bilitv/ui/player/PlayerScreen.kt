package com.kirin.bilitv.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.model.isWatchCompleted
import com.kirin.bilitv.core.model.shouldAdvanceToNextHistoryEpisode
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.core.player.AirJumpSegment
import com.kirin.bilitv.core.player.BiliMediaDataSourceFactory
import com.kirin.bilitv.core.player.DanmakuEntry
import com.kirin.bilitv.core.player.DanmakuSettings
import com.kirin.bilitv.core.player.DanmakuSettingsStore
import com.kirin.bilitv.core.player.PlaybackInfo
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackQualityPreference
import com.kirin.bilitv.core.player.PlaybackQuality
import com.kirin.bilitv.core.player.PlaybackRepository
import com.kirin.bilitv.core.player.PlaybackRequest
import com.kirin.bilitv.core.player.PlaybackTrack
import com.kirin.bilitv.core.player.PlaybackVideoMetadata
import com.kirin.bilitv.core.player.VideoshotData
import com.kirin.bilitv.core.player.createTvPlaybackLoadControl
import com.kirin.bilitv.ui.common.ClockOverlay
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.i18n.LocalChineseTextConverter
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
fun PlayerScreen(
  request: PlaybackRequest,
  videoRepository: VideoRepository,
  playbackRepository: PlaybackRepository,
  danmakuSettingsStore: DanmakuSettingsStore,
  playbackHttpClient: OkHttpClient,
  playbackCodecPreference: PlaybackCodecPreference,
  playbackQualityPreference: PlaybackQualityPreference,
  seekPreviewSpritesEnabled: Boolean,
  airJumpAssistantEnabled: Boolean,
  confirmPlaybackExit: Boolean,
  autoPlayNextEpisode: Boolean,
  autoPlayRelatedVideo: Boolean,
  autoReturnHomeOnCompletion: Boolean,
  showClock: Boolean,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val rootView = LocalView.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val textConverter = LocalChineseTextConverter.current
  var activeRequest by remember(request) { mutableStateOf(request) }
  var displayRequest by remember(request) { mutableStateOf(request) }
  var playerState by remember(activeRequest.bvid, activeRequest.cid, activeRequest.preferredQualityId) {
    mutableStateOf<PlayerScreenState>(PlayerScreenState.Loading)
  }
  var metadata by remember(request) { mutableStateOf<PlaybackVideoMetadata?>(null) }
  var sidePanelVideos by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
  var sidePanelLoading by remember { mutableStateOf(false) }
  var sidePanelLoadToken by remember { mutableLongStateOf(0L) }
  var upVideoCache by remember { mutableStateOf<Map<String, List<VideoSummary>>>(emptyMap()) }
  var upVideoOrder by remember { mutableStateOf(UpVideoOrderLatest) }
  var upFollowed by remember { mutableStateOf(false) }
  var upFollowLoading by remember { mutableStateOf(false) }
  var showUnfollowConfirm by remember { mutableStateOf(false) }
  var unfollowConfirmFocusedConfirm by remember { mutableStateOf(false) }
  var onlineCountText by remember { mutableStateOf("") }
  var currentCodecText by remember { mutableStateOf("") }
  var danmakuEntries by remember { mutableStateOf<List<DanmakuEntry>>(emptyList()) }
  var videoshotData by remember { mutableStateOf<VideoshotData?>(null) }
  var videoshotSprites by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
  var airJumpSegments by remember { mutableStateOf<List<AirJumpSegment>>(emptyList()) }
  var warnedAirJumpIds by remember { mutableStateOf(emptySet<String>()) }
  var skippedAirJumpIds by remember { mutableStateOf(emptySet<String>()) }
  var lastAirJumpPositionMs by remember { mutableLongStateOf(0L) }
  var controlsVisible by remember { mutableStateOf(false) }
  var progressFocused by remember { mutableStateOf(false) }
  var focusedControl by remember { mutableStateOf(PlayerControl.Episodes) }
  var activePanel by remember { mutableStateOf(PlayerPanel.None) }
  var focusedPanelIndex by remember { mutableIntStateOf(0) }
  var selectedQuality by remember { mutableStateOf<PlaybackQuality?>(null) }
  val storedDanmakuSettings by danmakuSettingsStore.settings.collectAsState(initial = DanmakuSettings())
  var danmakuSettings by remember { mutableStateOf(DanmakuSettings()) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var previewPositionMs by remember { mutableStateOf<Long?>(null) }
  var positionMs by remember { mutableLongStateOf(0L) }
  var durationMs by remember { mutableLongStateOf(0L) }
  var bufferedPercentage by remember { mutableLongStateOf(0L) }
  var playbackPaused by remember { mutableStateOf(false) }
  var playerActuallyPlaying by remember { mutableStateOf(false) }
  var retryKey by remember { mutableLongStateOf(0L) }
  var lastPlaybackExitBackPressMs by remember { mutableLongStateOf(0L) }
  var playbackExitConfirmToast by remember { mutableStateOf<Toast?>(null) }
  var playbackCompletionToast by remember { mutableStateOf<Toast?>(null) }
  var completionReported by remember { mutableStateOf(false) }
  var completionActionToken by remember { mutableLongStateOf(0L) }
  val controlsFocusRequester = remember { FocusRequester() }
  val player = remember {
    ExoPlayer.Builder(context)
      .setLoadControl(createTvPlaybackLoadControl())
      .build()
  }
  val playbackWakeLock = remember(context) {
    context.applicationContext.createPlayerWakeLock()
  }

  LaunchedEffect(storedDanmakuSettings) {
    danmakuSettings = storedDanmakuSettings
  }

  DisposableEffect(Unit) {
    onDispose {
      playbackExitConfirmToast?.cancel()
      playbackCompletionToast?.cancel()
    }
  }

  fun showPlaybackExitConfirmToast() {
    playbackExitConfirmToast?.cancel()
    playbackExitConfirmToast = Toast.makeText(context, context.getString(R.string.player_exit_confirm_toast), Toast.LENGTH_SHORT).also { toast ->
      toast.show()
    }
  }

  fun cancelPlaybackExitConfirmToast() {
    playbackExitConfirmToast?.cancel()
    playbackExitConfirmToast = null
  }

  fun showPlaybackCompletionToast(message: String) {
    playbackExitConfirmToast?.cancel()
    playbackCompletionToast?.cancel()
    playbackCompletionToast = Toast.makeText(context, message, Toast.LENGTH_LONG).also { toast ->
      toast.show()
    }
  }

  fun cancelPlaybackCompletionToast() {
    playbackCompletionToast?.cancel()
    playbackCompletionToast = null
  }

  fun cancelPendingCompletionAction() {
    completionActionToken += 1L
    cancelPlaybackCompletionToast()
  }

  fun acquirePlaybackWakeLock() {
    runCatching {
      if (playbackWakeLock?.isHeld != true) {
        playbackWakeLock?.acquire()
      }
    }
  }

  fun releasePlaybackWakeLock() {
    runCatching {
      if (playbackWakeLock?.isHeld == true) {
        playbackWakeLock.release()
      }
    }
  }

  fun showControls() {
    if (!controlsVisible && activePanel == PlayerPanel.None) {
      focusedControl = PlayerControl.Episodes
      progressFocused = false
    }
    controlsVisible = true
    runCatching { controlsFocusRequester.requestFocus() }
  }

  fun persistDanmakuSettings(next: DanmakuSettings) {
    danmakuSettings = next
    coroutineScope.launch {
      danmakuSettingsStore.setSettings(next)
    }
  }

  fun hideControlsForPlayback() {
    if (showUnfollowConfirm) return
    activePanel = PlayerPanel.None
    previewPositionMs = null
    progressFocused = false
    controlsVisible = false
  }

  fun maxDurationMs(): Long {
    return player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: durationMs.coerceAtLeast(0L)
  }

  fun alignPreviewTarget(targetMs: Long, currentPreviewMs: Long?, deltaMs: Long, maxDurationMs: Long): Long {
    val videoshot = videoshotData ?: return targetMs
    if (!seekPreviewSpritesEnabled) return targetMs
    var aligned = videoshot.closestTimestampMs(targetMs).coerceIn(0L, maxDurationMs)
    if (currentPreviewMs != null && deltaMs > 0 && aligned <= currentPreviewMs && currentPreviewMs < maxDurationMs) {
      aligned = videoshot.closestTimestampMs((currentPreviewMs + 1_000L).coerceIn(0L, maxDurationMs))
        .coerceIn(0L, maxDurationMs)
    }
    if (currentPreviewMs != null && deltaMs < 0 && aligned >= currentPreviewMs && currentPreviewMs > 0L) {
      aligned = videoshot.closestTimestampMs((currentPreviewMs - 1_000L).coerceIn(0L, maxDurationMs))
        .coerceIn(0L, maxDurationMs)
    }
    return aligned
  }

  fun commitPreviewSeek(revealControls: Boolean = controlsVisible || progressFocused) {
    val target = previewPositionMs ?: return
    player.seekTo(target.coerceIn(0L, maxDurationMs().takeIf { it > 0L } ?: Long.MAX_VALUE))
    positionMs = target
    previewPositionMs = null
    if (revealControls) {
      showControls()
    } else {
      progressFocused = false
    }
  }

  fun updatePreviewSeek(deltaMs: Long, revealControls: Boolean = controlsVisible || progressFocused) {
    val maxDuration = maxDurationMs().takeIf { it > 0L } ?: Long.MAX_VALUE
    val basePosition = previewPositionMs ?: player.currentPosition.takeIf { it >= 0L } ?: positionMs
    val target = (basePosition + deltaMs).coerceIn(0L, maxDuration)
    previewPositionMs = alignPreviewTarget(
      targetMs = target,
      currentPreviewMs = previewPositionMs,
      deltaMs = deltaMs,
      maxDurationMs = maxDuration,
    )
    activePanel = PlayerPanel.None
    if (revealControls) {
      progressFocused = true
      showControls()
    } else {
      progressFocused = false
    }
  }

  fun handleAirJumpPosition(currentPositionMs: Long) {
    if (!airJumpAssistantEnabled || previewPositionMs != null || airJumpSegments.isEmpty()) {
      lastAirJumpPositionMs = currentPositionMs
      return
    }

    if (currentPositionMs < lastAirJumpPositionMs - AirJumpRewindResetThresholdMs) {
      val resetIds = airJumpSegments
        .filter { segment -> currentPositionMs < segment.startMs - AirJumpRewindResetLeadMs }
        .map(AirJumpSegment::id)
        .toSet()
      if (resetIds.isNotEmpty()) {
        warnedAirJumpIds = warnedAirJumpIds - resetIds
        skippedAirJumpIds = skippedAirJumpIds - resetIds
      }
    }
    lastAirJumpPositionMs = currentPositionMs

    val hitSegment = airJumpSegments.firstOrNull { segment ->
      segment.id !in skippedAirJumpIds &&
        currentPositionMs >= segment.startMs &&
        currentPositionMs < segment.endMs
    }
    if (hitSegment != null) {
      cancelPlaybackCompletionToast()
      val targetPositionMs = hitSegment.endMs.coerceIn(
        0L,
        maxDurationMs().takeIf { it > 0L } ?: hitSegment.endMs,
      )
      skippedAirJumpIds = skippedAirJumpIds + hitSegment.id
      warnedAirJumpIds = warnedAirJumpIds + hitSegment.id
      player.seekTo(targetPositionMs)
      positionMs = targetPositionMs
      val duration = maxDurationMs().takeIf { it > 0L } ?: 0L
      if (duration <= 0L || targetPositionMs < duration - AirJumpCompletionToastSuppressMs) {
        Toast.makeText(context, context.getString(R.string.player_air_jump_skipped), Toast.LENGTH_SHORT).show()
      }
      return
    }

    val warningSegment = airJumpSegments.firstOrNull { segment ->
      segment.id !in warnedAirJumpIds &&
        segment.id !in skippedAirJumpIds &&
        currentPositionMs >= segment.startMs - AirJumpWarningLeadMs &&
        currentPositionMs < segment.startMs
    }
    if (warningSegment != null) {
      warnedAirJumpIds = warnedAirJumpIds + warningSegment.id
      Toast.makeText(context, context.getString(R.string.player_air_jump_will_skip), Toast.LENGTH_LONG).show()
    }
  }

  suspend fun saveProgressNow() {
    val state = playerState as? PlayerScreenState.Ready ?: return
    val currentPositionMs = player.currentPosition.takeIf { it >= 0L } ?: 0L
    val currentDurationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: durationMs
    playbackRepository.saveProgress(
      bvid = state.info.bvid,
      cid = state.info.cid,
      positionMs = currentPositionMs,
      durationMs = currentDurationMs,
    )
  }

  suspend fun reportProgressNow(overrideProgressSeconds: Int? = null) {
    val state = playerState as? PlayerScreenState.Ready ?: return
    val progressSeconds = overrideProgressSeconds
      ?: ((player.currentPosition.takeIf { it >= 0L } ?: positionMs).coerceAtLeast(0L) / 1000L).toInt()
    playbackRepository.reportProgress(
      bvid = state.info.bvid,
      cid = state.info.cid,
      progressSeconds = progressSeconds,
    )
  }

  suspend fun saveAndReportProgressNow(overrideProgressSeconds: Int? = null) {
    val progressOverride = overrideProgressSeconds ?: if (completionReported) CompletedProgressSeconds else null
    if (progressOverride == null) {
      saveProgressNow()
    }
    reportProgressNow(progressOverride)
  }

  fun saveProgress() {
    coroutineScope.launch {
      saveProgressNow()
    }
  }

  fun saveAndReportProgress(overrideProgressSeconds: Int? = null) {
    coroutineScope.launch {
      saveAndReportProgressNow(overrideProgressSeconds)
    }
  }

  fun finishPlayer(onFinished: () -> Unit) {
    cancelPlaybackExitConfirmToast()
    cancelPendingCompletionAction()
    coroutineScope.launch {
      saveAndReportProgressNow()
      onFinished()
    }
  }

  fun exitPlayer() {
    finishPlayer(onBack)
  }

  fun requestExitPlayer() {
    if (!confirmPlaybackExit) {
      exitPlayer()
      return
    }
    val now = SystemClock.elapsedRealtime()
    if (now - lastPlaybackExitBackPressMs <= ExitConfirmWindowMs) {
      exitPlayer()
    } else {
      lastPlaybackExitBackPressMs = now
      showPlaybackExitConfirmToast()
    }
  }

  fun openPanel(panel: PlayerPanel) {
    if (panel != PlayerPanel.UpVideos) {
      showUnfollowConfirm = false
      unfollowConfirmFocusedConfirm = false
    }
    activePanel = panel
    focusedPanelIndex = when (panel) {
      PlayerPanel.Quality -> selectedQuality?.let { quality ->
        (playerState as? PlayerScreenState.Ready)?.info?.qualities?.indexOfFirst { it.id == quality.id }
      }?.takeIf { it >= 0 } ?: 0
      PlayerPanel.Speed -> PlayerSpeedOptions.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 2
      PlayerPanel.Episodes -> metadata?.pages
        ?.indexOfFirst { episode -> episode.cid == displayRequest.cid }
        ?.takeIf { it >= 0 } ?: 0
      else -> 0
    }
    progressFocused = false
    showControls()
  }

  fun applyResolvedMetadata(videoMetadata: PlaybackVideoMetadata): PlaybackRequest {
    metadata = videoMetadata
    val resolved = displayRequest.withResolvedMetadata(
      metadata = videoMetadata,
      cid = displayRequest.cid.takeIf { it > 0L } ?: videoMetadata.cid,
    )
    displayRequest = resolved
    return resolved
  }

  suspend fun resolveDisplayMetadata(): PlaybackVideoMetadata? {
    metadata?.let { return it }
    return runCatching { playbackRepository.getVideoMetadata(displayRequest) }
      .getOrNull()
      ?.also(::applyResolvedMetadata)
  }

  fun openVideoListPanel(
    panel: PlayerPanel,
    defaultFocusedIndex: Int = 0,
    loader: suspend () -> List<VideoSummary>,
  ) {
    val loadToken = ++sidePanelLoadToken
    openPanel(panel)
    sidePanelVideos = emptyList()
    sidePanelLoading = true
    focusedPanelIndex = defaultFocusedIndex
    coroutineScope.launch {
      val videos = runCatching { loader() }.getOrDefault(emptyList())
      if (sidePanelLoadToken != loadToken || activePanel != panel) {
        return@launch
      }
      sidePanelVideos = videos
      sidePanelLoading = false
      focusedPanelIndex = if (videos.isNotEmpty()) defaultFocusedIndex else 0
      showControls()
    }
  }

  fun openUpVideos(order: String = UpVideoOrderLatest) {
    val loadToken = ++sidePanelLoadToken
    val knownOwnerMid = displayRequest.ownerMid.takeIf { it > 0L } ?: metadata?.ownerMid ?: 0L
    val cachedVideos = upVideoCache[upVideoCacheKey(knownOwnerMid, order)].orEmpty()
      .withoutCurrentVideo(displayRequest)
    upVideoOrder = order
    openPanel(PlayerPanel.UpVideos)
    sidePanelVideos = cachedVideos
    sidePanelLoading = cachedVideos.isEmpty()
    focusedPanelIndex = if (cachedVideos.isNotEmpty()) UpPanelHeaderItemCount else UpFocusSort
    coroutineScope.launch {
      val resolvedMetadata = resolveDisplayMetadata()
      val ownerMid = displayRequest.ownerMid.takeIf { it > 0L } ?: resolvedMetadata?.ownerMid ?: 0L
      val cacheKey = upVideoCacheKey(ownerMid, order)
      val resolvedCachedVideos = upVideoCache[cacheKey].orEmpty()
        .withoutCurrentVideo(displayRequest)
      if (sidePanelLoadToken == loadToken && activePanel == PlayerPanel.UpVideos && resolvedCachedVideos.isNotEmpty()) {
        sidePanelVideos = resolvedCachedVideos
        sidePanelLoading = false
        focusedPanelIndex = if (focusedPanelIndex < UpPanelHeaderItemCount) UpPanelHeaderItemCount else focusedPanelIndex
        showControls()
      }
      val videos = runCatching {
        videoRepository.getSpaceVideos(mid = ownerMid, order = order)
      }.getOrDefault(emptyList())
      if (sidePanelLoadToken != loadToken || activePanel != PlayerPanel.UpVideos) {
        return@launch
      }
      val nextVideos = videos.ifEmpty { upVideoCache[cacheKey].orEmpty() }
        .withoutCurrentVideo(displayRequest)
      if (videos.isNotEmpty()) {
        upVideoCache = upVideoCache.withBoundedEntry(cacheKey, videos)
      }
      sidePanelVideos = nextVideos
      sidePanelLoading = false
      focusedPanelIndex = if (nextVideos.isNotEmpty() && focusedPanelIndex < UpPanelHeaderItemCount) {
        UpPanelHeaderItemCount
      } else {
        focusedPanelIndex.coerceIn(0, (UpPanelHeaderItemCount + nextVideos.size - 1).coerceAtLeast(0))
      }
      showControls()

      val followed = runCatching {
        videoRepository.checkFollowStatus(ownerMid)
      }.getOrDefault(false)
      if (sidePanelLoadToken != loadToken || activePanel != PlayerPanel.UpVideos) {
        return@launch
      }
      upFollowed = followed
      showControls()
    }
  }

  fun toggleUpOrder() {
    openUpVideos(if (upVideoOrder == UpVideoOrderLatest) UpVideoOrderHot else UpVideoOrderLatest)
  }

  fun setUpFollowStatus(follow: Boolean) {
    val ownerMid = displayRequest.ownerMid.takeIf { it > 0L } ?: metadata?.ownerMid ?: 0L
    if (ownerMid <= 0L || upFollowLoading) return
    upFollowLoading = true
    coroutineScope.launch {
      val success = runCatching {
        videoRepository.setFollowStatus(ownerMid, follow)
      }.getOrDefault(false)
      if (success) {
        upFollowed = follow
      }
      upFollowLoading = false
      showUnfollowConfirm = false
      unfollowConfirmFocusedConfirm = false
      showControls()
    }
  }

  fun closePanelOrControls() {
    if (completionReported) {
      cancelPendingCompletionAction()
    }
    when {
      showUnfollowConfirm -> {
        showUnfollowConfirm = false
        unfollowConfirmFocusedConfirm = false
      }
      previewPositionMs != null -> {
        previewPositionMs = null
        progressFocused = false
      }
      activePanel != PlayerPanel.None -> openPanel(PlayerPanel.None)
      controlsVisible -> controlsVisible = false
      else -> requestExitPlayer()
    }
  }

  fun togglePlayback() {
    if (completionReported) {
      cancelPendingCompletionAction()
    }
    if (player.isPlaying) {
      player.pause()
      playbackPaused = true
      showControls()
      saveAndReportProgress()
    } else {
      player.play()
      playbackPaused = false
      hideControlsForPlayback()
    }
  }

  fun startPlaybackRequest(nextRequest: PlaybackRequest, clearMetadata: Boolean) {
    if (clearMetadata) {
      metadata = null
    }
    cancelPlaybackCompletionToast()
    completionActionToken += 1L
    onlineCountText = ""
    sidePanelVideos = emptyList()
    activePanel = PlayerPanel.None
    progressFocused = false
    previewPositionMs = null
    completionReported = false
    selectedQuality = if (nextRequest.preferredQualityId == null) null else selectedQuality
    activeRequest = nextRequest
    displayRequest = nextRequest
    controlsVisible = false
  }

  fun nextEpisodeRequest(videoMetadata: PlaybackVideoMetadata?): PlaybackRequest? {
    val pages = videoMetadata?.pages.orEmpty()
    val currentIndex = pages.indexOfFirst { episode ->
      episode.cid == displayRequest.cid || (displayRequest.historyPage > 0 && episode.page == displayRequest.historyPage)
    }
    val nextEpisode = pages.getOrNull(currentIndex + 1) ?: return null
    return displayRequest.copy(
      cid = nextEpisode.cid,
      startPositionMs = 0L,
      preferredQualityId = selectedQuality?.id,
      forceStartPosition = true,
      historyPage = nextEpisode.page,
      advanceToNextHistoryEpisode = false,
    )
  }

  fun scheduleCompletionAction() {
    val actionToken = ++completionActionToken
    coroutineScope.launch {
      if (autoPlayNextEpisode) {
        val videoMetadata = resolveDisplayMetadata()
        if (completionActionToken != actionToken || !completionReported) return@launch
        val nextRequest = nextEpisodeRequest(videoMetadata)
        if (nextRequest != null) {
          val nextTitle = videoMetadata?.pages
            ?.firstOrNull { episode -> episode.cid == nextRequest.cid }
            ?.title
            .orEmpty()
            .ifBlank { nextRequest.title }
          showPlaybackCompletionToast(
            context.getString(R.string.player_completion_next_episode_toast, textConverter.convert(nextTitle)),
          )
          delay(CompletionActionDelayMs)
          if (completionActionToken == actionToken && completionReported) {
            startPlaybackRequest(nextRequest, clearMetadata = false)
          }
          return@launch
        }
      }

      if (autoPlayRelatedVideo) {
        val relatedVideo = runCatching {
          videoRepository.getRelatedVideos(displayRequest.bvid)
            .firstOrNull { video -> !video.bvid.equals(displayRequest.bvid, ignoreCase = true) }
        }.getOrNull()
        if (completionActionToken != actionToken || !completionReported) return@launch
        if (relatedVideo != null) {
          showPlaybackCompletionToast(
            context.getString(R.string.player_completion_related_toast, textConverter.convert(relatedVideo.title)),
          )
          delay(CompletionActionDelayMs)
          if (completionActionToken == actionToken && completionReported) {
            startPlaybackRequest(relatedVideo.toPlaybackRequest(), clearMetadata = true)
          }
          return@launch
        }
      }

      if (autoReturnHomeOnCompletion) {
        showPlaybackCompletionToast(context.getString(R.string.player_completion_home_toast))
        delay(CompletionActionDelayMs)
        if (completionActionToken == actionToken && completionReported) {
          cancelPlaybackCompletionToast()
          exitPlayer()
        }
      }
    }
  }

  fun reportPlaybackCompleted() {
    if (completionReported) return
    completionReported = true
    val completedDurationMs = maxDurationMs()
    if (completedDurationMs > 0L) {
      positionMs = completedDurationMs
      durationMs = completedDurationMs
    }
    controlsVisible = true
    playbackPaused = true
    saveAndReportProgress(CompletedProgressSeconds)
    scheduleCompletionAction()
  }

  fun panelItemCount(): Int {
    val info = (playerState as? PlayerScreenState.Ready)?.info
    return when (activePanel) {
      PlayerPanel.Main -> 3
      PlayerPanel.Quality -> info?.qualities?.size?.coerceAtLeast(1) ?: 1
      PlayerPanel.Danmaku -> 7
      PlayerPanel.Speed -> PlayerSpeedOptions.size
      PlayerPanel.Episodes -> metadata?.pages?.size ?: 0
      PlayerPanel.UpVideos -> UpPanelHeaderItemCount + sidePanelVideos.size
      PlayerPanel.RelatedVideos -> if (sidePanelLoading) 0 else sidePanelVideos.size
      PlayerPanel.None -> 0
    }
  }

  fun changePanelFocus(delta: Int) {
    val count = panelItemCount()
    if (count <= 0) return
    focusedPanelIndex = if (activePanel == PlayerPanel.UpVideos) {
      when {
        delta > 0 && focusedPanelIndex < UpPanelHeaderItemCount -> {
          if (sidePanelVideos.isNotEmpty()) UpPanelHeaderItemCount else focusedPanelIndex
        }
        delta < 0 && focusedPanelIndex == UpPanelHeaderItemCount -> UpFocusSort
        else -> (focusedPanelIndex + delta).coerceIn(0, count - 1)
      }
    } else {
      (focusedPanelIndex + delta).coerceIn(0, count - 1)
    }
    showControls()
  }

  fun activateFocusedPanelItem() {
    val info = (playerState as? PlayerScreenState.Ready)?.info ?: return
    when (activePanel) {
      PlayerPanel.Main -> when (focusedPanelIndex) {
        0 -> openPanel(PlayerPanel.Quality)
        1 -> openPanel(PlayerPanel.Danmaku)
        2 -> openPanel(PlayerPanel.Speed)
      }
      PlayerPanel.Quality -> {
        val quality = info.qualities.getOrNull(focusedPanelIndex) ?: return
        selectedQuality = quality
        activeRequest = activeRequest.copy(
          startPositionMs = player.currentPosition.takeIf { it > 0L } ?: positionMs,
          preferredQualityId = quality.id,
        )
      }
      PlayerPanel.Danmaku -> {
        when (focusedPanelIndex) {
          0 -> persistDanmakuSettings(danmakuSettings.copy(enabled = !danmakuSettings.enabled))
          5 -> persistDanmakuSettings(danmakuSettings.copy(allowTop = !danmakuSettings.allowTop))
          6 -> persistDanmakuSettings(danmakuSettings.copy(allowBottom = !danmakuSettings.allowBottom))
          else -> Unit
        }
      }
      PlayerPanel.Speed -> {
        playbackSpeed = PlayerSpeedOptions.getOrNull(focusedPanelIndex) ?: playbackSpeed
        player.setPlaybackSpeed(playbackSpeed)
      }
      PlayerPanel.Episodes -> {
        val episode = metadata?.pages?.getOrNull(focusedPanelIndex) ?: return
        coroutineScope.launch {
          saveAndReportProgressNow()
          val nextRequest = displayRequest.copy(
            cid = episode.cid,
            startPositionMs = 0L,
            preferredQualityId = selectedQuality?.id,
            forceStartPosition = true,
          )
          startPlaybackRequest(nextRequest, clearMetadata = false)
        }
        return
      }
      PlayerPanel.UpVideos -> {
        when (focusedPanelIndex) {
          UpFocusSort -> toggleUpOrder()
          UpFocusFollow -> {
            if (upFollowed) {
              showUnfollowConfirm = true
              unfollowConfirmFocusedConfirm = false
            } else {
              setUpFollowStatus(true)
            }
          }
          else -> {
            val video = sidePanelVideos.getOrNull(focusedPanelIndex - UpPanelHeaderItemCount) ?: return
            coroutineScope.launch {
              saveAndReportProgressNow()
              startPlaybackRequest(video.toPlaybackRequest(), clearMetadata = true)
            }
            return
          }
        }
      }
      PlayerPanel.RelatedVideos -> {
        val video = sidePanelVideos.getOrNull(focusedPanelIndex) ?: return
        coroutineScope.launch {
          saveAndReportProgressNow()
          startPlaybackRequest(video.toPlaybackRequest(), clearMetadata = true)
        }
        return
      }
      PlayerPanel.None -> Unit
    }
    showControls()
  }

  fun adjustFocusedDanmakuSetting(delta: Int): Boolean {
    if (activePanel != PlayerPanel.Danmaku) return false
    val next = when (focusedPanelIndex) {
      1 -> danmakuSettings.copy(opacity = stepFloat(danmakuSettings.opacity, DanmakuOpacityOptions, delta))
      2 -> danmakuSettings.copy(fontSize = stepInt(danmakuSettings.fontSize, DanmakuFontSizeOptions, delta))
      3 -> danmakuSettings.copy(area = stepFloat(danmakuSettings.area, DanmakuAreaOptions, delta))
      4 -> danmakuSettings.copy(speed = stepInt(danmakuSettings.speed, DanmakuSpeedOptions, delta))
      else -> return false
    }
    persistDanmakuSettings(next)
    showControls()
    return true
  }

  fun moveFocusedControl(delta: Int) {
    val controls = PlayerControl.entries
    val next = (controls.indexOf(focusedControl) + delta).coerceIn(0, controls.lastIndex)
    focusedControl = controls[next]
    progressFocused = false
    showControls()
  }

  fun activateFocusedControl() {
    when (focusedControl) {
      PlayerControl.Settings -> openPanel(PlayerPanel.Main)
      PlayerControl.Episodes -> openPanel(PlayerPanel.Episodes)
      PlayerControl.Up -> openUpVideos(UpVideoOrderLatest)
      PlayerControl.Related -> {
        openVideoListPanel(
          panel = PlayerPanel.RelatedVideos,
          defaultFocusedIndex = 0,
        ) {
          videoRepository.getRelatedVideos(displayRequest.bvid)
        }
      }
    }
  }

  fun activateFocusedOverlay() {
    when {
      previewPositionMs != null -> commitPreviewSeek()
      activePanel != PlayerPanel.None -> activateFocusedPanelItem()
      playbackPaused && !completionReported -> togglePlayback()
      controlsVisible -> activateFocusedControl()
      else -> showControls()
    }
  }

  BackHandler(onBack = ::closePanelOrControls)

  DisposableEffect(context, rootView, playbackWakeLock) {
    val activity = context.findActivity()
    val previousKeepScreenOn = rootView.keepScreenOn
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    rootView.keepScreenOn = true
    acquirePlaybackWakeLock()

    onDispose {
      rootView.keepScreenOn = previousKeepScreenOn
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      releasePlaybackWakeLock()
    }
  }

  DisposableEffect(player) {
    val listener = object : Player.Listener {
      override fun onPlayerError(error: PlaybackException) {
        playerState = PlayerScreenState.Failed(error.message.orEmpty())
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        playerActuallyPlaying = isPlaying
        val pausedByUser = !isPlaying && !player.playWhenReady && player.playbackState != Player.STATE_ENDED
        playbackPaused = pausedByUser
        when {
          pausedByUser -> showControls()
          isPlaying -> hideControlsForPlayback()
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
          reportPlaybackCompleted()
        }
      }
    }
    player.addListener(listener)
    onDispose {
      player.removeListener(listener)
      player.release()
    }
  }

  DisposableEffect(lifecycleOwner, player, playerState) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> {
          saveAndReportProgress()
          releasePlaybackWakeLock()
          player.pause()
        }
        Lifecycle.Event.ON_RESUME -> {
          acquirePlaybackWakeLock()
          if (playerState is PlayerScreenState.Ready) {
            player.play()
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(activeRequest, playbackCodecPreference, playbackQualityPreference, retryKey) {
    playerState = PlayerScreenState.Loading
    previewPositionMs = null
    currentCodecText = ""
    danmakuEntries = emptyList()
    airJumpSegments = emptyList()
    warnedAirJumpIds = emptySet()
    skippedAirJumpIds = emptySet()
    lastAirJumpPositionMs = 0L
    playerActuallyPlaying = false
    completionReported = false
    player.clearMediaItems()
    val videoMetadata = runCatching {
      playbackRepository.getVideoMetadata(activeRequest)
    }.getOrNull()
    metadata = videoMetadata
    var effectiveRequest = activeRequest.withNextHistoryEpisodeIfNeeded(videoMetadata)
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
      playerState = PlayerScreenState.Failed(context.getString(R.string.player_error_missing_cid))
      return@LaunchedEffect
    }
    val resolvedRequest = effectiveRequest.withResolvedMetadata(
      metadata = videoMetadata,
      cid = cid,
    )
    displayRequest = resolvedRequest
    playerState = try {
      val info = playbackRepository.getPlaybackInfo(
        request = resolvedRequest,
        codecPreference = playbackCodecPreference,
        qualityPreference = playbackQualityPreference,
      )
      if (info.videoTracks.isEmpty() || info.audioTracks.isEmpty()) {
        PlayerScreenState.Failed(context.getString(R.string.player_error_empty_tracks))
      } else {
        selectedQuality = info.selectedQuality
        currentCodecText = info.videoTracks.firstOrNull()?.codecLabel().orEmpty()
        val requestedStartPositionMs = if (resolvedRequest.preferredQualityId != null || resolvedRequest.forceStartPosition) {
          resolvedRequest.startPositionMs
        } else {
          playbackRepository.getSavedProgress(info.bvid, info.cid)?.positionMs
            ?: resolvedRequest.startPositionMs
        }
        val startPositionMs = requestedStartPositionMs
        val mediaSource = DashMediaSource.Factory(
          DefaultDataSource.Factory(
            context,
            BiliMediaDataSourceFactory(
              client = playbackHttpClient,
              headers = info.headers,
            ).create(),
          ),
        ).createMediaSource(buildDashMediaItem(info))
        player.setMediaSource(mediaSource)
        player.prepare()
        player.setPlaybackSpeed(playbackSpeed)
        if (startPositionMs > 0L) {
          player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
        playbackPaused = false
        PlayerScreenState.Ready(info)
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      PlayerScreenState.Failed(error.message.orEmpty())
    }
  }

  LaunchedEffect(player, playerState) {
    while (isActive) {
      val currentPositionMs = player.currentPosition.takeIf { it >= 0L } ?: 0L
      positionMs = currentPositionMs
      durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: durationMs
      bufferedPercentage = player.bufferedPercentage.toLong()
      currentCodecText = player.videoFormat?.codecs?.codecLabelFromCodecs()
        ?: (playerState as? PlayerScreenState.Ready)?.info?.videoTracks?.firstOrNull()?.codecLabel().orEmpty()
      if (playerState is PlayerScreenState.Ready) {
        handleAirJumpPosition(currentPositionMs)
      }
      delay(BiliMotion.PlayerProgressUpdateMs)
    }
  }

  LaunchedEffect(airJumpAssistantEnabled, displayRequest.bvid, displayRequest.cid) {
    airJumpSegments = emptyList()
    warnedAirJumpIds = emptySet()
    skippedAirJumpIds = emptySet()
    lastAirJumpPositionMs = 0L
    if (!airJumpAssistantEnabled || displayRequest.bvid.isBlank()) {
      return@LaunchedEffect
    }
    airJumpSegments = runCatching {
      playbackRepository.getAirJumpSegments(displayRequest.bvid)
    }.getOrDefault(emptyList())
  }

  LaunchedEffect(displayRequest.aid, displayRequest.cid) {
    onlineCountText = ""
    if (displayRequest.aid <= 0L || displayRequest.cid <= 0L) {
      return@LaunchedEffect
    }
    while (isActive) {
      onlineCountText = runCatching {
        playbackRepository.getOnlineCount(displayRequest.aid, displayRequest.cid).orEmpty()
      }.getOrDefault("")
      delay(OnlineCountRefreshMs)
    }
  }

  LaunchedEffect(danmakuSettings.enabled, displayRequest.cid) {
    danmakuEntries = emptyList()
    val cid = displayRequest.cid
    if (!danmakuSettings.enabled || cid <= 0L) {
      return@LaunchedEffect
    }
    val result = runCatching {
      playbackRepository.getDanmaku(cid)
    }
    result.onFailure { error ->
      Log.w(PlayerDanmakuLogTag, "Failed to load danmaku cid=$cid", error)
    }
    danmakuEntries = result.getOrDefault(emptyList())
    Log.i(PlayerDanmakuLogTag, "Loaded danmaku cid=$cid count=${danmakuEntries.size}")
  }

  LaunchedEffect(seekPreviewSpritesEnabled, displayRequest.bvid, displayRequest.cid) {
    videoshotData = null
    videoshotSprites = emptyMap()
    if (!seekPreviewSpritesEnabled || displayRequest.bvid.isBlank()) {
      return@LaunchedEffect
    }
    videoshotData = runCatching {
      playbackRepository.getVideoshot(displayRequest.bvid, displayRequest.cid)
    }.getOrNull()
  }

  LaunchedEffect(seekPreviewSpritesEnabled, videoshotData?.images, previewPositionMs, durationMs) {
    val data = videoshotData ?: return@LaunchedEffect
    if (!seekPreviewSpritesEnabled) return@LaunchedEffect

    val targetUrls = buildList {
      data.frameAt(previewPositionMs ?: positionMs, durationMs)?.imageUrl?.let(::add)
      data.images.take(VideoshotPreloadImageCount).forEach(::add)
    }
      .filter { url -> url.isNotBlank() && url !in videoshotSprites }
      .distinct()

    targetUrls.forEach { url ->
      val image = runCatching {
        playbackRepository.getVideoshotImageBytes(url)?.decodeImageBitmapOrNull()
      }.getOrNull() ?: return@forEach
      videoshotSprites = videoshotSprites + (url to image)
    }
  }

  LaunchedEffect(Unit) {
    withFrameNanos { }
    runCatching { controlsFocusRequester.requestFocus() }
  }

  LaunchedEffect(controlsVisible, playerState, activePanel, previewPositionMs, playbackPaused) {
    if (controlsVisible && playerState is PlayerScreenState.Ready) {
      runCatching { controlsFocusRequester.requestFocus() }
      if (!playbackPaused) {
        delay(BiliMotion.PlayerControlsAutoHideMs)
        if (activePanel == PlayerPanel.None && previewPositionMs == null && !playbackPaused) {
          controlsVisible = false
        }
      }
    }
  }

  LaunchedEffect(previewPositionMs, seekPreviewSpritesEnabled) {
    if (previewPositionMs != null && !seekPreviewSpritesEnabled) {
      delay(BiliMotion.PlayerSeekPreviewAutoCommitMs)
      commitPreviewSeek()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BiliColors.VideoBlack)
      .focusRequester(controlsFocusRequester)
      .focusable()
      .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) {
          return@onPreviewKeyEvent false
        }
        if (showUnfollowConfirm) {
          return@onPreviewKeyEvent when (event.key) {
            Key.Back -> {
              showUnfollowConfirm = false
              unfollowConfirmFocusedConfirm = false
              true
            }
            Key.DirectionLeft,
            Key.DirectionRight -> {
              unfollowConfirmFocusedConfirm = !unfollowConfirmFocusedConfirm
              showControls()
              true
            }
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter -> {
              if (unfollowConfirmFocusedConfirm) {
                setUpFollowStatus(false)
              } else {
                showUnfollowConfirm = false
                unfollowConfirmFocusedConfirm = false
              }
              showControls()
              true
            }
            else -> true
          }
        }
        if (playerState !is PlayerScreenState.Ready && event.key != Key.Back) {
          return@onPreviewKeyEvent false
        }
        when (event.key) {
          Key.Back -> {
            closePanelOrControls()
            true
          }
          Key.Menu -> {
            showControls()
            true
          }
          Key.DirectionCenter,
          Key.Enter,
          Key.NumPadEnter -> {
            if (controlsVisible || activePanel != PlayerPanel.None || previewPositionMs != null) {
              activateFocusedOverlay()
            } else {
              togglePlayback()
            }
            true
          }
          Key.DirectionLeft -> {
            when {
              activePanel != PlayerPanel.None -> {
                when (activePanel) {
                  PlayerPanel.Quality,
                  PlayerPanel.Speed -> openPanel(PlayerPanel.Main)
                  PlayerPanel.Danmaku -> {
                    if (!adjustFocusedDanmakuSetting(-1)) {
                      openPanel(PlayerPanel.Main)
                    }
                  }
                  PlayerPanel.UpVideos -> {
                    if (focusedPanelIndex == UpFocusFollow) {
                      focusedPanelIndex = UpFocusSort
                      showControls()
                    } else {
                      closePanelOrControls()
                    }
                  }
                  else -> closePanelOrControls()
                }
              }
              previewPositionMs != null -> updatePreviewSeek(-SeekStepMs)
              progressFocused -> updatePreviewSeek(-SeekStepMs, revealControls = true)
              controlsVisible -> moveFocusedControl(-1)
              else -> updatePreviewSeek(-SeekStepMs, revealControls = false)
            }
            true
          }
          Key.DirectionRight -> {
            when {
              activePanel != PlayerPanel.None -> {
                when {
                  activePanel == PlayerPanel.Main -> activateFocusedPanelItem()
                  activePanel == PlayerPanel.Danmaku -> adjustFocusedDanmakuSetting(1)
                  activePanel == PlayerPanel.UpVideos && focusedPanelIndex == UpFocusSort -> {
                    focusedPanelIndex = UpFocusFollow
                    showControls()
                  }
                }
              }
              previewPositionMs != null -> updatePreviewSeek(SeekStepMs)
              progressFocused -> updatePreviewSeek(SeekStepMs, revealControls = true)
              controlsVisible -> moveFocusedControl(1)
              else -> updatePreviewSeek(SeekStepMs, revealControls = false)
            }
            true
          }
          Key.DirectionUp -> {
            when {
              activePanel != PlayerPanel.None -> changePanelFocus(-1)
              controlsVisible && !progressFocused -> progressFocused = true
              else -> Unit
            }
            true
          }
          Key.DirectionDown -> {
            when {
              activePanel != PlayerPanel.None -> changePanelFocus(1)
              controlsVisible && progressFocused -> progressFocused = false
              else -> Unit
            }
            true
          }
          Key.MediaPlayPause -> {
            togglePlayback()
            true
          }
          else -> false
        }
      },
  ) {
    AndroidView(
      factory = { viewContext ->
        PlayerView(viewContext).apply {
          useController = false
          keepScreenOn = true
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
          setShutterBackgroundColor(android.graphics.Color.BLACK)
          this.player = player
        }
      },
      update = { view ->
        view.keepScreenOn = true
        view.player = player
      },
      modifier = Modifier.fillMaxSize(),
    )

    when (val state = playerState) {
      PlayerScreenState.Loading -> PlayerLoadingOverlay()
      is PlayerScreenState.Failed -> FeedStatusScreen(
        message = stringResource(R.string.player_failed_with_message, state.message),
        actionLabel = stringResource(R.string.action_retry),
        onAction = {
          retryKey += 1L
        },
      )
      is PlayerScreenState.Ready -> {
        PlayerDanmakuLayer(
          entries = danmakuEntries,
          settings = danmakuSettings,
          positionMs = positionMs,
          isPlaying = playerActuallyPlaying && previewPositionMs == null && !completionReported,
          playbackSpeed = playbackSpeed,
          lowSpecMode = performancePolicy.lowSpecMode,
          modifier = Modifier.fillMaxSize(),
        )
        PlayerOverlay(
          request = displayRequest,
          info = state.info,
          metadata = metadata,
          sidePanelVideos = sidePanelVideos,
          sidePanelLoading = sidePanelLoading,
          upVideoOrder = upVideoOrder,
          upFollowed = upFollowed,
          upFollowLoading = upFollowLoading,
          playbackPaused = playbackPaused,
          seekPreviewSpritesEnabled = seekPreviewSpritesEnabled,
          videoshotData = videoshotData,
          videoshotSprites = videoshotSprites,
          onlineCountText = onlineCountText,
          currentCodecText = currentCodecText,
          showUnfollowConfirm = showUnfollowConfirm,
          unfollowConfirmFocusedConfirm = unfollowConfirmFocusedConfirm,
          controlsVisible = controlsVisible,
          focusedControl = focusedControl,
          progressFocused = progressFocused,
          activePanel = activePanel,
          focusedPanelIndex = focusedPanelIndex,
          playbackSpeed = playbackSpeed,
          danmakuSettings = danmakuSettings,
          positionMs = positionMs,
          durationMs = durationMs,
          bufferedPercentage = bufferedPercentage,
          airJumpSegments = airJumpSegments,
          previewPositionMs = previewPositionMs,
          showClock = showClock,
        )
      }
    }
    if (showClock && playerState !is PlayerScreenState.Ready) {
      ClockOverlay(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(
            top = BiliSizing.ClockOverlayTopPadding,
            end = BiliSizing.ClockOverlayEndPadding,
          ),
      )
    }
  }
}

@Composable
private fun PlayerLoadingOverlay() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(BiliSpacing.Lg),
    ) {
      CircularProgressIndicator(color = BiliColors.BiliPink)
      Text(
        text = stringResource(R.string.player_loading),
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.Body,
      )
    }
  }
}

private fun buildDashMediaItem(info: PlaybackInfo): MediaItem {
  return MediaItem.Builder()
    .setUri(buildDashManifest(info))
    .setMimeType(MimeTypes.APPLICATION_MPD)
    .build()
}

private fun buildDashManifest(info: PlaybackInfo): String {
  val videoRepresentations = info.videoTracks.joinToString(separator = "\n") { track ->
    track.toRepresentation(adaptationSetId = "0", contentType = "video")
  }
  val audioRepresentations = info.audioTracks.joinToString(separator = "\n") { track ->
    track.toRepresentation(adaptationSetId = "1", contentType = "audio")
  }
  val durationSeconds = (info.durationMs / 1000L).coerceAtLeast(1L)
  return """
    <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT${durationSeconds}S" minBufferTime="PT1.5S" profiles="urn:mpeg:dash:profile:isoff-on-demand:2011">
      <Period duration="PT${durationSeconds}S">
        <AdaptationSet id="0" contentType="video" mimeType="${info.videoTracks.firstOrNull()?.mimeType.orEmpty().ifBlank { "video/mp4" }}" segmentAlignment="true">
          $videoRepresentations
        </AdaptationSet>
        <AdaptationSet id="1" contentType="audio" mimeType="${info.audioTracks.firstOrNull()?.mimeType.orEmpty().ifBlank { "audio/mp4" }}" segmentAlignment="true">
          $audioRepresentations
        </AdaptationSet>
      </Period>
    </MPD>
  """.trimIndent()
    .toByteArray(Charsets.UTF_8)
    .let { bytes -> "data:application/dash+xml;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}" }
}

private fun PlaybackTrack.toRepresentation(adaptationSetId: String, contentType: String): String {
  val escapedUrl = baseUrl.escapeXml()
  val dimensions = if (contentType == "video") {
    """ width="$width" height="$height""""
  } else {
    ""
  }
  return """
    <Representation id="${adaptationSetId}_$id" bandwidth="$bandwidth" codecs="${codecs.escapeXml()}"$dimensions>
      <BaseURL>$escapedUrl</BaseURL>
      <SegmentBase indexRange="${segmentBase.indexRange.escapeXml()}">
        <Initialization range="${segmentBase.initializationRange.escapeXml()}" />
      </SegmentBase>
    </Representation>
  """.trimIndent()
}

private fun String.escapeXml(): String {
  return replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
}

private fun stepFloat(current: Float, values: List<Float>, delta: Int): Float {
  val index = values.indexOf(current).takeIf { it >= 0 } ?: values.indexOfFirst { it >= current }.takeIf { it >= 0 } ?: 0
  return values[(index + delta).coerceIn(0, values.lastIndex)]
}

private fun stepInt(current: Int, values: List<Int>, delta: Int): Int {
  val index = values.indexOf(current).takeIf { it >= 0 } ?: values.indexOfFirst { it >= current }.takeIf { it >= 0 } ?: 0
  return values[(index + delta).coerceIn(0, values.lastIndex)]
}

private fun VideoSummary.toPlaybackRequest(): PlaybackRequest {
  val advanceToNextEpisode = shouldAdvanceToNextHistoryEpisode()
  return PlaybackRequest(
    bvid = bvid,
    cid = cid,
    title = title,
    startPositionMs = progress
      .takeIf { progress -> progress > 0 && !isWatchCompleted() && !advanceToNextEpisode }
      ?.times(1000L) ?: 0L,
    ownerName = ownerName,
    ownerFace = ownerFace,
    ownerMid = ownerMid,
    viewCount = view,
    danmakuCount = danmaku,
    pubdate = pubdate,
    historyPage = historyPage,
    advanceToNextHistoryEpisode = advanceToNextEpisode,
  )
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
  val pages = this?.pages.orEmpty()
  return pages.isEmpty() || pages.any { episode -> episode.cid == cid }
}

private fun List<VideoSummary>.withoutCurrentVideo(request: PlaybackRequest): List<VideoSummary> {
  if (request.bvid.isBlank()) return this
  return filterNot { video -> video.bvid.equals(request.bvid, ignoreCase = true) }
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

private fun PlaybackTrack.codecLabel(): String {
  return codecs.codecLabelFromCodecs().orEmpty()
}

private fun String.codecLabelFromCodecs(): String? {
  return when {
    contains("av01", ignoreCase = true) -> "AV1"
    contains("hev", ignoreCase = true) || contains("hvc", ignoreCase = true) -> "H.265"
    contains("avc", ignoreCase = true) -> "H.264"
    else -> null
  }
}

private fun ByteArray.decodeImageBitmapOrNull(): ImageBitmap? {
  return BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
}

private fun upVideoCacheKey(ownerMid: Long, order: String): String {
  return "$ownerMid:$order"
}

private fun Map<String, List<VideoSummary>>.withBoundedEntry(
  key: String,
  videos: List<VideoSummary>,
): Map<String, List<VideoSummary>> {
  val nextEntries = (this - key).toMutableMap()
  nextEntries[key] = videos.take(MaxUpVideoCacheVideosPerKey)
  return nextEntries.entries
    .toList()
    .takeLast(MaxUpVideoCacheKeys)
    .associate { entry -> entry.key to entry.value }
}

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}

@Suppress("DEPRECATION")
private fun Context.createPlayerWakeLock(): PowerManager.WakeLock? {
  val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
  return powerManager.newWakeLock(
    PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
    "BiliTVNative:PlayerScreen",
  ).apply {
    setReferenceCounted(false)
  }
}

private sealed interface PlayerScreenState {
  data object Loading : PlayerScreenState
  data class Ready(val info: PlaybackInfo) : PlayerScreenState
  data class Failed(val message: String) : PlayerScreenState
}

private const val SeekStepMs = 10_000L
private const val OnlineCountRefreshMs = 60_000L
private const val VideoshotPreloadImageCount = 2
private const val ExitConfirmWindowMs = 3_000L
private const val CompletedProgressSeconds = -1
private const val CompletionActionDelayMs = 3_000L
private const val AirJumpWarningLeadMs = 3_500L
private const val AirJumpCompletionToastSuppressMs = 1_500L
private const val AirJumpRewindResetThresholdMs = 2_000L
private const val AirJumpRewindResetLeadMs = 1_000L
private const val MaxUpVideoCacheKeys = 4
private const val MaxUpVideoCacheVideosPerKey = 50
private const val PlayerDanmakuLogTag = "BiliTVNative:Danmaku"
private val DanmakuOpacityOptions = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
private val DanmakuFontSizeOptions = listOf(16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36)
private val DanmakuAreaOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
private val DanmakuSpeedOptions = listOf(3, 4, 5, 6, 7)
