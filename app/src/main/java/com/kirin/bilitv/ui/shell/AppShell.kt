package com.kirin.bilitv.ui.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.imageLoader
import com.kirin.bilitv.R
import com.kirin.bilitv.core.auth.AuthRepository
import com.kirin.bilitv.core.cache.AppCacheManager
import com.kirin.bilitv.core.image.BiliImageSizing
import com.kirin.bilitv.core.image.buildOwnerAvatarRequest
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
import com.kirin.bilitv.core.storage.SearchHistoryStore
import com.kirin.bilitv.core.storage.SessionStore
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.common.ClockOverlay
import com.kirin.bilitv.ui.feed.DynamicFeedScreen
import com.kirin.bilitv.ui.feed.DynamicFeedUiState
import com.kirin.bilitv.ui.feed.HistoryFeedScreen
import com.kirin.bilitv.ui.feed.HistoryFeedUiState
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.home.RecommendScreen
import com.kirin.bilitv.ui.home.RecommendUiState
import com.kirin.bilitv.ui.i18n.LocalChineseTextConverter
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.i18n.localizedContext
import com.kirin.bilitv.ui.login.AccountScreen
import com.kirin.bilitv.ui.player.PlayerScreen
import com.kirin.bilitv.ui.search.SearchScreen
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.settings.SettingsScreen
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import java.util.Locale
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private const val PlaybackFocusRestoreRetryCount = 8
private const val PlaybackFocusRestoreCleanupFrameCount = 30
private const val ExitConfirmWindowMs = 3_000L

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
  val codecCapability = remember(codecCapabilityProbe) { codecCapabilityProbe.probe() }
  val autoConfirmOnFocus = settings.autoConfirmOnFocus
  val autoRefreshOnSwitch = settings.autoConfirmOnFocus && settings.autoRefreshOnSwitch
  val performancePolicy = remember(settings.lowSpecMode) {
    AppPerformancePolicy.fromSettings(settings)
  }
  val effectivePlaybackCodecPreference = if (settings.lowSpecMode) {
    PlaybackCodecPreference.H264
  } else {
    settings.playbackCodecPreference
  }
  val coroutineScope = rememberCoroutineScope()
  var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.Recommend) }
  var visitedDestinationNames by rememberSaveable { mutableStateOf(setOf(AppDestination.Recommend.name)) }
  var accountSelected by rememberSaveable { mutableStateOf(false) }
  val accountFocusRequester = remember { FocusRequester() }
  val navFocusRequesters = remember {
    AppDestination.entries.associateWith { FocusRequester() }
  }
  val contentFocusRequester = remember { FocusRequester() }
  val searchFocusRequester = remember { FocusRequester() }
  val dynamicFocusRequester = remember { FocusRequester() }
  val historyFocusRequester = remember { FocusRequester() }
  val settingsFocusRequester = remember { FocusRequester() }
  val recommendUiState = remember { RecommendUiState() }
  val dynamicFeedState = remember { DynamicFeedUiState() }
  val historyFeedState = remember { HistoryFeedUiState() }
  var initialHomeFocusPending by remember { mutableStateOf(true) }
  var recommendManualRefreshKey by rememberSaveable { mutableStateOf(0) }
  var dynamicManualRefreshKey by rememberSaveable { mutableStateOf(0) }
  var historyManualRefreshKey by rememberSaveable { mutableStateOf(0) }
  var playbackRequest by remember { mutableStateOf<PlaybackRequest?>(null) }
  var playbackFocusRestoreDestination by remember { mutableStateOf<AppDestination?>(null) }
  var playbackFocusRestoreRequestKey by remember { mutableIntStateOf(0) }
  var contentFocusRestoreDestination by remember { mutableStateOf<AppDestination?>(null) }
  var contentFocusRestoreRequestKey by remember { mutableIntStateOf(0) }
  var lastAppExitBackPressMs by remember { mutableStateOf(0L) }
  var appExitConfirmToast by remember { mutableStateOf<Toast?>(null) }
  var pendingContentFocusDestination by remember { mutableStateOf<AppDestination?>(null) }
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

  fun requestDestinationFocus(destination: AppDestination): Boolean {
    return runCatching {
      when (destination) {
        AppDestination.Recommend -> contentFocusRequester.requestFocus()
        AppDestination.Search -> searchFocusRequester.requestFocus()
        AppDestination.Dynamic -> dynamicFocusRequester.requestFocus()
        AppDestination.History -> historyFocusRequester.requestFocus()
        AppDestination.Settings -> settingsFocusRequester.requestFocus()
      }
    }.getOrDefault(false)
  }

  fun restoreFocusRequestKeyFor(destination: AppDestination): Int {
    return when {
      playbackFocusRestoreDestination == destination -> playbackFocusRestoreRequestKey
      contentFocusRestoreDestination == destination -> contentFocusRestoreRequestKey
      else -> 0
    }
  }

  fun clearFocusRestoreRequest(destination: AppDestination, key: Int) {
    if (playbackFocusRestoreDestination == destination && key == playbackFocusRestoreRequestKey) {
      playbackFocusRestoreDestination = null
    }
    if (contentFocusRestoreDestination == destination && key == contentFocusRestoreRequestKey) {
      contentFocusRestoreDestination = null
      pendingContentFocusDestination = null
    }
  }

  fun AppDestination.usesGridFocusRestore(): Boolean {
    return this == AppDestination.Recommend || this == AppDestination.Dynamic || this == AppDestination.History
  }

  fun requestContentFocusRestore(destination: AppDestination) {
    if (destination.usesGridFocusRestore()) {
      contentFocusRestoreDestination = destination
      contentFocusRestoreRequestKey += 1
      pendingContentFocusDestination = null
    } else {
      pendingContentFocusDestination = destination
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
    accountSelected = false
    val destinationChanged = selectedDestination != destination
    if (!destinationChanged) {
      requestManualRefresh(destination)
    }
    selectedDestination = destination
    visitedDestinationNames = visitedDestinationNames + destination.name
  }

  fun moveIntoDestination(destination: AppDestination): Boolean {
    if (accountSelected) {
      return false
    }
    if (selectedDestination != destination) {
      selectDestination(destination)
      requestContentFocusRestore(destination)
      return true
    }
    val focused = requestDestinationFocus(destination)
    if (!focused) {
      requestContentFocusRestore(destination)
    }
    return true
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

  LaunchedEffect(userSession.isLoggedIn) {
    if (userSession.isLoggedIn && accountSelected) {
      selectDestination(AppDestination.Recommend)
      runCatching {
        contentFocusRequester.requestFocus()
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
  ) {
    val activePlaybackRequest = playbackRequest
    LaunchedEffect(pendingContentFocusDestination, selectedDestination, accountSelected) {
      val destination = pendingContentFocusDestination ?: return@LaunchedEffect
      if (accountSelected || selectedDestination != destination) {
        return@LaunchedEffect
      }
      repeat(PlaybackFocusRestoreRetryCount) {
        withFrameNanos { }
        if (requestDestinationFocus(destination)) {
          pendingContentFocusDestination = null
          return@LaunchedEffect
        }
      }
    }

    LaunchedEffect(activePlaybackRequest, playbackFocusRestoreDestination, playbackFocusRestoreRequestKey) {
      val restoreDestination = playbackFocusRestoreDestination
      val restoreRequestKey = playbackFocusRestoreRequestKey
      if (activePlaybackRequest == null && restoreDestination != null && restoreRequestKey > 0) {
        repeat(PlaybackFocusRestoreCleanupFrameCount) {
          withFrameNanos { }
        }
        if (playbackFocusRestoreDestination == restoreDestination && playbackFocusRestoreRequestKey == restoreRequestKey) {
          playbackFocusRestoreDestination = null
        }
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      BackHandler(enabled = activePlaybackRequest == null) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAppExitBackPressMs <= ExitConfirmWindowMs) {
          cancelAppExitConfirmToast()
          context.findActivity()?.finish()
        } else {
          lastAppExitBackPressMs = now
          showAppExitConfirmToast()
        }
      }
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(BiliColors.Background),
      ) {
        AppSidebar(
          selectedDestination = selectedDestination,
          accountSelected = accountSelected,
          userSession = userSession,
          autoConfirmOnFocus = autoConfirmOnFocus,
          accountFocusRequester = accountFocusRequester,
          navFocusRequesters = navFocusRequesters,
          onAccountSelected = {
            accountSelected = true
          },
          onDestinationSelected = { destination ->
            selectDestination(destination)
          },
          shouldAutoConfirmDestination = { destination ->
            autoConfirmOnFocus || destination.name !in visitedDestinationNames
          },
          onMoveRight = { destination ->
            moveIntoDestination(destination)
          },
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .then(
              if (!accountSelected && selectedDestination == AppDestination.Search) {
                Modifier
              } else {
                Modifier.padding(BiliSizing.ContentPadding)
              },
            ),
        ) {
          if (accountSelected) {
            AccountScreen(
              userSession = userSession,
              authRepository = authRepository,
            )
          } else when (selectedDestination) {
            AppDestination.Recommend -> RecommendScreen(
              videoRepository = videoRepository,
              uiState = recommendUiState,
              firstItemFocusRequester = contentFocusRequester,
              enabledHomeSections = settings.enabledHomeSections,
              autoConfirmOnFocus = autoConfirmOnFocus,
              autoRefreshOnSwitch = autoRefreshOnSwitch,
              manualRefreshKey = recommendManualRefreshKey,
              restoreFocusRequestKey = restoreFocusRequestKeyFor(AppDestination.Recommend),
              onRestoreFocusHandled = { key -> clearFocusRestoreRequest(AppDestination.Recommend, key) },
              requestInitialFocus = initialHomeFocusPending,
              onInitialFocusRequested = {
                initialHomeFocusPending = false
              },
              onMoveLeftToNav = {
                runCatching {
                  if (accountSelected) {
                    accountFocusRequester.requestFocus()
                  } else {
                    navFocusRequesters.getValue(selectedDestination).requestFocus()
                  }
                }.isSuccess
              },
              onVideoSelected = { video ->
                playbackRequest = video.toPlaybackRequest()
              },
            )
            AppDestination.Search -> SearchScreen(
              videoRepository = videoRepository,
              searchHistoryStore = searchHistoryStore,
              firstItemFocusRequester = searchFocusRequester,
              restoreFocusRequestKey = restoreFocusRequestKeyFor(AppDestination.Search),
              onRestoreFocusHandled = { key -> clearFocusRestoreRequest(AppDestination.Search, key) },
              onMoveLeftToNav = {
                runCatching {
                  if (accountSelected) {
                    accountFocusRequester.requestFocus()
                  } else {
                    navFocusRequesters.getValue(selectedDestination).requestFocus()
                  }
                }.isSuccess
              },
              onVideoSelected = { video ->
                playbackRequest = video.toPlaybackRequest()
              },
            )
            AppDestination.History -> HistoryFeedScreen(
              videoRepository = videoRepository,
              isLoggedIn = userSession.isLoggedIn,
              feedState = historyFeedState,
              autoRefreshOnSwitch = autoRefreshOnSwitch,
              manualRefreshKey = historyManualRefreshKey,
              firstItemFocusRequester = historyFocusRequester,
              restoreFocusRequestKey = restoreFocusRequestKeyFor(AppDestination.History),
              onRestoreFocusHandled = { key -> clearFocusRestoreRequest(AppDestination.History, key) },
              onMoveLeftToNav = {
                runCatching {
                  navFocusRequesters.getValue(selectedDestination).requestFocus()
                }.isSuccess
              },
              onVideoSelected = { video ->
                playbackRequest = video.toPlaybackRequest(forceStartPosition = true)
              },
            )
            AppDestination.Dynamic -> DynamicFeedScreen(
              videoRepository = videoRepository,
              isLoggedIn = userSession.isLoggedIn,
              feedState = dynamicFeedState,
              autoRefreshOnSwitch = autoRefreshOnSwitch,
              manualRefreshKey = dynamicManualRefreshKey,
              firstItemFocusRequester = dynamicFocusRequester,
              restoreFocusRequestKey = restoreFocusRequestKeyFor(AppDestination.Dynamic),
              onRestoreFocusHandled = { key -> clearFocusRestoreRequest(AppDestination.Dynamic, key) },
              onMoveLeftToNav = {
                runCatching {
                  navFocusRequesters.getValue(selectedDestination).requestFocus()
                }.isSuccess
              },
              onVideoSelected = { video ->
                playbackRequest = video.toPlaybackRequest()
              },
            )
            AppDestination.Settings -> SettingsScreen(
              settings = settings,
              cacheSizeText = cacheSizeBytes?.let(::formatCacheSize) ?: stringResource(R.string.settings_clear_cache_calculating),
              codecCapability = codecCapability,
              firstItemFocusRequester = settingsFocusRequester,
              onMoveLeftToNav = {
                runCatching {
                  if (accountSelected) {
                    accountFocusRequester.requestFocus()
                  } else {
                    navFocusRequesters.getValue(selectedDestination).requestFocus()
                  }
                }.isSuccess
              },
              onLowSpecModeChange = { enabled ->
                coroutineScope.launch {
                  appSettingsStore.setLowSpecMode(enabled)
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
      if (settings.showClock && activePlaybackRequest == null) {
        ClockOverlay(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(
              top = BiliSizing.ClockOverlayTopPadding,
              end = BiliSizing.ClockOverlayEndPadding,
            ),
        )
      }
      if (activePlaybackRequest != null) {
        PlayerScreen(
          request = activePlaybackRequest,
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
          onBack = {
            playbackFocusRestoreDestination = selectedDestination
            playbackRequest = null
            playbackFocusRestoreRequestKey += 1
          },
        )
      }
    }
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
private fun AppSidebar(
  selectedDestination: AppDestination,
  accountSelected: Boolean,
  userSession: UserSession,
  autoConfirmOnFocus: Boolean,
  accountFocusRequester: FocusRequester,
  navFocusRequesters: Map<AppDestination, FocusRequester>,
  onAccountSelected: () -> Unit,
  onDestinationSelected: (AppDestination) -> Unit,
  shouldAutoConfirmDestination: (AppDestination) -> Boolean,
  onMoveRight: (AppDestination) -> Boolean,
) {
  Column(
    modifier = Modifier
      .width(BiliSizing.SidebarWidth)
      .fillMaxHeight()
      .background(BiliColors.Surface)
      .padding(horizontal = BiliSpacing.Lg, vertical = BiliSizing.ContentPadding),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(BiliSpacing.Lg),
  ) {
    if (userSession.isLoggedIn) {
      AccountStatusAvatar(userSession = userSession)
    } else {
      AccountNavItem(
        selected = accountSelected,
        userSession = userSession,
        autoConfirmOnFocus = autoConfirmOnFocus,
        modifier = Modifier.focusRequester(accountFocusRequester),
        onClick = onAccountSelected,
        onMoveRight = {
          onMoveRight(selectedDestination)
        },
      )
    }
    Spacer(modifier = Modifier.height(BiliSpacing.Sm))
    AppDestination.entries.forEach { destination ->
      AppNavItem(
        destination = destination,
        selected = !accountSelected && selectedDestination == destination,
        autoConfirmOnFocus = shouldAutoConfirmDestination(destination),
        modifier = Modifier.focusRequester(navFocusRequesters.getValue(destination)),
        onClick = {
          onDestinationSelected(destination)
        },
        onMoveRight = {
          onMoveRight(destination)
        },
      )
    }
    Spacer(modifier = Modifier.weight(1f))
  }
}

@Composable
private fun AccountNavItem(
  selected: Boolean,
  userSession: UserSession,
  autoConfirmOnFocus: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
  onMoveRight: () -> Boolean,
) {
  BiliFocusableSurface(
    shape = CircleShape,
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.NavItemHeight)
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
          onMoveRight()
        } else {
          false
        }
      },
    onClick = onClick,
    onFocused = {
      if (autoConfirmOnFocus && !selected) {
        onClick()
      }
    },
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      AccountAvatar(userSession = userSession)
    }
  }
}

@Composable
private fun AccountStatusAvatar(userSession: UserSession) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.NavItemHeight)
      .focusProperties { canFocus = false },
    contentAlignment = Alignment.Center,
  ) {
    AccountAvatar(userSession = userSession)
  }
}

@Composable
private fun AccountAvatar(userSession: UserSession) {
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val accountAvatarRequestSizePx = if (performancePolicy.lowSpecMode) {
    performancePolicy.ownerAvatarSizePx
  } else {
    BiliImageSizing.AccountAvatarSizePx
  }
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val face = userSession.face.orEmpty()

  Box(
    modifier = Modifier.size(BiliSizing.AccountAvatarContainerSize),
    contentAlignment = Alignment.Center,
  ) {
    if (userSession.isLoggedIn && face.isNotBlank()) {
      val request = remember(
        context,
        face,
        accountAvatarRequestSizePx,
        performancePolicy.ownerAvatarRgb565Enabled,
        performancePolicy.imageMemoryCacheEnabled,
      ) {
        buildOwnerAvatarRequest(
          context = context,
          url = face,
          sizePx = accountAvatarRequestSizePx,
          allowRgb565 = performancePolicy.ownerAvatarRgb565Enabled,
          memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
        )
      }
      AsyncImage(
        model = request,
        contentDescription = userSession.uname?.let { name -> convertChineseText(name) } ?: stringResource(R.string.account_logged_in_default),
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = Modifier
          .align(Alignment.Center)
          .size(BiliSizing.AccountAvatarSize)
          .clip(CircleShape)
          .background(BiliColors.SurfaceElevated),
      )
    } else {
      Icon(
        painter = painterResource(R.drawable.ic_nav_account),
        contentDescription = stringResource(R.string.nav_login),
        tint = if (userSession.isLoggedIn) BiliColors.BiliPink else BiliColors.TextSecondary,
        modifier = Modifier.size(BiliSizing.NavIconSize),
      )
    }

    if (userSession.isLoggedIn && userSession.isVip) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = BiliSpacing.Xs, y = BiliSpacing.Xs)
          .size(BiliSizing.AccountVipBadgeSize)
          .clip(CircleShape)
          .background(BiliColors.BiliPink),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.account_vip_badge),
          color = BiliColors.TextPrimary,
          fontSize = BiliTypography.AccountVipBadge,
          lineHeight = BiliTypography.AccountVipBadgeLineHeight,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun AppNavItem(
  destination: AppDestination,
  selected: Boolean,
  autoConfirmOnFocus: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
  onMoveRight: () -> Boolean,
) {
  var focused by remember { mutableStateOf(false) }

  BiliFocusableSurface(
    shape = RoundedCornerShape(BiliRadius.Pill),
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.NavItemHeight)
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
          onMoveRight()
        } else {
          false
      }
    },
    onFocusChanged = { focused = it },
    onClick = onClick,
    onFocused = {
      if (autoConfirmOnFocus && !selected) {
        onClick()
      }
    },
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(BiliSpacing.Sm),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(destination.iconRes),
        contentDescription = stringResource(destination.titleRes),
        tint = if (selected || focused) BiliColors.BiliPink else BiliColors.TextSecondary,
        modifier = Modifier
          .width(BiliSizing.NavIconSize)
          .height(BiliSizing.NavIconSize),
      )
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
