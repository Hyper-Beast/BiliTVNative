package com.kirin.bilitv.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.kirin.bilitv.core.player.PlaybackRepository
import com.kirin.bilitv.core.player.PlaybackRequest
import com.kirin.bilitv.core.player.PlaybackTrack
import com.kirin.bilitv.core.player.PlayerComment
import com.kirin.bilitv.core.player.VideoshotData
import com.kirin.bilitv.core.player.createTvPlaybackLoadControl
import com.kirin.bilitv.ui.common.ClockOverlay
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.common.currentClockMinuteKey
import com.kirin.bilitv.ui.common.currentClockText
import com.kirin.bilitv.ui.glass.biliLiquidGlassSurface
import com.kirin.bilitv.ui.i18n.LocalChineseTextConverter
import com.kirin.bilitv.ui.input.InteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.coroutines.resume

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
  showMiniProgressBar: Boolean,
  captureExitFrame: Boolean = false,
  onExitFrameReady: (ImageBitmap?) -> Unit = {},
  onPlaybackRequestChanged: (PlaybackRequest) -> Unit = {},
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
  val rootView = LocalView.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val interactionMode = LocalInteractionMode.current
  val touchMode = interactionMode == InteractionMode.Touch
  val textConverter = LocalChineseTextConverter.current
  val showClockState = rememberUpdatedState(showClock)
  val latestOnPlaybackRequestChanged = rememberUpdatedState(onPlaybackRequestChanged)
  val playerLoadStateHolder = remember(request, playbackRepository) {
    PlayerLoadStateHolder(
      initialRequest = request,
      playbackRepository = playbackRepository,
    )
  }
  val loadViewState = playerLoadStateHolder.viewState
  val activeRequest = loadViewState.activeRequest
  val displayRequest = loadViewState.displayRequest
  val playerState = loadViewState.screenState
  val metadata = loadViewState.metadata
  val selectedQuality = loadViewState.selectedQuality
  val loadRetryToken = loadViewState.retryToken
  val playerSidePanelStateHolder = remember { PlayerSidePanelStateHolder() }
  val sidePanelState = playerSidePanelStateHolder.viewState
  val sidePanelVideos = sidePanelState.videos
  val sidePanelLoading = sidePanelState.loading
  val upVideoOrder = sidePanelState.upVideoOrder
  val upFollowed = sidePanelState.upFollowed
  val upFollowLoading = sidePanelState.upFollowLoading
  var comments by remember { mutableStateOf<List<PlayerComment>>(emptyList()) }
  var commentsLoading by remember { mutableStateOf(false) }
  var commentsLoadJob by remember { mutableStateOf<Job?>(null) }
  var commentsLoadToken by remember { mutableLongStateOf(0L) }
  var commentsAid by remember { mutableLongStateOf(0L) }
  var showUnfollowConfirm by remember { mutableStateOf(false) }
  var unfollowConfirmFocusedConfirm by remember { mutableStateOf(false) }
  var onlineCountText by remember { mutableStateOf("") }
  var onlineCountRequestJob by remember { mutableStateOf<Job?>(null) }
  var onlineCountRequestToken by remember { mutableLongStateOf(0L) }
  var nextOnlineCountRefreshAtMs by remember { mutableLongStateOf(0L) }
  var clockText by remember { mutableStateOf(currentClockText()) }
  var clockMinuteKey by remember { mutableLongStateOf(currentClockMinuteKey()) }
  var currentCodecText by remember { mutableStateOf("") }
  var danmakuEntries by remember { mutableStateOf<List<DanmakuEntry>>(emptyList()) }
  var videoshotData by remember { mutableStateOf<VideoshotData?>(null) }
  var videoshotSprites by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
  var airJumpSegments by remember { mutableStateOf<List<AirJumpSegment>>(emptyList()) }
  var warnedAirJumpIds by remember { mutableStateOf(emptySet<String>()) }
  var skippedAirJumpIds by remember { mutableStateOf(emptySet<String>()) }
  var lastAirJumpPositionMs by remember { mutableLongStateOf(0L) }
  var controlsVisible by remember { mutableStateOf(false) }
  val overlayFocusStateHolder = remember { PlayerOverlayFocusStateHolder() }
  var progressFocused by overlayFocusStateHolder.progressFocusedState
  var focusedControl by overlayFocusStateHolder.focusedControlState
  var activePanel by remember { mutableStateOf(PlayerPanel.None) }
  var focusedPanelIndex by overlayFocusStateHolder.focusedPanelIndexState
  val storedDanmakuSettings by danmakuSettingsStore.settings.collectAsState(initial = DanmakuSettings())
  var danmakuSettings by remember { mutableStateOf(DanmakuSettings()) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var previewPositionMs by remember { mutableStateOf<Long?>(null) }
  val touchGestureStateHolder = remember { PlayerTouchGestureStateHolder() }
  var touchSeekActive by touchGestureStateHolder.seekActiveState
  var touchGestureSeekActive by touchGestureStateHolder.gestureSeekActiveState
  var touchGestureSeekDeltaMs by touchGestureStateHolder.gestureSeekDeltaMsState
  var touchGestureFeedback by touchGestureStateHolder.feedbackState
  val playbackPositionState = remember { mutableLongStateOf(0L) }
  val playbackDurationState = remember { mutableLongStateOf(0L) }
  val playbackBufferedPercentageState = remember { mutableLongStateOf(0L) }
  var danmakuSyncToken by remember { mutableLongStateOf(0L) }
  var playbackPaused by remember { mutableStateOf(false) }
  var playerActuallyPlaying by remember { mutableStateOf(false) }
  var lastPlaybackExitBackPressMs by remember { mutableLongStateOf(0L) }
  var playbackExitConfirmToast by remember { mutableStateOf<Toast?>(null) }
  var playbackCompletionToast by remember { mutableStateOf<Toast?>(null) }
  var playerExitJob by remember { mutableStateOf<Job?>(null) }
  val playerCompletionCoordinator = remember { PlayerCompletionCoordinator() }
  val completionReported = playerCompletionCoordinator.reported
  val controlsFocusRequester = remember { FocusRequester() }
  val player = remember {
    ExoPlayer.Builder(context)
      .setLoadControl(createTvPlaybackLoadControl())
      .build()
  }
  var playerView by remember { mutableStateOf<PlayerView?>(null) }
  val playbackWakeLock = remember(context) {
    context.applicationContext.createPlayerWakeLock()
  }

  fun checkpointPlaybackSession(positionMs: Long) {
    val loadState = playerLoadStateHolder.viewState
    if (loadState.screenState !is PlayerScreenState.Ready) return

    latestOnPlaybackRequestChanged.value(
      loadState.displayRequest.copy(
        startPositionMs = positionMs.coerceAtLeast(0L),
        preferredQualityId = loadState.selectedQuality?.id ?: loadState.activeRequest.preferredQualityId,
        forceStartPosition = true,
        advanceToNextHistoryEpisode = false,
      ),
    )
  }

  LaunchedEffect(storedDanmakuSettings) {
    danmakuSettings = storedDanmakuSettings
  }

  DisposableEffect(Unit) {
    onDispose {
      playerCompletionCoordinator.dispose()
      onlineCountRequestJob?.cancel()
      onlineCountRequestJob = null
      commentsLoadJob?.cancel()
      commentsLoadJob = null
      playerExitJob?.cancel()
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
    playerCompletionCoordinator.cancelPendingAction()
    cancelPlaybackCompletionToast()
  }

  fun resetOnlineCountPolling() {
    if (onlineCountText.isNotEmpty() || onlineCountRequestJob != null || nextOnlineCountRefreshAtMs != 0L) {
      onlineCountRequestJob?.cancel()
      onlineCountRequestJob = null
      onlineCountRequestToken += 1L
      nextOnlineCountRefreshAtMs = 0L
      onlineCountText = ""
    }
  }

  fun acquirePlaybackWakeLock() {
    runCatching {
      if (playbackWakeLock?.isHeld != true) {
        playbackWakeLock?.acquire(PlayerWakeLockTimeoutMs)
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
	      overlayFocusStateHolder.resetPrimaryControlFocus()
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
	    overlayFocusStateHolder.clearProgressFocus()
	    touchGestureStateHolder.clearSeek()
	    controlsVisible = false
	  }

  fun toggleControlsFromRemoteMenu() {
    if (!playbackPaused && controlsVisible && activePanel == PlayerPanel.None && previewPositionMs == null) {
      hideControlsForPlayback()
    } else {
      showControls()
    }
  }

  fun maxDurationMs(): Long {
    return player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
      ?: playbackDurationState.longValue.coerceAtLeast(0L)
  }

  fun alignPreviewTarget(targetMs: Long, currentPreviewMs: Long?, deltaMs: Long, maxDurationMs: Long): Long {
    return videoshotData.alignPreviewTarget(
      targetMs = targetMs,
      currentPreviewMs = currentPreviewMs,
      deltaMs = deltaMs,
      maxDurationMs = maxDurationMs,
      enabled = seekPreviewSpritesEnabled,
    )
  }

  fun commitPreviewSeek(revealControls: Boolean = controlsVisible || progressFocused) {
    val target = previewPositionMs ?: return
    player.seekTo(target.coerceIn(0L, maxDurationMs().takeIf { it > 0L } ?: Long.MAX_VALUE))
    playbackPositionState.longValue = target
    danmakuSyncToken += 1L
    previewPositionMs = null
	    if (revealControls) {
	      showControls()
	    } else {
	      overlayFocusStateHolder.clearProgressFocus()
	    }
	  }

  fun updatePreviewSeek(deltaMs: Long, revealControls: Boolean = controlsVisible || progressFocused) {
    val maxDuration = maxDurationMs().takeIf { it > 0L } ?: Long.MAX_VALUE
    val basePosition = previewPositionMs
      ?: player.currentPosition.takeIf { it >= 0L }
      ?: playbackPositionState.longValue
    val target = (basePosition + deltaMs).coerceIn(0L, maxDuration)
    previewPositionMs = alignPreviewTarget(
      targetMs = target,
      currentPreviewMs = previewPositionMs,
      deltaMs = deltaMs,
      maxDurationMs = maxDuration,
    )
    activePanel = PlayerPanel.None
	    if (revealControls) {
	      overlayFocusStateHolder.focusProgress()
	      showControls()
	    } else {
	      overlayFocusStateHolder.clearProgressFocus()
	    }
	  }

  fun updatePreviewSeekTo(
    targetMs: Long,
    revealControls: Boolean = controlsVisible || progressFocused,
    snapToVideoshot: Boolean = true,
  ) {
    val maxDuration = maxDurationMs().takeIf { it > 0L } ?: Long.MAX_VALUE
    val basePosition = previewPositionMs
      ?: player.currentPosition.takeIf { it >= 0L }
      ?: playbackPositionState.longValue
    val target = targetMs.coerceIn(0L, maxDuration)
    previewPositionMs = if (snapToVideoshot) {
      alignPreviewTarget(
        targetMs = target,
        currentPreviewMs = previewPositionMs,
        deltaMs = target - basePosition,
        maxDurationMs = maxDuration,
      )
    } else {
      target
    }
	    if (revealControls) {
	      showControls()
	    } else {
	      overlayFocusStateHolder.clearProgressFocus()
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
      playbackPositionState.longValue = targetPositionMs
      danmakuSyncToken += 1L
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
    val currentDurationMs = player.duration.takeIf { it != C.TIME_UNSET }
      ?: playbackDurationState.longValue
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
      ?: ((player.currentPosition.takeIf { it >= 0L } ?: playbackPositionState.longValue).coerceAtLeast(0L) / 1000L).toInt()
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
    if (playerExitJob?.isActive == true) {
      return
    }
    cancelPlaybackExitConfirmToast()
    cancelPendingCompletionAction()
    playerExitJob = coroutineScope.launch {
      saveAndReportProgressNow()
      val exitFrame = if (captureExitFrame) {
        playerView.capturePlaybackSurfaceFrame()
      } else {
        null
      }
      onExitFrameReady(exitFrame)
      onFinished()
    }
  }

  fun exitPlayer() {
    if (playerExitJob?.isActive == true) {
      return
    }
    if (touchMode) {
      cancelPlaybackExitConfirmToast()
      cancelPendingCompletionAction()
	      activePanel = PlayerPanel.None
	      showUnfollowConfirm = false
	      unfollowConfirmFocusedConfirm = false
	      controlsVisible = false
	      previewPositionMs = null
	      overlayFocusStateHolder.clearProgressFocus()
	      touchGestureStateHolder.clearSeek()
	      touchGestureStateHolder.feedbackState.value = null
	      finishPlayer(onBack)
	      return
	    }
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
	    overlayFocusStateHolder.clearProgressFocus()
	    showControls()
	  }

  suspend fun resolveDisplayMetadata() = playerLoadStateHolder.resolveDisplayMetadata()

  fun openVideoListPanel(
    panel: PlayerPanel,
    defaultFocusedIndex: Int = 0,
    loader: suspend () -> List<VideoSummary>,
  ) {
    openPanel(panel)
    playerSidePanelStateHolder.openVideoListPanel(
      coroutineScope = coroutineScope,
      panel = panel,
      defaultFocusedIndex = defaultFocusedIndex,
      loader = loader,
      isActivePanel = { expectedPanel -> activePanel == expectedPanel },
      updateFocusedPanelIndex = { focusedIndex -> focusedPanelIndex = focusedIndex },
      showControls = ::showControls,
    )
  }

  fun openUpVideos(order: String = UpVideoOrderLatest) {
    openPanel(PlayerPanel.UpVideos)
    playerSidePanelStateHolder.openUpVideos(
      coroutineScope = coroutineScope,
      order = order,
      displayRequest = displayRequest,
      metadata = metadata,
      videoRepository = videoRepository,
      resolveDisplayMetadata = ::resolveDisplayMetadata,
      currentRequest = { displayRequest },
      isActiveUpVideosPanel = { activePanel == PlayerPanel.UpVideos },
      currentPanelDescription = { "activePanel=$activePanel" },
      currentFocusedPanelIndex = { focusedPanelIndex },
      updateFocusedPanelIndex = { focusedIndex -> focusedPanelIndex = focusedIndex },
      showControls = ::showControls,
    )
  }

  fun toggleUpOrder() {
    openUpVideos(playerSidePanelStateHolder.nextUpVideoOrder())
  }

  fun openComments() {
    openPanel(PlayerPanel.Comments)
    val knownAid = displayRequest.aid.takeIf { it > 0L } ?: metadata?.aid ?: 0L
    if (knownAid > 0L && commentsAid == knownAid && (comments.isNotEmpty() || commentsLoading)) {
      return
    }
    val loadToken = ++commentsLoadToken
    commentsLoadJob?.cancel()
    commentsLoadJob = coroutineScope.launch {
      comments = emptyList()
      commentsLoading = true
      val aid = knownAid.takeIf { it > 0L } ?: resolveDisplayMetadata()?.aid ?: 0L
      if (aid <= 0L) {
        if (commentsLoadToken == loadToken) {
          commentsAid = 0L
          commentsLoading = false
        }
        return@launch
      }
      val loadedComments = runCatching {
        playbackRepository.getComments(aid)
      }.getOrDefault(emptyList())
      if (commentsLoadToken == loadToken) {
        commentsAid = aid
        comments = loadedComments
        commentsLoading = false
        focusedPanelIndex = 0
        if (activePanel == PlayerPanel.Comments) {
          showControls()
        }
      }
    }
  }

  fun setUpFollowStatus(follow: Boolean) {
    val ownerMid = displayRequest.ownerMid.takeIf { it > 0L } ?: metadata?.ownerMid ?: 0L
    playerSidePanelStateHolder.setUpFollowStatus(
      coroutineScope = coroutineScope,
      ownerMid = ownerMid,
      follow = follow,
      videoRepository = videoRepository,
    ) {
      showUnfollowConfirm = false
      unfollowConfirmFocusedConfirm = false
      showControls()
    }
  }

  fun closePanelOrControls() {
    if (playerExitJob?.isActive == true) {
      return
    }
    if (completionReported) {
      cancelPendingCompletionAction()
    }
    when {
      playerState is PlayerScreenState.Failed -> exitPlayer()
      showUnfollowConfirm -> {
        showUnfollowConfirm = false
        unfollowConfirmFocusedConfirm = false
      }
	      previewPositionMs != null -> {
	        previewPositionMs = null
	        overlayFocusStateHolder.clearProgressFocus()
	      }
      activePanel != PlayerPanel.None -> openPanel(PlayerPanel.None)
      controlsVisible -> controlsVisible = false
      touchMode -> exitPlayer()
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
	    cancelPendingCompletionAction()
	    resetOnlineCountPolling()
	    playerSidePanelStateHolder.clearVideos()
	    commentsLoadJob?.cancel()
	    commentsLoadJob = null
	    commentsLoadToken += 1L
	    comments = emptyList()
	    commentsLoading = false
	    commentsAid = 0L
	    activePanel = PlayerPanel.None
	    overlayFocusStateHolder.clearProgressFocus()
	    previewPositionMs = null
	    playerCompletionCoordinator.clearReported()
	    latestOnPlaybackRequestChanged.value(nextRequest)
	    playerLoadStateHolder.startPlaybackRequest(nextRequest, clearMetadata)
	    controlsVisible = false
	  }

  fun scheduleCompletionAction() {
    playerCompletionCoordinator.launchAction(coroutineScope) {
      if (autoPlayNextEpisode) {
        val videoMetadata = resolveDisplayMetadata()
        if (!isActive()) return@launchAction
        val nextEpisode = displayRequest.nextEpisodeCompletion(
          metadata = videoMetadata,
          selectedQualityId = selectedQuality?.id,
        )
        if (nextEpisode != null) {
          showPlaybackCompletionToast(
            context.getString(
              R.string.player_completion_next_episode_toast,
              textConverter.convert(nextEpisode.title),
            ),
          )
          if (delayIfActive(CompletionActionDelayMs)) {
            startPlaybackRequest(nextEpisode.request, clearMetadata = false)
          }
          return@launchAction
        }
      }

      if (autoPlayRelatedVideo) {
        val relatedVideo = runCatching {
          videoRepository.getRelatedVideos(displayRequest.bvid)
            .firstCompletionRelatedVideo(displayRequest.bvid)
        }.getOrNull()
        if (!isActive()) return@launchAction
        if (relatedVideo != null) {
          showPlaybackCompletionToast(
            context.getString(R.string.player_completion_related_toast, textConverter.convert(relatedVideo.title)),
          )
          if (delayIfActive(CompletionActionDelayMs)) {
            startPlaybackRequest(relatedVideo.toPlaybackRequest(), clearMetadata = true)
          }
          return@launchAction
        }
      }

      if (autoReturnHomeOnCompletion) {
        showPlaybackCompletionToast(context.getString(R.string.player_completion_home_toast))
        if (delayIfActive(CompletionActionDelayMs)) {
          cancelPlaybackCompletionToast()
          exitPlayer()
        }
      }
    }
  }

  fun reportPlaybackCompleted() {
    if (!playerCompletionCoordinator.markReported()) return
    val completedDurationMs = maxDurationMs()
    if (completedDurationMs > 0L) {
      playbackPositionState.longValue = completedDurationMs
      playbackDurationState.longValue = completedDurationMs
    }
    controlsVisible = true
    playbackPaused = true
    saveAndReportProgress(CompletedProgressSeconds)
    scheduleCompletionAction()
  }

  val latestPlayerState = rememberUpdatedState(playerState)
  val latestReportPlaybackCompleted = rememberUpdatedState { reportPlaybackCompleted() }

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
      PlayerPanel.Comments -> if (commentsLoading) 0 else comments.size
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
        playerLoadStateHolder.selectQuality(
          quality = quality,
          startPositionMs = player.currentPosition.takeIf { it > 0L } ?: playbackPositionState.longValue,
        )
        latestOnPlaybackRequestChanged.value(playerLoadStateHolder.viewState.activeRequest)
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
      PlayerPanel.Comments -> Unit
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

  fun activateTouchSettingsRow(index: Int) {
    focusedPanelIndex = index
    if (activePanel == PlayerPanel.Danmaku && adjustFocusedDanmakuSetting(1)) {
      return
    }
    activateFocusedPanelItem()
  }

  fun adjustTouchSettingsRow(index: Int, delta: Int) {
    focusedPanelIndex = index
    if (!adjustFocusedDanmakuSetting(delta)) {
      activateFocusedPanelItem()
    }
  }

  fun activateTouchEpisode(index: Int) {
    focusedPanelIndex = index
    activateFocusedPanelItem()
  }

  fun activateTouchVideo(index: Int) {
    focusedPanelIndex = if (activePanel == PlayerPanel.UpVideos) {
      index + UpPanelHeaderItemCount
    } else {
      index
    }
    activateFocusedPanelItem()
  }

  fun activateTouchUpSort() {
    focusedPanelIndex = UpFocusSort
    activateFocusedPanelItem()
  }

  fun activateTouchUpFollow() {
    focusedPanelIndex = UpFocusFollow
    activateFocusedPanelItem()
  }

  fun dismissTouchPanel() {
    if (showUnfollowConfirm) {
      showUnfollowConfirm = false
      unfollowConfirmFocusedConfirm = false
      showControls()
      return
    }
    hideControlsForPlayback()
  }

  fun cancelTouchUnfollow() {
    showUnfollowConfirm = false
    unfollowConfirmFocusedConfirm = false
    showControls()
  }

	  fun moveFocusedControl(delta: Int) {
	    overlayFocusStateHolder.moveFocusedControl(delta)
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

  fun openTouchPanel(panel: PlayerPanel) {
    if (activePanel == panel) {
      activePanel = PlayerPanel.None
      showControls()
      return
    }
    when (panel) {
      PlayerPanel.Main,
      PlayerPanel.Danmaku,
      PlayerPanel.Episodes -> openPanel(panel)
      PlayerPanel.UpVideos -> openUpVideos(UpVideoOrderLatest)
      PlayerPanel.RelatedVideos -> {
        openVideoListPanel(
          panel = PlayerPanel.RelatedVideos,
          defaultFocusedIndex = 0,
        ) {
          videoRepository.getRelatedVideos(displayRequest.bvid)
        }
      }
      PlayerPanel.Comments -> openComments()
      PlayerPanel.Quality,
      PlayerPanel.Speed,
      PlayerPanel.None -> Unit
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

  DisposableEffect(activity, rootView, playbackWakeLock) {
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
        playerLoadStateHolder.fail(error.message.orEmpty())
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
        if (playbackState == Player.STATE_ENDED && latestPlayerState.value is PlayerScreenState.Ready && player.mediaItemCount > 0) {
          latestReportPlaybackCompleted.value()
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
          checkpointPlaybackSession(
            player.currentPosition.takeIf { position -> position >= 0L }
              ?: playbackPositionState.longValue,
          )
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

  LaunchedEffect(activeRequest, playbackCodecPreference, playbackQualityPreference, loadRetryToken) {
	    cancelPendingCompletionAction()
	    previewPositionMs = null
	    touchGestureStateHolder.clearSeek()
	    playbackPositionState.longValue = 0L
    playbackDurationState.longValue = 0L
    playbackBufferedPercentageState.longValue = 0L
    playbackPaused = false
    currentCodecText = ""
    danmakuEntries = emptyList()
    commentsLoadJob?.cancel()
    commentsLoadJob = null
    commentsLoadToken += 1L
    comments = emptyList()
    commentsLoading = false
    commentsAid = 0L
    airJumpSegments = emptyList()
    warnedAirJumpIds = emptySet()
    skippedAirJumpIds = emptySet()
    lastAirJumpPositionMs = 0L
    playerActuallyPlaying = false
    player.clearMediaItems()
    playerCompletionCoordinator.clearReported()
    val readyState = playerLoadStateHolder.load(
      codecPreference = playbackCodecPreference,
      qualityPreference = playbackQualityPreference,
      missingCidMessage = context.getString(R.string.player_error_missing_cid),
      emptyTracksMessage = context.getString(R.string.player_error_empty_tracks),
    ) ?: return@LaunchedEffect
    checkpointPlaybackSession(readyState.startPositionMs)
    try {
      val info = readyState.info
      currentCodecText = info.videoTracks.firstOrNull()?.codecLabel().orEmpty()
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
      if (readyState.startPositionMs > 0L) {
        player.seekTo(readyState.startPositionMs)
        playbackPositionState.longValue = readyState.startPositionMs
        danmakuSyncToken += 1L
      }
      player.playWhenReady = true
      playbackPaused = false
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      playerLoadStateHolder.fail(error.message.orEmpty())
    }
  }

  val shouldPollOnlineCount = playerState is PlayerScreenState.Ready &&
    playerActuallyPlaying &&
    previewPositionMs == null &&
    !completionReported
  val shouldPollOnlineCountState = rememberUpdatedState(shouldPollOnlineCount)
  val displayRequestState = rememberUpdatedState(displayRequest)

  LaunchedEffect(player, playerState) {
    while (isActive) {
      val nowMs = SystemClock.elapsedRealtime()
      val currentPositionMs = player.currentPosition.takeIf { it >= 0L } ?: 0L
      playbackPositionState.longValue = currentPositionMs
      playbackDurationState.longValue = player.duration.takeIf { it != C.TIME_UNSET }
        ?: playbackDurationState.longValue
      playbackBufferedPercentageState.longValue = player.bufferedPercentage.toLong()
      currentCodecText = player.videoFormat?.codecs?.codecLabelFromCodecs()
        ?: (playerState as? PlayerScreenState.Ready)?.info?.videoTracks?.firstOrNull()?.codecLabel().orEmpty()
      if (showClockState.value) {
        val nextClockMinuteKey = currentClockMinuteKey()
        if (clockMinuteKey != nextClockMinuteKey) {
          clockMinuteKey = nextClockMinuteKey
          clockText = currentClockText()
        }
      }
      val onlineRequest = displayRequestState.value
      val canPollOnlineCount = shouldPollOnlineCountState.value &&
        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
        onlineRequest.aid > 0L &&
        onlineRequest.cid > 0L
      if (!canPollOnlineCount) {
        resetOnlineCountPolling()
      } else if (nowMs >= nextOnlineCountRefreshAtMs && onlineCountRequestJob?.isActive != true) {
        val aid = onlineRequest.aid
        val cid = onlineRequest.cid
        val requestToken = ++onlineCountRequestToken
        nextOnlineCountRefreshAtMs = nowMs + OnlineCountRefreshMs
        onlineCountRequestJob = coroutineScope.launch {
          try {
            val countText = runCatching {
              playbackRepository.getOnlineCount(aid, cid).orEmpty()
            }.getOrDefault("")
            val currentRequest = displayRequestState.value
            if (
              onlineCountRequestToken == requestToken &&
              shouldPollOnlineCountState.value &&
              lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
              currentRequest.aid == aid &&
              currentRequest.cid == cid
            ) {
              onlineCountText = countText
            }
          } finally {
            if (onlineCountRequestToken == requestToken) {
              onlineCountRequestJob = null
            }
          }
        }
      }
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

  LaunchedEffect(seekPreviewSpritesEnabled, videoshotData?.images, previewPositionMs, playbackDurationState.longValue) {
    val data = videoshotData ?: return@LaunchedEffect
    if (!seekPreviewSpritesEnabled) return@LaunchedEffect
    val targetUrls = data.previewSpriteUrls(
      previewPositionMs = previewPositionMs,
      playbackPositionMs = playbackPositionState.longValue,
      durationMs = playbackDurationState.longValue,
      cachedUrls = videoshotSprites.keys,
    )

    targetUrls.forEach { url ->
      val image = runCatching {
        playbackRepository.getVideoshotImageBytes(url)?.decodeImageBitmapOrNull()
      }.getOrNull() ?: return@forEach
      videoshotSprites = videoshotSprites.withBoundedSprite(url, image)
    }
  }

  LaunchedEffect(Unit) {
    withFrameNanos { }
    runCatching { controlsFocusRequester.requestFocus() }
  }

  LaunchedEffect(controlsVisible, playerState, activePanel, previewPositionMs, playbackPaused, touchSeekActive) {
    if (controlsVisible && playerState is PlayerScreenState.Ready) {
      runCatching { controlsFocusRequester.requestFocus() }
      if (!playbackPaused && !touchSeekActive) {
        delay(BiliMotion.PlayerControlsAutoHideMs)
        if (activePanel == PlayerPanel.None && previewPositionMs == null && !playbackPaused && !touchSeekActive) {
          controlsVisible = false
        }
      }
    }
  }

  LaunchedEffect(previewPositionMs, seekPreviewSpritesEnabled, touchSeekActive) {
    if (previewPositionMs != null && !seekPreviewSpritesEnabled && !touchSeekActive) {
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
            toggleControlsFromRemoteMenu()
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
	              controlsVisible && !progressFocused -> overlayFocusStateHolder.focusProgress()
              else -> Unit
            }
            true
          }
          Key.DirectionDown -> {
            when {
              activePanel != PlayerPanel.None -> changePanelFocus(1)
	              playbackPaused && controlsVisible && progressFocused -> overlayFocusStateHolder.clearProgressFocus()
              else -> toggleControlsFromRemoteMenu()
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
          playerView = this
        }
      },
      update = { view ->
        view.keepScreenOn = true
        view.player = player
        playerView = view
      },
      modifier = Modifier.fillMaxSize(),
    )

    Box(
      modifier = Modifier
        .fillMaxSize(),
    ) {
      when (val state = playerState) {
        PlayerScreenState.Loading -> PlayerLoadingOverlay()
        is PlayerScreenState.Failed -> PlayerFailedOverlay(
          message = stringResource(R.string.player_failed_with_message, state.message),
          touchMode = touchMode,
          onRetry = {
            playerLoadStateHolder.retry()
          },
          onBack = ::exitPlayer,
        )
        is PlayerScreenState.Ready -> {
          PlayerDanmakuLayer(
            entries = danmakuEntries,
            settings = danmakuSettings,
            positionState = playbackPositionState,
            syncToken = danmakuSyncToken,
            isPlaying = playerActuallyPlaying && previewPositionMs == null && !completionReported,
            playbackSpeed = playbackSpeed,
            lowSpecMode = performancePolicy.lowSpecMode,
            modifier = Modifier.fillMaxSize(),
          )
          PlayerTouchGestureLayer(
            enabled = interactionMode == InteractionMode.Touch && playerExitJob?.isActive != true,
            activePanel = activePanel,
            controlsVisible = controlsVisible,
            previewPositionMs = previewPositionMs,
            completionReported = completionReported,
            showUnfollowConfirm = showUnfollowConfirm,
            playbackSpeed = playbackSpeed,
            player = player,
            audioManager = audioManager,
            activity = activity,
            onSingleTap = {
              if (activePanel != PlayerPanel.None || previewPositionMs != null || controlsVisible) {
                hideControlsForPlayback()
              } else {
                showControls()
              }
            },
            onDoubleTap = {
              togglePlayback()
            },
	            onSpeedBoostStart = {
	              cancelPendingCompletionAction()
	              player.setPlaybackSpeed(TouchSpeedBoostRate)
	              touchGestureStateHolder.showFeedback(PlayerTouchFeedbackType.SpeedBoost)
	            },
	            onSpeedBoostEnd = {
	              player.setPlaybackSpeed(playbackSpeed)
	              touchGestureStateHolder.clearFeedback()
	            },
	            onTouchSeekStart = {
	              touchGestureStateHolder.beginGestureSeek()
	              controlsVisible = false
	              overlayFocusStateHolder.clearProgressFocus()
	              cancelPendingCompletionAction()
	            },
	            onTouchSeekPreview = { targetMs, deltaMs ->
	              touchGestureStateHolder.updateGestureSeekDelta(deltaMs)
	              updatePreviewSeekTo(
	                targetMs = targetMs,
	                revealControls = false,
                snapToVideoshot = false,
              )
            },
	            onTouchSeekEnd = {
	              commitPreviewSeek(revealControls = false)
	              touchGestureStateHolder.clearSeek()
	              controlsVisible = false
	            },
	            onTouchSeekCancel = {
	              previewPositionMs = null
	              overlayFocusStateHolder.clearProgressFocus()
	              touchGestureStateHolder.clearSeek()
	            },
            onExitGesture = {
              exitPlayer()
            },
	            onBrightnessChanged = { percent ->
	              touchGestureStateHolder.showFeedback(PlayerTouchFeedbackType.Brightness, percent)
	            },
	            onVolumeChanged = { percent ->
	              touchGestureStateHolder.showFeedback(PlayerTouchFeedbackType.Volume, percent)
	            },
	            onTouchAdjustmentEnd = {
	              touchGestureStateHolder.clearFeedbackUnlessSpeedBoost()
	            },
            modifier = Modifier.fillMaxSize(),
          )
        if (touchMode) {
          PlayerPassiveStatusChrome(
            positionState = playbackPositionState,
            durationState = playbackDurationState,
            airJumpSegments = airJumpSegments,
            showClock = showClock && !controlsVisible,
            clockText = clockText,
            showMiniProgressBar = showMiniProgressBar && !controlsVisible,
          )
        } else {
          PlayerTvChrome(
            request = displayRequest,
            info = state.info,
            onlineCountText = onlineCountText,
            currentCodecText = currentCodecText,
            controlsVisible = controlsVisible,
            focusedControl = focusedControl,
            progressFocused = progressFocused,
            danmakuSettings = danmakuSettings,
            positionState = playbackPositionState,
            durationState = playbackDurationState,
            bufferedPercentageState = playbackBufferedPercentageState,
            airJumpSegments = airJumpSegments,
            previewPositionMs = previewPositionMs,
            showClock = showClock,
            clockText = clockText,
            showMiniProgressBar = showMiniProgressBar,
          )
        }
        PlayerSharedOverlay(
          request = displayRequest,
          info = state.info,
          metadata = metadata,
          sidePanelVideos = sidePanelVideos,
          sidePanelLoading = sidePanelLoading,
          comments = comments,
          commentsLoading = commentsLoading,
          upVideoOrder = upVideoOrder,
          upFollowed = upFollowed,
          upFollowLoading = upFollowLoading,
          playbackPaused = playbackPaused,
          seekPreviewSpritesEnabled = seekPreviewSpritesEnabled,
          videoshotData = videoshotData,
          videoshotSprites = videoshotSprites,
          currentCodecText = currentCodecText,
          showUnfollowConfirm = showUnfollowConfirm,
          unfollowConfirmFocusedConfirm = unfollowConfirmFocusedConfirm,
          activePanel = activePanel,
          focusedPanelIndex = focusedPanelIndex,
          playbackSpeed = playbackSpeed,
          danmakuSettings = danmakuSettings,
          durationState = playbackDurationState,
          previewPositionMs = previewPositionMs,
          previewDeltaMs = if (touchGestureSeekActive) touchGestureSeekDeltaMs else null,
          onDismissPanel = if (touchMode) ::dismissTouchPanel else null,
          onSettingsRowClick = if (touchMode) ::activateTouchSettingsRow else null,
          onSettingsRowAdjust = if (touchMode) ::adjustTouchSettingsRow else null,
          onEpisodeClick = if (touchMode) ::activateTouchEpisode else null,
          onVideoClick = if (touchMode) ::activateTouchVideo else null,
          onUpSortClick = if (touchMode) ::activateTouchUpSort else null,
          onUpFollowClick = if (touchMode) ::activateTouchUpFollow else null,
          onUnfollowCancel = if (touchMode) ::cancelTouchUnfollow else null,
          onUnfollowConfirm = if (touchMode) {
            { setUpFollowStatus(false) }
          } else {
            null
          },
        )
        if (touchMode) {
          PlayerTouchOverlay(
            request = displayRequest,
            info = state.info,
            controlsVisible = controlsVisible,
            activePanel = activePanel,
            positionState = playbackPositionState,
            durationState = playbackDurationState,
            bufferedPercentageState = playbackBufferedPercentageState,
            airJumpSegments = airJumpSegments,
            previewPositionMs = previewPositionMs,
            showClock = showClock,
            clockText = clockText,
            onBack = {
              exitPlayer()
            },
	            onSeekStart = {
	              touchGestureStateHolder.beginSeek()
	              cancelPendingCompletionAction()
	            },
            onSeekPreview = { targetMs ->
              updatePreviewSeekTo(targetMs, revealControls = true)
            },
            onSeekCommit = { targetMs ->
	              updatePreviewSeekTo(targetMs, revealControls = false)
	              commitPreviewSeek(revealControls = false)
	              touchGestureStateHolder.endSeek()
	              controlsVisible = false
	            },
	            onSeekCancel = {
	              previewPositionMs = null
	              overlayFocusStateHolder.clearProgressFocus()
	              touchGestureStateHolder.endSeek()
	            },
            onPanelSelected = { panel ->
              openTouchPanel(panel)
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
        touchGestureFeedback?.let { feedback ->
          if (feedback.type == PlayerTouchFeedbackType.SpeedBoost) {
            PlayerTouchSpeedBoostOverlay(
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = BiliSizing.PlayerTouchSpeedBoostTopPadding),
            )
          } else {
            PlayerTouchFeedbackOverlay(
              feedback = feedback,
              modifier = Modifier.align(Alignment.Center),
            )
          }
        }
      }
    }
    if (showClock && playerState !is PlayerScreenState.Ready) {
      ClockOverlay(
        clockText = clockText,
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

@Composable
private fun PlayerFailedOverlay(
  message: String,
  touchMode: Boolean,
  onRetry: () -> Unit,
  onBack: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    FeedStatusScreen(
      message = message,
      actionLabel = stringResource(R.string.action_retry),
      onAction = onRetry,
    )
    if (touchMode) {
      PlayerFailedBackButton(
        onClick = onBack,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(
            start = BiliSizing.PlayerTouchOverlayHorizontalPadding,
            top = BiliSizing.PlayerTouchTopPadding,
          ),
      )
    }
  }
}

@Composable
private fun PlayerFailedBackButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(BiliRadius.Pill)
  Box(
    modifier = modifier
      .size(BiliSizing.PlayerTouchBackButtonSize)
      .clip(shape)
      .background(BiliColors.PlayerPanel.copy(alpha = BiliFocus.PlayerTouchChromeBalancedSurfaceAlpha), shape)
      .border(
        width = BiliFocus.RestingBorderWidth,
        color = BiliColors.TextPrimary.copy(alpha = BiliFocus.PlayerTouchChromeBalancedBorderAlpha),
        shape = shape,
      )
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_player_chevron_left),
      contentDescription = stringResource(R.string.player_control_back),
      tint = BiliColors.TextPrimary,
      modifier = Modifier.size(BiliSizing.PlayerTouchBackIconSize),
    )
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

private suspend fun View?.capturePlaybackSurfaceFrame(): ImageBitmap? {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
    return null
  }
  val surfaceView = (this as? PlayerView)?.videoSurfaceView as? SurfaceView ?: return null
  val width = surfaceView.width.takeIf { it > 0 } ?: return null
  val height = surfaceView.height.takeIf { it > 0 } ?: return null
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val copyResult = suspendCancellableCoroutine { continuation ->
    PixelCopy.request(
      surfaceView,
      bitmap,
      { result ->
        if (continuation.isActive) {
          continuation.resume(result)
        }
      },
      Handler(Looper.getMainLooper()),
    )
  }
  return bitmap
    .takeIf { copyResult == PixelCopy.SUCCESS && it.hasVisibleSamples() }
    ?.asImageBitmap()
}

private fun Bitmap.hasVisibleSamples(): Boolean {
  val columns = 16
  val rows = 9
  repeat(rows) { row ->
    val y = ((row + 0.5f) * height / rows).toInt().coerceIn(0, height - 1)
    repeat(columns) { column ->
      val x = ((column + 0.5f) * width / columns).toInt().coerceIn(0, width - 1)
      val pixel = getPixel(x, y)
      val red = pixel shr 16 and 0xFF
      val green = pixel shr 8 and 0xFF
      val blue = pixel and 0xFF
      if (maxOf(red, green, blue) > 8) {
        return true
      }
    }
  }
  return false
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

@Composable
private fun PlayerTouchGestureLayer(
  enabled: Boolean,
  activePanel: PlayerPanel,
  controlsVisible: Boolean,
  previewPositionMs: Long?,
  completionReported: Boolean,
  showUnfollowConfirm: Boolean,
  playbackSpeed: Float,
  player: ExoPlayer,
  audioManager: AudioManager?,
  activity: Activity?,
  onSingleTap: () -> Unit,
  onDoubleTap: () -> Unit,
  onSpeedBoostStart: () -> Unit,
  onSpeedBoostEnd: () -> Unit,
  onTouchSeekStart: () -> Unit,
  onTouchSeekPreview: (Long, Long) -> Unit,
  onTouchSeekEnd: () -> Unit,
  onTouchSeekCancel: () -> Unit,
  onExitGesture: () -> Unit,
  onBrightnessChanged: (Int) -> Unit,
  onVolumeChanged: (Int) -> Unit,
  onTouchAdjustmentEnd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!enabled) return

  val density = LocalDensity.current
  val edgeSwipeWidthPx = with(density) { BiliSizing.PlayerTouchEdgeSwipeWidth.toPx() }
  val bottomGestureIgnoreHeightPx = with(density) { BiliSizing.PlayerTouchBottomGestureIgnoreHeight.toPx() }
  var speedBoostActive by remember { mutableStateOf(false) }

  fun endSpeedBoostIfNeeded() {
    if (speedBoostActive) {
      speedBoostActive = false
      onSpeedBoostEnd()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .pointerInput(
        activePanel,
        controlsVisible,
        previewPositionMs,
        completionReported,
        showUnfollowConfirm,
        playbackSpeed,
      ) {
        detectTapGestures(
          onTap = {
            if (!showUnfollowConfirm) {
              onSingleTap()
            }
          },
          onDoubleTap = {
            if (!showUnfollowConfirm && activePanel == PlayerPanel.None && previewPositionMs == null && !completionReported) {
              endSpeedBoostIfNeeded()
              onDoubleTap()
            }
          },
          onLongPress = {
            if (
              !showUnfollowConfirm &&
              activePanel == PlayerPanel.None &&
              previewPositionMs == null &&
              !completionReported &&
              player.isPlaying
            ) {
              speedBoostActive = true
              onSpeedBoostStart()
            }
          },
          onPress = {
            try {
              tryAwaitRelease()
            } finally {
              endSpeedBoostIfNeeded()
            }
          },
        )
      }
      .pointerInput(
        activePanel,
        completionReported,
        showUnfollowConfirm,
        edgeSwipeWidthPx,
        bottomGestureIgnoreHeightPx,
        speedBoostActive,
      ) {
        var dragStart = Offset.Zero
        var dragTotal = Offset.Zero
        var dragMode: PlayerTouchDragMode? = null
        var initialSeekPositionMs = 0L
        var seekDurationMs = 0L
        var seekTargetMs = -1L
        var initialBrightness = TouchDefaultBrightness
        var initialVolume = 0
        var maxVolume = 1

        detectDragGestures(
          onDragStart = { offset ->
            dragStart = offset
            dragTotal = Offset.Zero
            dragMode = null
            initialSeekPositionMs = 0L
            seekDurationMs = 0L
            seekTargetMs = -1L
            initialBrightness = currentWindowBrightness(activity)
            maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
            initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.coerceIn(0, maxVolume) ?: 0
            if (
              showUnfollowConfirm ||
              activePanel != PlayerPanel.None ||
              completionReported ||
              speedBoostActive ||
              offset.y > size.height - bottomGestureIgnoreHeightPx
            ) {
              dragMode = PlayerTouchDragMode.Ignored
            }
          },
          onDrag = { change, dragAmount ->
            if (dragMode == PlayerTouchDragMode.Ignored) {
              return@detectDragGestures
            }

            dragTotal += dragAmount
            val width = size.width.toFloat().coerceAtLeast(1f)
            val height = size.height.toFloat().coerceAtLeast(1f)
            if (dragMode == null) {
              val deltaX = abs(dragTotal.x)
              val deltaY = abs(dragTotal.y)
              dragMode = when {
                deltaX > deltaY -> {
                  if (dragStart.x > width - edgeSwipeWidthPx) {
                    PlayerTouchDragMode.EdgeBack
                  } else {
                    initialSeekPositionMs = player.currentPosition.takeIf { it >= 0L } ?: 0L
                    seekDurationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
                    seekTargetMs = initialSeekPositionMs
                    onTouchSeekStart()
                    PlayerTouchDragMode.Seek
                  }
                }
                deltaY > deltaX -> {
                  if (dragStart.x < width / 2f) {
                    PlayerTouchDragMode.Brightness
                  } else {
                    PlayerTouchDragMode.Volume
                  }
                }
                else -> null
              }
            }

            when (dragMode) {
              PlayerTouchDragMode.Seek -> {
                change.consume()
                val seekChangeMs = ((dragTotal.x / width) * TouchSeekRangeMs).roundToLong()
                val maxSeek = seekDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                val nextTargetMs = (initialSeekPositionMs + seekChangeMs).coerceIn(0L, maxSeek)
                if (nextTargetMs != seekTargetMs) {
                  seekTargetMs = nextTargetMs
                  onTouchSeekPreview(nextTargetMs, nextTargetMs - initialSeekPositionMs)
                }
              }
              PlayerTouchDragMode.Brightness -> {
                change.consume()
                val nextBrightness = (initialBrightness + (-dragTotal.y / height))
                  .coerceIn(TouchMinimumBrightness, 1f)
                setWindowBrightness(activity, nextBrightness)
                onBrightnessChanged((nextBrightness * 100f).roundToInt().coerceIn(0, 100))
              }
              PlayerTouchDragMode.Volume -> {
                change.consume()
                val nextVolume = (initialVolume + ((-dragTotal.y / height) * maxVolume))
                  .roundToInt()
                  .coerceIn(0, maxVolume)
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
                val percent = ((nextVolume.toFloat() / maxVolume.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                onVolumeChanged(percent)
              }
              PlayerTouchDragMode.EdgeBack -> {
                change.consume()
              }
              PlayerTouchDragMode.Ignored,
              null -> Unit
            }
          },
          onDragEnd = {
            val width = size.width.toFloat().coerceAtLeast(1f)
            when (dragMode) {
              PlayerTouchDragMode.Seek -> onTouchSeekEnd()
              PlayerTouchDragMode.Brightness,
              PlayerTouchDragMode.Volume -> onTouchAdjustmentEnd()
              PlayerTouchDragMode.EdgeBack -> {
                val commitDistancePx = edgeSwipeWidthPx * TouchEdgeBackCommitRatio
                val rightEdgeBack = dragStart.x > width - edgeSwipeWidthPx && dragTotal.x < -commitDistancePx
                if (rightEdgeBack) {
                  onExitGesture()
                }
              }
              PlayerTouchDragMode.Ignored,
              null -> Unit
            }
            dragMode = null
          },
          onDragCancel = {
            when (dragMode) {
              PlayerTouchDragMode.Seek -> onTouchSeekCancel()
              PlayerTouchDragMode.Brightness,
              PlayerTouchDragMode.Volume -> onTouchAdjustmentEnd()
              PlayerTouchDragMode.EdgeBack,
              PlayerTouchDragMode.Ignored,
              null -> Unit
            }
            dragMode = null
          },
        )
      },
  )
}

@Composable
private fun PlayerTouchFeedbackOverlay(
  feedback: PlayerTouchFeedback,
  modifier: Modifier = Modifier,
) {
  val performancePolicy = LocalBiliPerformancePolicy.current
  val refined = performancePolicy.cinematicVisualEffectsEnabled
  val shape = RoundedCornerShape(BiliRadius.Panel)
  val feedbackSize = when {
    performancePolicy.lowSpecMode -> BiliSizing.PlayerTouchFeedbackCompactSize
    refined -> BiliSizing.PlayerTouchFeedbackRefinedSize
    else -> BiliSizing.PlayerTouchFeedbackSize
  }
  val iconSize = if (refined) {
    BiliSizing.PlayerTouchFeedbackRefinedIconSize
  } else {
    BiliSizing.PlayerTouchFeedbackIconSize
  }
  val surfaceAlpha = when {
    performancePolicy.lowSpecMode -> BiliFocus.PlayerTouchFeedbackSmoothSurfaceAlpha
    refined -> BiliFocus.PlayerTouchFeedbackRefinedSurfaceAlpha
    else -> BiliFocus.PlayerTouchFeedbackBalancedSurfaceAlpha
  }
  val borderAlpha = when {
    performancePolicy.lowSpecMode -> BiliFocus.PlayerTouchFeedbackSmoothBorderAlpha
    refined -> BiliFocus.PlayerTouchFeedbackRefinedBorderAlpha
    else -> BiliFocus.PlayerTouchFeedbackBalancedBorderAlpha
  }
  val iconRes = when (feedback.type) {
    PlayerTouchFeedbackType.Brightness -> R.drawable.ic_player_brightness
    PlayerTouchFeedbackType.Volume -> R.drawable.ic_player_volume
    PlayerTouchFeedbackType.SpeedBoost -> R.drawable.ic_player_speed_boost
  }
  val contentDescription = when (feedback.type) {
    PlayerTouchFeedbackType.Brightness -> stringResource(R.string.player_touch_brightness, feedback.percent)
    PlayerTouchFeedbackType.Volume -> stringResource(R.string.player_touch_volume, feedback.percent)
    PlayerTouchFeedbackType.SpeedBoost -> stringResource(R.string.player_touch_speed_boost)
  }

  Box(
    modifier = modifier
      .size(feedbackSize)
      .clip(shape)
      .biliLiquidGlassSurface(
        enabled = refined && performancePolicy.liquidGlassCardsEnabled,
        shape = shape,
        surfaceColor = BiliColors.PlayerPanel.copy(alpha = surfaceAlpha),
        borderColor = BiliColors.TextPrimary.copy(alpha = borderAlpha),
        borderWidth = BiliFocus.RestingBorderWidth,
      )
      .background(
        color = BiliColors.Transparent,
        shape = RoundedCornerShape(BiliRadius.Panel),
      ),
    contentAlignment = Alignment.Center,
  ) {
    if (feedback.type != PlayerTouchFeedbackType.SpeedBoost) {
      PlayerTouchFeedbackProgressRing(
        percent = feedback.percent,
        modifier = Modifier
          .fillMaxSize()
          .padding(BiliSpacing.Sm),
      )
    }
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = BiliColors.TextPrimary,
      modifier = Modifier.size(iconSize),
    )
  }
}

@Composable
private fun PlayerTouchSpeedBoostOverlay(
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(BiliSizing.PlayerTouchSpeedBoostHeight / 2)
  Row(
    modifier = modifier
      .size(
        width = BiliSizing.PlayerTouchSpeedBoostWidth,
        height = BiliSizing.PlayerTouchSpeedBoostHeight,
      )
      .background(
        color = BiliColors.PlayerPanel.copy(alpha = BiliFocus.PlayerTouchFeedbackBalancedSurfaceAlpha),
        shape = shape,
      )
      .border(
        width = BiliFocus.RestingBorderWidth,
        color = BiliColors.BiliPink.copy(alpha = BiliFocus.PlayerTouchFeedbackTrackAlpha),
        shape = shape,
      )
      .padding(
        horizontal = BiliSizing.PlayerTouchSpeedBoostHorizontalPadding,
        vertical = BiliSizing.PlayerTouchSpeedBoostVerticalPadding,
      ),
    horizontalArrangement = Arrangement.spacedBy(
      space = BiliSpacing.Sm,
      alignment = Alignment.CenterHorizontally,
    ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_player_speed_boost),
      contentDescription = stringResource(R.string.player_touch_speed_boost),
      tint = BiliColors.BiliPink,
      modifier = Modifier.size(BiliSizing.PlayerTouchSpeedBoostIconSize),
    )
    Text(
      text = stringResource(R.string.player_touch_speed_boost_badge),
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerStatus,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun PlayerTouchFeedbackProgressRing(
  percent: Int,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val strokeWidth = BiliSizing.PlayerTouchFeedbackRingStroke.toPx()
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    drawArc(
      color = BiliColors.TextPrimary.copy(alpha = BiliFocus.PlayerTouchFeedbackTrackAlpha),
      startAngle = -90f,
      sweepAngle = 360f,
      useCenter = false,
      style = stroke,
    )
    drawArc(
      color = BiliColors.BiliPink,
      startAngle = -90f,
      sweepAngle = percent.coerceIn(0, 100) * 3.6f,
      useCenter = false,
      style = stroke,
    )
  }
}

private fun currentWindowBrightness(activity: Activity?): Float {
  val brightness = activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
  return brightness.takeIf { value -> value >= 0f } ?: TouchDefaultBrightness
}

private fun setWindowBrightness(activity: Activity?, brightness: Float) {
  val window = activity?.window ?: return
  val attributes = window.attributes
  attributes.screenBrightness = brightness.coerceIn(TouchMinimumBrightness, 1f)
  window.attributes = attributes
}

private enum class PlayerTouchDragMode {
  Seek,
  Brightness,
  Volume,
  EdgeBack,
  Ignored,
}

private const val SeekStepMs = 10_000L
private const val TouchSeekRangeMs = 90_000L
private const val TouchSpeedBoostRate = 2.0f
private const val TouchDefaultBrightness = 0.5f
private const val TouchMinimumBrightness = 0.02f
private const val TouchEdgeBackCommitRatio = 0.625f
private const val OnlineCountRefreshMs = 60_000L
private const val ExitConfirmWindowMs = 3_000L
private const val CompletedProgressSeconds = -1
private const val CompletionActionDelayMs = 3_000L
private const val PlayerWakeLockTimeoutMs = 10 * 60 * 1000L
private const val AirJumpWarningLeadMs = 3_500L
private const val AirJumpCompletionToastSuppressMs = 1_500L
private const val AirJumpRewindResetThresholdMs = 2_000L
private const val AirJumpRewindResetLeadMs = 1_000L
private const val PlayerDanmakuLogTag = "BiliTVNative:Danmaku"
private val DanmakuOpacityOptions = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)
private val DanmakuFontSizeOptions = listOf(16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36)
private val DanmakuAreaOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
private val DanmakuSpeedOptions = listOf(3, 4, 5, 6, 7)
