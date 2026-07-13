package com.kirin.bilitv.ui.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import com.kirin.bilitv.R
import com.kirin.bilitv.core.auth.AuthRepository
import com.kirin.bilitv.core.cache.AppCacheManager
import com.kirin.bilitv.core.image.buildVideoThumbnailRequest
import com.kirin.bilitv.core.i18n.ChineseTextConverters
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.core.player.CodecCapabilityProbe
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackRepository
import com.kirin.bilitv.core.player.PlaybackRequest
import com.kirin.bilitv.core.player.DanmakuSettingsStore
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.model.isWatchCompleted
import com.kirin.bilitv.core.model.shouldAdvanceToNextHistoryEpisode
import com.kirin.bilitv.core.settings.AppPerformancePolicy
import com.kirin.bilitv.core.settings.AppSettings
import com.kirin.bilitv.core.settings.AppSettingsStore
import com.kirin.bilitv.core.settings.supportsLiquidGlassCards
import com.kirin.bilitv.core.storage.SearchHistoryStore
import com.kirin.bilitv.core.storage.SessionStore
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.feed.DynamicFeedScreen
import com.kirin.bilitv.ui.feed.DynamicFeedViewModel
import com.kirin.bilitv.ui.feed.HistoryFeedScreen
import com.kirin.bilitv.ui.feed.HistoryFeedViewModel
import com.kirin.bilitv.ui.feed.UserFeedFocusState
import com.kirin.bilitv.ui.home.RecommendFocusState
import com.kirin.bilitv.ui.home.RecommendScreen
import com.kirin.bilitv.ui.home.RecommendViewModel
import com.kirin.bilitv.ui.glass.LocalLiquidGlassBackdrop
import com.kirin.bilitv.ui.i18n.LocalChineseTextConverter
import com.kirin.bilitv.ui.i18n.localizedContext
import com.kirin.bilitv.ui.input.InteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile
import com.kirin.bilitv.ui.input.rememberInteractionProfile
import com.kirin.bilitv.ui.login.AccountScreen
import com.kirin.bilitv.ui.player.PlaybackSessionViewModel
import com.kirin.bilitv.ui.player.PlayerScreen
import com.kirin.bilitv.ui.search.SearchFocusState
import com.kirin.bilitv.ui.search.SearchScreen
import com.kirin.bilitv.ui.search.SearchViewModel
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.settings.SettingsScreen
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.HomeColorScheme
import com.kirin.bilitv.ui.theme.HomeThemes
import com.kirin.bilitv.ui.theme.LocalHomeColors
import com.kirin.bilitv.ui.transition.PlaybackSharedAnimatedScope
import com.kirin.bilitv.ui.transition.PlaybackSharedTransitionLayout
import com.kirin.bilitv.ui.transition.PlaybackTransitionPolicy
import com.kirin.bilitv.ui.transition.playbackSharedBounds
import com.kirin.bilitv.ui.transition.playbackSharedTransitionKey
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private const val PlaybackFocusRestoreRetryCount = 8
private const val ExitConfirmWindowMs = 3_000L

private fun isConstrainedTvUiDevice(): Boolean {
  val buildValues = listOf(
    Build.HARDWARE,
    Build.BOARD,
    Build.DEVICE,
    Build.PRODUCT,
    Build.MODEL,
    Build.MANUFACTURER,
    buildStringField("SOC_MODEL"),
    buildStringField("SOC_MANUFACTURER"),
  )
  val normalizedValues = buildValues.map { value -> value.orEmpty().lowercase(Locale.ROOT) }
  return normalizedValues.any { value -> value.contains("mt9655") } ||
    (normalizedValues.any { value -> value.contains("xiaomi") } &&
      normalizedValues.any { value -> value.contains("mitv-mffu1") })
}

private fun buildStringField(name: String): String {
  return runCatching {
    Build::class.java.getField(name).get(null) as? String
  }.getOrDefault("").orEmpty()
}

@Composable
fun BiliTvApp(
  videoRepository: VideoRepository,
  playbackRepository: PlaybackRepository,
  danmakuSettingsStore: DanmakuSettingsStore,
  playbackHttpClient: OkHttpClient,
  codecCapabilityProbe: CodecCapabilityProbe,
  authRepository: AuthRepository,
  appSettingsStore: AppSettingsStore,
  appCacheManager: AppCacheManager,
  searchHistoryStore: SearchHistoryStore,
  sessionStore: SessionStore,
) {
  val settings by appSettingsStore.settings.collectAsState(initial = AppSettings())
  val context = LocalContext.current
  val localizedContext = remember(context, settings.chineseTextVariant) {
    context.localizedContext(settings.chineseTextVariant)
  }
  val textConverter = remember(settings.chineseTextVariant) {
    ChineseTextConverters.forVariant(settings.chineseTextVariant)
  }
  val userSession by sessionStore.session.collectAsState(initial = UserSession())
  val interactionProfile = rememberInteractionProfile()
  val interactionMode = interactionProfile.legacyMode
  val tvInteractionEnabled = interactionMode == InteractionMode.Tv
  val codecCapability = remember(codecCapabilityProbe) { codecCapabilityProbe.probe() }
  val autoConfirmOnFocus = settings.autoConfirmOnFocus && tvInteractionEnabled
  val autoRefreshOnSwitch = autoConfirmOnFocus && settings.autoRefreshOnSwitch
  val liquidGlassCardsSupported = remember { supportsLiquidGlassCards() }
  val constrainedTvUiDevice = remember { isConstrainedTvUiDevice() }
  val performancePolicy = remember(settings.visualPerformanceMode, settings.liquidGlassCardsEnabled, constrainedTvUiDevice) {
    AppPerformancePolicy.fromSettings(
      settings = settings,
      constrainedTvUi = constrainedTvUiDevice,
    )
  }
  val homeColors = remember(settings.homeThemeVariant) {
    HomeThemes.fromVariant(settings.homeThemeVariant)
  }
  val liquidGlassBackdrop = rememberLayerBackdrop()
  val activeLiquidGlassBackdrop = liquidGlassBackdrop.takeIf {
    performancePolicy.liquidGlassCardsEnabled && liquidGlassCardsSupported
  }
  val effectivePlaybackCodecPreference = if (settings.lowSpecMode) {
    PlaybackCodecPreference.H264
  } else {
    settings.playbackCodecPreference
  }
  val recommendViewModel: RecommendViewModel = viewModel(
    factory = remember(videoRepository) {
      RecommendViewModel.factory(videoRepository)
    },
  )
  val searchViewModel: SearchViewModel = viewModel(
    factory = remember(videoRepository, searchHistoryStore) {
      SearchViewModel.factory(
        videoRepository = videoRepository,
        searchHistoryStore = searchHistoryStore,
      )
    },
  )
  val dynamicFeedViewModel: DynamicFeedViewModel = viewModel(
    factory = remember(videoRepository) {
      DynamicFeedViewModel.factory(videoRepository)
    },
  )
  val historyFeedViewModel: HistoryFeedViewModel = viewModel(
    factory = remember(videoRepository) {
      HistoryFeedViewModel.factory(videoRepository)
    },
  )
  val playbackSessionViewModel: PlaybackSessionViewModel = viewModel(
    factory = PlaybackSessionViewModel.Factory,
  )
  val playbackRequest by playbackSessionViewModel.activeRequest.collectAsState()
  val navigationViewModel: AppShellNavigationViewModel = viewModel()
  val navigationState by navigationViewModel.state.collectAsState()
  val selectedDestination = navigationState.selectedDestination
  val visitedDestinations = navigationState.visitedDestinations
  val accountSelected = navigationState.accountSelected
  val coroutineScope = rememberCoroutineScope()
  val shellFocusState = remember { AppShellFocusState() }
  val recommendFocusState = remember { RecommendFocusState() }
  val dynamicFeedFocusState = remember { UserFeedFocusState() }
  val historyFeedFocusState = remember { UserFeedFocusState() }
  val searchFocusState = remember { SearchFocusState() }
  var initialHomeFocusPending by remember { mutableStateOf(true) }
  var startupShellFocusPending by remember {
    mutableStateOf(tvInteractionEnabled && playbackRequest == null)
  }
  var recommendManualRefreshKey by rememberSaveable { mutableStateOf(0) }
  var dynamicManualRefreshKey by rememberSaveable { mutableStateOf(0) }
  var historyManualRefreshKey by rememberSaveable { mutableStateOf(0) }
  var lastAppExitBackPressMs by remember { mutableStateOf(0L) }
  var appExitConfirmToast by remember { mutableStateOf<Toast?>(null) }
  var cacheSizeBytes by remember { mutableStateOf<Long?>(null) }

  LaunchedEffect(performancePolicy.imageMemoryCacheEnabled) {
    if (!performancePolicy.imageMemoryCacheEnabled) {
      context.imageLoader.memoryCache?.clear()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      appExitConfirmToast?.cancel()
    }
  }

  fun showAppExitConfirmToast() {
    appExitConfirmToast?.cancel()
    appExitConfirmToast = Toast.makeText(localizedContext, R.string.app_exit_confirm_toast, Toast.LENGTH_SHORT).also { toast ->
      toast.show()
    }
  }

  fun cancelAppExitConfirmToast() {
    appExitConfirmToast?.cancel()
    appExitConfirmToast = null
  }

  fun refreshCacheSize() {
    coroutineScope.launch {
      cacheSizeBytes = appCacheManager.cacheSizeBytes()
    }
  }

  fun requestManualRefresh(destination: AppDestination) {
    when (destination) {
      AppDestination.Recommend -> recommendManualRefreshKey += 1
      AppDestination.Dynamic -> dynamicManualRefreshKey += 1
      AppDestination.History -> historyManualRefreshKey += 1
      else -> Unit
    }
  }

  fun selectDestination(destination: AppDestination) {
    if (selectedDestination == AppDestination.Search && destination != AppDestination.Search) {
      searchViewModel.clear()
      searchFocusState.clear()
    }
    val destinationChanged = selectedDestination != destination
    if (!destinationChanged) {
      requestManualRefresh(destination)
    }
    navigationViewModel.selectDestination(destination)
  }

  fun moveIntoDestination(destination: AppDestination): Boolean {
    if (accountSelected) {
      return false
    }
    if (selectedDestination != destination) {
      selectDestination(destination)
      shellFocusState.requestContentFocusRestore(destination)
      return true
    }
    val focused = shellFocusState.requestDestinationFocus(destination)
    if (!focused) {
      shellFocusState.requestContentFocusRestore(destination)
    }
    return true
  }

  fun requestSidebarFocus(): Boolean {
    return runCatching {
      if (accountSelected) {
        shellFocusState.accountFocusRequester.requestFocus()
      } else {
        shellFocusState.navFocusRequesters.getValue(selectedDestination).requestFocus()
      }
    }.isSuccess
  }

  fun VideoSummary.toPlaybackRequest(forceStartPosition: Boolean = false): PlaybackRequest {
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
      forceStartPosition = forceStartPosition,
      historyPage = historyPage,
      advanceToNextHistoryEpisode = advanceToNextEpisode,
    )
  }

  LaunchedEffect(userSession.isLoggedIn, tvInteractionEnabled) {
    if (userSession.isLoggedIn && accountSelected) {
      selectDestination(AppDestination.Recommend)
      if (tvInteractionEnabled) {
        runCatching {
          shellFocusState.contentFocusRequester.requestFocus()
        }
      }
    }
  }

  LaunchedEffect(userSession.isLoggedIn, userSession.face, userSession.uname) {
    if (userSession.isLoggedIn && (userSession.face.isNullOrBlank() || userSession.uname.isNullOrBlank())) {
      runCatching {
        authRepository.refreshUserProfile()
      }
    }
  }

  LaunchedEffect(selectedDestination) {
    if (selectedDestination == AppDestination.Settings) {
      refreshCacheSize()
    }
  }

  CompositionLocalProvider(
    LocalContext provides localizedContext,
    LocalBiliPerformancePolicy provides performancePolicy,
	    LocalChineseTextConverter provides textConverter,
	    LocalInteractionProfile provides interactionProfile,
	    LocalInteractionMode provides interactionMode,
    LocalHomeColors provides homeColors,
    LocalLiquidGlassBackdrop provides activeLiquidGlassBackdrop,
  ) {
    val activePlaybackRequest = playbackRequest
    var visiblePlaybackRequest by remember { mutableStateOf(activePlaybackRequest) }
    var playbackSharedKey by remember { mutableStateOf<String?>(null) }
    var playbackSharedThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var playbackExitFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var playbackExitCoverVisible by remember { mutableStateOf(false) }
    var playerContentVisible by remember { mutableStateOf(activePlaybackRequest != null) }
    var playbackSharedTransitionActive by remember { mutableStateOf(false) }
    var playbackTransitionToken by remember { mutableStateOf(0) }
    LaunchedEffect(
      startupShellFocusPending,
      activePlaybackRequest,
      tvInteractionEnabled,
      selectedDestination,
      accountSelected,
    ) {
      if (!startupShellFocusPending || activePlaybackRequest != null || !tvInteractionEnabled) {
        return@LaunchedEffect
      }
      repeat(PlaybackFocusRestoreRetryCount) {
        withFrameNanos { }
        val focused = runCatching {
          if (accountSelected) {
            shellFocusState.accountFocusRequester.requestFocus()
          } else {
            shellFocusState.navFocusRequesters.getValue(selectedDestination).requestFocus()
          }
        }.getOrDefault(false)
        if (focused) {
          startupShellFocusPending = false
          return@LaunchedEffect
        }
      }
      startupShellFocusPending = false
    }

    LaunchedEffect(activePlaybackRequest, shellFocusState.pendingContentFocusDestination, selectedDestination, accountSelected) {
      if (activePlaybackRequest != null) {
        return@LaunchedEffect
      }
      val destination = shellFocusState.pendingContentFocusDestination ?: return@LaunchedEffect
      if (accountSelected || selectedDestination != destination) {
        return@LaunchedEffect
      }
      repeat(PlaybackFocusRestoreRetryCount) {
        withFrameNanos { }
        if (shellFocusState.requestDestinationFocus(destination)) {
          shellFocusState.clearPendingContentFocusDestination()
          return@LaunchedEffect
        }
      }
    }

    fun startPlaybackFromCard(
      video: VideoSummary,
      forceStartPosition: Boolean = false,
    ) {
      if (visiblePlaybackRequest != null || playbackSharedTransitionActive) {
        return
      }
      val request = video.toPlaybackRequest(forceStartPosition = forceStartPosition)
      val playbackFocusOrigin = when (selectedDestination) {
        AppDestination.Recommend -> PlaybackFocusOrigin(
          destination = selectedDestination,
          index = recommendFocusState.focusedVideoIndex,
          key = recommendFocusState.focusedVideoKey,
        )
        AppDestination.Search -> PlaybackFocusOrigin(
          destination = selectedDestination,
          index = searchFocusState.focusedResultIndex,
          key = searchFocusState.focusedResultKey,
        )
        AppDestination.Dynamic -> PlaybackFocusOrigin(
          destination = selectedDestination,
          index = dynamicFeedFocusState.focusedVideoIndex,
          key = dynamicFeedFocusState.focusedVideoKey,
        )
        AppDestination.History -> PlaybackFocusOrigin(
          destination = selectedDestination,
          index = historyFeedFocusState.focusedVideoIndex,
          key = historyFeedFocusState.focusedVideoKey,
        )
        AppDestination.Settings -> null
      }
      playbackFocusOrigin?.let(navigationViewModel::rememberPlaybackFocusOrigin)
      val sharedTransitionEnabled = PlaybackTransitionPolicy.shouldAnimateEntry(
        motionEnabled = performancePolicy.motionEnabled,
        thumbnailAvailable = video.pic.isNotBlank(),
      )
      playbackSessionViewModel.startOrUpdate(request)
      visiblePlaybackRequest = request
      playbackExitFrame = null
      playbackExitCoverVisible = false
      if (!sharedTransitionEnabled) {
        playbackSharedKey = null
        playbackSharedThumbnailUrl = null
        playerContentVisible = true
        playbackSharedTransitionActive = false
        playbackTransitionToken += 1
        return
      }

      playbackSharedKey = video.playbackSharedTransitionKey()
      playbackSharedThumbnailUrl = video.pic
      playerContentVisible = false
      playbackSharedTransitionActive = true
      val token = playbackTransitionToken + 1
      playbackTransitionToken = token
      coroutineScope.launch {
        delay(BiliMotion.PlaybackHeroTransitionMs.toLong())
        if (playbackTransitionToken != token) {
          return@launch
        }
        playerContentVisible = true
        playbackSharedTransitionActive = false
      }
    }

    fun finishPlaybackWithSharedTransition() {
      val playbackFocusOrigin = navigationViewModel.consumePlaybackFocusOrigin()
      playbackFocusOrigin?.let { origin ->
        when (origin.destination) {
          AppDestination.Recommend -> {
            recommendFocusState.focusedVideoIndex = origin.index
            recommendFocusState.focusedVideoKey = origin.key
          }
          AppDestination.Search -> {
            searchFocusState.focusedResultIndex = origin.index
            searchFocusState.focusedResultKey = origin.key
          }
          AppDestination.Dynamic -> {
            dynamicFeedFocusState.focusedVideoIndex = origin.index
            dynamicFeedFocusState.focusedVideoKey = origin.key
          }
          AppDestination.History -> {
            historyFeedFocusState.focusedVideoIndex = origin.index
            historyFeedFocusState.focusedVideoKey = origin.key
          }
          AppDestination.Settings -> Unit
        }
      }
      shellFocusState.requestPlaybackFocusRestore(
        playbackFocusOrigin?.destination ?: selectedDestination,
      )
      val sharedTransitionEnabled = PlaybackTransitionPolicy.shouldAnimateExit(
        motionEnabled = performancePolicy.motionEnabled,
        sharedKeyAvailable = playbackSharedKey != null,
        exitFrameAvailable = playbackExitFrame != null,
      )
      val token = playbackTransitionToken + 1
      playbackTransitionToken = token
      if (!sharedTransitionEnabled) {
        playerContentVisible = false
        playbackSessionViewModel.clear()
        visiblePlaybackRequest = null
        playbackSharedKey = null
        playbackSharedThumbnailUrl = null
        playbackExitFrame = null
        playbackExitCoverVisible = false
        playbackSharedTransitionActive = false
        return
      }

      playbackSharedTransitionActive = true
      playbackExitCoverVisible = true
      coroutineScope.launch {
        withFrameNanos { }
        if (playbackTransitionToken != token) {
          return@launch
        }
        playerContentVisible = false
        playbackSessionViewModel.clear()
        visiblePlaybackRequest = null
        delay(BiliMotion.PlaybackHeroTransitionMs.toLong())
        withFrameNanos { }
        if (playbackTransitionToken != token) {
          return@launch
        }
        playbackSharedKey = null
        playbackSharedThumbnailUrl = null
        playbackExitFrame = null
        playbackExitCoverVisible = false
        playbackSharedTransitionActive = false
      }
    }

    PlaybackSharedTransitionLayout {
      AnimatedContent(
        targetState = visiblePlaybackRequest,
        transitionSpec = {
          if (
            PlaybackTransitionPolicy.shouldRetainOutgoingContent(
              motionEnabled = performancePolicy.motionEnabled,
              sharedTransitionActive = playbackSharedTransitionActive,
            )
          ) {
            fadeIn(animationSpec = tween(durationMillis = 1)) togetherWith
              fadeOut(
                animationSpec = tween(durationMillis = BiliMotion.PlaybackHeroTransitionMs),
                targetAlpha = 1f,
              ) using
              SizeTransform(clip = false)
          } else {
            EnterTransition.None togetherWith ExitTransition.None
          }
        },
        label = "playbackSharedTransition",
      ) { displayedPlaybackRequest ->
        PlaybackSharedAnimatedScope(animatedVisibilityScope = this) {
      if (displayedPlaybackRequest == null) {
        Box(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .then(
              if (activeLiquidGlassBackdrop != null) {
                Modifier.layerBackdrop(liquidGlassBackdrop)
              } else {
                Modifier
              },
            ),
        ) {
          HomeAppBackground(
            colors = homeColors,
            refinedVisualsEnabled = performancePolicy.refinedVisualEffectsEnabled,
            cinematicVisualsEnabled = performancePolicy.cinematicVisualEffectsEnabled,
          )
        }
    BackHandler(enabled = activePlaybackRequest == null && !playbackSharedTransitionActive) {
          val now = SystemClock.elapsedRealtime()
          if (now - lastAppExitBackPressMs <= ExitConfirmWindowMs) {
            cancelAppExitConfirmToast()
            context.findActivity()?.finish()
          } else {
            lastAppExitBackPressMs = now
            showAppExitConfirmToast()
          }
        }
	        AdaptiveAppScaffold(
	          selectedDestination = selectedDestination,
	          accountSelected = accountSelected,
	          userSession = userSession,
	          autoConfirmOnFocus = autoConfirmOnFocus && !startupShellFocusPending,
	          accountFocusRequester = shellFocusState.accountFocusRequester,
	          navFocusRequesters = shellFocusState.navFocusRequesters,
	          contentPaddingEnabled = accountSelected || selectedDestination != AppDestination.Search,
	          onAccountSelected = {
	            navigationViewModel.selectAccount()
	          },
	          onDestinationSelected = { destination ->
	            selectDestination(destination)
	          },
	          shouldAutoConfirmDestination = { destination ->
	            shouldAutoConfirmDestinationOnFocus(
	              autoConfirmOnFocus = autoConfirmOnFocus,
	              destinationVisited = destination in visitedDestinations,
	              startupFocusPending = startupShellFocusPending,
	            )
	          },
	          onMoveRight = { destination ->
	            moveIntoDestination(destination)
	          },
	        ) {
	            if (accountSelected) {
	              AccountScreen(
	                userSession = userSession,
	                authRepository = authRepository,
	              )
            } else {
              when (selectedDestination) {
                AppDestination.Recommend -> RecommendScreen(
                  viewModel = recommendViewModel,
                  focusState = recommendFocusState,
                  firstItemFocusRequester = shellFocusState.contentFocusRequester,
                  enabledHomeSections = settings.enabledHomeSections,
                  autoConfirmOnFocus = autoConfirmOnFocus,
                  autoRefreshOnSwitch = autoRefreshOnSwitch,
                  manualRefreshKey = recommendManualRefreshKey,
                  restoreFocusRequestKey = shellFocusState.restoreFocusRequestKeyFor(AppDestination.Recommend),
                  onRestoreFocusHandled = { key ->
                    shellFocusState.clearFocusRestoreRequest(AppDestination.Recommend, key)
                  },
                  requestInitialFocus = tvInteractionEnabled && initialHomeFocusPending,
                  onInitialFocusRequested = {
                    initialHomeFocusPending = false
                  },
                  onMoveLeftToNav = ::requestSidebarFocus,
                  onVideoSelected = { video ->
                    startPlaybackFromCard(video)
                  },
                )
                AppDestination.Search -> SearchScreen(
                  viewModel = searchViewModel,
                  focusState = searchFocusState,
                  firstItemFocusRequester = shellFocusState.searchFocusRequester,
                  restoreFocusRequestKey = shellFocusState.restoreFocusRequestKeyFor(AppDestination.Search),
                  onRestoreFocusHandled = { key ->
                    shellFocusState.clearFocusRestoreRequest(AppDestination.Search, key)
                  },
                  onMoveLeftToNav = ::requestSidebarFocus,
                  onVideoSelected = { video ->
                    startPlaybackFromCard(video)
                  },
                )
                AppDestination.History -> HistoryFeedScreen(
                  viewModel = historyFeedViewModel,
                  isLoggedIn = userSession.isLoggedIn,
                  focusState = historyFeedFocusState,
                  autoRefreshOnSwitch = autoRefreshOnSwitch,
                  manualRefreshKey = historyManualRefreshKey,
                  firstItemFocusRequester = shellFocusState.historyFocusRequester,
                  restoreFocusRequestKey = shellFocusState.restoreFocusRequestKeyFor(AppDestination.History),
                  onRestoreFocusHandled = { key ->
                    shellFocusState.clearFocusRestoreRequest(AppDestination.History, key)
                  },
                  onMoveLeftToNav = ::requestSidebarFocus,
                  onVideoSelected = { video ->
                    startPlaybackFromCard(video, forceStartPosition = true)
                  },
                )
                AppDestination.Dynamic -> DynamicFeedScreen(
                  viewModel = dynamicFeedViewModel,
                  isLoggedIn = userSession.isLoggedIn,
                  focusState = dynamicFeedFocusState,
                  autoRefreshOnSwitch = autoRefreshOnSwitch,
                  manualRefreshKey = dynamicManualRefreshKey,
                  firstItemFocusRequester = shellFocusState.dynamicFocusRequester,
                  restoreFocusRequestKey = shellFocusState.restoreFocusRequestKeyFor(AppDestination.Dynamic),
                  onRestoreFocusHandled = { key ->
                    shellFocusState.clearFocusRestoreRequest(AppDestination.Dynamic, key)
                  },
                  onMoveLeftToNav = ::requestSidebarFocus,
                  onVideoSelected = { video ->
                    startPlaybackFromCard(video)
                  },
                )
                AppDestination.Settings -> SettingsScreen(
                  settings = settings,
                  cacheSizeText = cacheSizeBytes?.let(::formatCacheSize) ?: stringResource(R.string.settings_clear_cache_calculating),
                  codecCapability = codecCapability,
                  firstItemFocusRequester = shellFocusState.settingsFocusRequester,
                  onMoveLeftToNav = ::requestSidebarFocus,
                  onVisualPerformanceModeChange = { mode ->
                    coroutineScope.launch {
                      appSettingsStore.setVisualPerformanceMode(mode)
                    }
                  },
                  liquidGlassCardsSupported = liquidGlassCardsSupported,
                  onLiquidGlassCardsEnabledChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setLiquidGlassCardsEnabled(enabled)
                    }
                  },
                  onHomeThemeVariantChange = { variant ->
                    coroutineScope.launch {
                      appSettingsStore.setHomeThemeVariant(variant)
                    }
                  },
                  onChineseTextVariantChange = { variant ->
                    coroutineScope.launch {
                      appSettingsStore.setChineseTextVariant(variant)
                    }
                  },
                  onClearCache = {
                    coroutineScope.launch {
                      val result = appCacheManager.clearCache()
                      cacheSizeBytes = appCacheManager.cacheSizeBytes()
                      Toast.makeText(
                        localizedContext,
                        localizedContext.getString(R.string.settings_clear_cache_done, formatCacheSize(result.clearedBytes)),
                        Toast.LENGTH_SHORT,
                      ).show()
                    }
                  },
                  onSeekPreviewSpritesEnabledChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setSeekPreviewSpritesEnabled(enabled)
                    }
                  },
                  onPlaybackQualityPreferenceChange = { preference ->
                    coroutineScope.launch {
                      appSettingsStore.setPlaybackQualityPreference(preference)
                    }
                  },
                  onPlaybackCodecPreferenceChange = { preference ->
                    coroutineScope.launch {
                      appSettingsStore.setPlaybackCodecPreference(preference)
                    }
                  },
                  onAirJumpAssistantEnabledChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setAirJumpAssistantEnabled(enabled)
                    }
                  },
                  onConfirmPlaybackExitChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setConfirmPlaybackExit(enabled)
                    }
                  },
                  onAutoPlayNextEpisodeChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setAutoPlayNextEpisode(enabled)
                    }
                  },
                  onAutoPlayRelatedVideoChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setAutoPlayRelatedVideo(enabled)
                    }
                  },
                  onAutoReturnHomeOnCompletionChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setAutoReturnHomeOnCompletion(enabled)
                    }
                  },
                  onShowClockChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setShowClock(enabled)
                    }
                  },
                  onShowMiniProgressBarChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setShowMiniProgressBar(enabled)
                    }
                  },
                  onAutoConfirmOnFocusChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setAutoConfirmOnFocus(enabled)
                    }
                  },
                  onAutoRefreshOnSwitchChange = { enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setAutoRefreshOnSwitch(enabled)
                    }
                  },
                  onHomeSectionEnabledChange = { section, enabled ->
                    coroutineScope.launch {
                      appSettingsStore.setHomeSectionEnabled(section, enabled)
                    }
	                  },
	                )
	              }
	            }
	        }
	        if (playbackSharedTransitionActive) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(BiliColors.VideoBlack.copy(alpha = BiliFocus.PlaybackHeroScrimAlpha)),
          )
        }
      }
      } else {
        val playerContainerBackground = if (playerContentVisible) {
          BiliColors.VideoBlack
        } else {
          BiliColors.VideoBlack.copy(alpha = 0f)
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(playerContainerBackground),
        ) {
          if (playerContentVisible) {
            PlayerScreen(
              request = displayedPlaybackRequest,
              videoRepository = videoRepository,
              playbackRepository = playbackRepository,
              danmakuSettingsStore = danmakuSettingsStore,
              playbackHttpClient = playbackHttpClient,
              playbackCodecPreference = effectivePlaybackCodecPreference,
              playbackQualityPreference = settings.playbackQualityPreference,
              seekPreviewSpritesEnabled = settings.seekPreviewSpritesEnabled,
              airJumpAssistantEnabled = settings.airJumpAssistantEnabled,
              confirmPlaybackExit = settings.confirmPlaybackExit,
              autoPlayNextEpisode = settings.autoPlayNextEpisode,
              autoPlayRelatedVideo = settings.autoPlayRelatedVideo,
              autoReturnHomeOnCompletion = settings.autoReturnHomeOnCompletion,
              showClock = settings.showClock,
              showMiniProgressBar = settings.showMiniProgressBar,
              captureExitFrame = performancePolicy.motionEnabled && playbackSharedKey != null,
              onExitFrameReady = { frame ->
                playbackExitFrame = frame
              },
              onPlaybackRequestChanged = playbackSessionViewModel::startOrUpdate,
              onBack = {
                finishPlaybackWithSharedTransition()
              },
            )
          }
          PlaybackSharedCoverTarget(
            sharedKey = playbackSharedKey,
            thumbnailUrl = playbackSharedThumbnailUrl,
            exitFrame = playbackExitFrame,
            visible = !playerContentVisible || playbackExitCoverVisible,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      }
    }
  }
}
}

@Composable
private fun PlaybackSharedCoverTarget(
  sharedKey: String?,
  thumbnailUrl: String?,
  exitFrame: ImageBitmap?,
  visible: Boolean,
  modifier: Modifier = Modifier,
) {
  if (sharedKey.isNullOrBlank() || !visible) {
    return
  }
  val sharedModifier = Modifier
    .then(modifier)
    .playbackSharedBounds(sharedKey)
  if (exitFrame != null) {
    Image(
      bitmap = exitFrame,
      contentDescription = null,
      contentScale = ContentScale.FillBounds,
      modifier = sharedModifier,
    )
  } else if (!thumbnailUrl.isNullOrBlank()) {
    val fallbackPainter = ColorPainter(BiliColors.Transparent)
    val context = LocalContext.current
    val performancePolicy = LocalBiliPerformancePolicy.current
    val thumbnailRequest = remember(
      context,
      thumbnailUrl,
      performancePolicy.videoThumbnailWidthPx,
      performancePolicy.videoThumbnailHeightPx,
      performancePolicy.videoThumbnailRgb565Enabled,
      performancePolicy.imageMemoryCacheEnabled,
    ) {
      buildVideoThumbnailRequest(
        context = context,
        url = thumbnailUrl,
        widthPx = performancePolicy.videoThumbnailWidthPx,
        heightPx = performancePolicy.videoThumbnailHeightPx,
        allowRgb565 = performancePolicy.videoThumbnailRgb565Enabled,
        memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
      )
    }
    AsyncImage(
      model = thumbnailRequest,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      placeholder = fallbackPainter,
      error = fallbackPainter,
      modifier = sharedModifier,
    )
  } else {
    Box(
      modifier = sharedModifier.background(BiliColors.VideoBlack),
    )
  }
}

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}

private fun formatCacheSize(bytes: Long): String {
  val safeBytes = bytes.coerceAtLeast(0L)
  val mb = safeBytes / (1024.0 * 1024.0)
  return if (mb >= 1.0) {
    String.format(Locale.US, "%.1f MB", mb)
  } else {
    String.format(Locale.US, "%.0f KB", safeBytes / 1024.0)
  }
}

@Composable
private fun HomeAppBackground(
  colors: HomeColorScheme,
  refinedVisualsEnabled: Boolean,
  cinematicVisualsEnabled: Boolean,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(colors.backgroundTop, colors.backgroundBottom),
        ),
      ),
  ) {
    if (cinematicVisualsEnabled) {
      val drift = BiliFocus.HomeBackgroundCinematicDrift
      Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = maxOf(size.width, size.height)
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              colors.backgroundTop,
              colors.backgroundBottom,
              colors.cardSurface.copy(alpha = BiliFocus.HomeBackgroundCinematicCardSurfaceAlpha),
            ),
          ),
        )
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(colors.ambientA.copy(alpha = BiliFocus.HomeBackgroundCinematicAmbientAAlpha), BiliColors.Transparent),
            center = Offset(
              x = size.width * (BiliFocus.HomeBackgroundCinematicAmbientAX + drift * BiliFocus.HomeBackgroundCinematicAmbientADriftX),
              y = size.height * (BiliFocus.HomeBackgroundCinematicAmbientAY + drift * BiliFocus.HomeBackgroundCinematicAmbientADriftY),
            ),
            radius = radius * BiliFocus.HomeBackgroundCinematicAmbientARadius,
          ),
        )
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(colors.ambientB.copy(alpha = BiliFocus.HomeBackgroundCinematicAmbientBAlpha), BiliColors.Transparent),
            center = Offset(
              x = size.width * (BiliFocus.HomeBackgroundCinematicAmbientBX - drift * BiliFocus.HomeBackgroundCinematicAmbientBDriftX),
              y = size.height * (BiliFocus.HomeBackgroundCinematicAmbientBY + drift * BiliFocus.HomeBackgroundCinematicAmbientBDriftY),
            ),
            radius = radius * BiliFocus.HomeBackgroundCinematicAmbientBRadius,
          ),
        )
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(colors.ambientA.copy(alpha = BiliFocus.HomeBackgroundCinematicAmbientCAlpha), BiliColors.Transparent),
            center = Offset(
              x = size.width * (BiliFocus.HomeBackgroundCinematicAmbientCX + drift * BiliFocus.HomeBackgroundCinematicAmbientCDriftX),
              y = size.height * (BiliFocus.HomeBackgroundCinematicAmbientCY - drift * BiliFocus.HomeBackgroundCinematicAmbientCDriftY),
            ),
            radius = radius * BiliFocus.HomeBackgroundCinematicAmbientCRadius,
          ),
        )
        val bokehColor = colors.textPrimary.copy(alpha = BiliFocus.HomeBackgroundCinematicBokehAlpha)
        BiliFocus.HomeBackgroundCinematicBokehDots.forEach { dot ->
          val center = Offset(
            x = size.width * dot.xFraction,
            y = size.height * dot.yFraction,
          )
          drawRect(
            brush = Brush.radialGradient(
              colors = listOf(bokehColor, BiliColors.Transparent),
              center = center + Offset(
                x = drift * BiliFocus.HomeBackgroundCinematicBokehDriftX,
                y = drift * BiliFocus.HomeBackgroundCinematicBokehDriftY,
              ),
              radius = dot.radius + drift * BiliFocus.HomeBackgroundCinematicBokehRadiusDrift,
            ),
          )
        }
      }
    } else if (refinedVisualsEnabled) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = maxOf(size.width, size.height)
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(colors.ambientA, BiliColors.Transparent),
            center = Offset(
              x = size.width * BiliFocus.HomeBackgroundRefinedAmbientAX,
              y = size.height * BiliFocus.HomeBackgroundRefinedAmbientAY,
            ),
            radius = radius * BiliFocus.HomeBackgroundRefinedAmbientARadius,
          ),
        )
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(colors.ambientB, BiliColors.Transparent),
            center = Offset(
              x = size.width * BiliFocus.HomeBackgroundRefinedAmbientBX,
              y = size.height * BiliFocus.HomeBackgroundRefinedAmbientBY,
            ),
            radius = radius * BiliFocus.HomeBackgroundRefinedAmbientBRadius,
          ),
        )
      }
    }
  }
}

@Composable
private fun ComingSoonScreen(
  @StringRes titleRes: Int,
  @StringRes messageRes: Int,
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = stringResource(titleRes),
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.ScreenTitle,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(messageRes),
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.Body,
      modifier = Modifier.padding(top = BiliSpacing.Md),
    )
  }
}
