package com.kirin.bilitv.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.common.VideoGridSkeleton
import com.kirin.bilitv.ui.home.AdaptiveVideoGrid
import com.kirin.bilitv.ui.home.VideoCardMode

@Stable
internal class UserFeedFocusState {
  var focusedVideoIndex by mutableIntStateOf(0)
  var focusedVideoKey by mutableStateOf("")

  fun clear() {
    focusedVideoIndex = 0
    focusedVideoKey = ""
  }
}

@Composable
internal fun DynamicFeedScreen(
  viewModel: DynamicFeedViewModel,
  isLoggedIn: Boolean,
  focusState: UserFeedFocusState,
  autoRefreshOnSwitch: Boolean,
  manualRefreshKey: Int,
  firstItemFocusRequester: FocusRequester,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  if (!isLoggedIn) {
    FeedStatusScreen(message = stringResource(R.string.dynamic_signed_out))
    return
  }

  val viewState by viewModel.viewState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel, autoRefreshOnSwitch) {
    val returningFromPlayback = restoreFocusRequestKey > 0
    if (autoRefreshOnSwitch && !returningFromPlayback) {
      focusState.clear()
    }
    viewModel.loadFirstPage(forceRefresh = autoRefreshOnSwitch && !returningFromPlayback)
  }

  LaunchedEffect(viewModel, manualRefreshKey) {
    if (viewModel.refreshForManualKey(manualRefreshKey)) {
      focusState.clear()
    }
  }

  UserFeedContent(
    state = viewState.state,
    emptyMessage = stringResource(R.string.dynamic_empty),
    failedMessage = { message -> stringResource(R.string.dynamic_failed_with_message, message) },
    cardMode = VideoCardMode.Dynamic,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = focusState.focusedVideoIndex,
    restoredFocusKey = focusState.focusedVideoKey,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = { index, video ->
      focusState.focusedVideoIndex = index
      focusState.focusedVideoKey = video.focusRestoreKey()
    },
    onRetry = {
      focusState.clear()
      viewModel.retry()
    },
    onLoadMore = viewModel::loadNextPage,
    onRefresh = {
      focusState.clear()
      viewModel.loadFirstPage(forceRefresh = true)
    },
    onMoveLeftToNav = onMoveLeftToNav,
    onVideoSelected = onVideoSelected,
  )
}

@Composable
internal fun HistoryFeedScreen(
  viewModel: HistoryFeedViewModel,
  isLoggedIn: Boolean,
  focusState: UserFeedFocusState,
  autoRefreshOnSwitch: Boolean,
  manualRefreshKey: Int,
  firstItemFocusRequester: FocusRequester,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  if (!isLoggedIn) {
    FeedStatusScreen(message = stringResource(R.string.history_signed_out))
    return
  }

  val viewState by viewModel.viewState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel, autoRefreshOnSwitch) {
    val returningFromPlayback = restoreFocusRequestKey > 0
    if (autoRefreshOnSwitch && !returningFromPlayback) {
      focusState.clear()
    }
    viewModel.loadFirstPage(forceRefresh = autoRefreshOnSwitch && !returningFromPlayback)
  }

  LaunchedEffect(viewModel, manualRefreshKey) {
    if (viewModel.refreshForManualKey(manualRefreshKey)) {
      focusState.clear()
    }
  }

  UserFeedContent(
    state = viewState.state,
    emptyMessage = stringResource(R.string.history_empty),
    failedMessage = { message -> stringResource(R.string.history_failed_with_message, message) },
    cardMode = VideoCardMode.History,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = focusState.focusedVideoIndex,
    restoredFocusKey = focusState.focusedVideoKey,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = { index, video ->
      focusState.focusedVideoIndex = index
      focusState.focusedVideoKey = video.focusRestoreKey()
    },
    onRetry = {
      focusState.clear()
      viewModel.retry()
    },
    onLoadMore = viewModel::loadNextPage,
    onRefresh = {
      focusState.clear()
      viewModel.loadFirstPage(forceRefresh = true)
    },
    onMoveLeftToNav = onMoveLeftToNav,
    onVideoSelected = onVideoSelected,
  )
}

@Composable
private fun UserFeedContent(
  state: UserFeedState,
  emptyMessage: String,
  failedMessage: @Composable (String) -> String,
  cardMode: VideoCardMode,
  firstItemFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoredFocusKey: String,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onRetry: () -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: () -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  when (state) {
    UserFeedState.Loading -> VideoGridSkeleton()
    UserFeedState.Empty -> FeedStatusScreen(message = emptyMessage)
    is UserFeedState.Failed -> FeedStatusScreen(
      message = failedMessage(state.message),
      actionLabel = stringResource(R.string.action_retry),
      onAction = onRetry,
    )
    is UserFeedState.Success -> UserFeedGrid(
      videos = state.videos,
      cardMode = cardMode,
      firstItemFocusRequester = firstItemFocusRequester,
      restoredFocusIndex = state.videos.resolveFocusIndex(
        focusKey = restoredFocusKey,
        fallbackIndex = restoredFocusIndex,
      ),
      restoreFocusRequestKey = restoreFocusRequestKey,
      onRestoreFocusHandled = onRestoreFocusHandled,
      onFocusedIndexChange = onFocusedIndexChange,
      onLoadMore = onLoadMore,
      onRefresh = onRefresh,
      onMoveLeftToNav = onMoveLeftToNav,
      onVideoSelected = onVideoSelected,
    )
  }
}

@Composable
private fun UserFeedGrid(
  videos: List<VideoSummary>,
  cardMode: VideoCardMode,
  firstItemFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: () -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  AdaptiveVideoGrid(
    videos = videos,
    cardMode = cardMode,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = restoredFocusIndex,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = onFocusedIndexChange,
    onLoadMore = onLoadMore,
    onRefresh = onRefresh,
    onMoveLeftToNav = onMoveLeftToNav,
    onVideoSelected = onVideoSelected,
    keyFactory = { index, video -> video.userFeedKey(index) },
  )
}

private fun List<VideoSummary>.resolveFocusIndex(focusKey: String, fallbackIndex: Int): Int {
  val keyIndex = focusKey
    .takeIf { key -> key.isNotBlank() }
    ?.let { key -> indexOfFirst { video -> video.focusRestoreKey() == key } }
    ?.takeIf { index -> index >= 0 }
  return keyIndex ?: fallbackIndex.coerceIn(0, lastIndex)
}

private fun VideoSummary.focusRestoreKey(): String {
  return bvid.ifBlank {
    when {
      cid > 0L -> "cid-$cid"
      historyPage > 0 -> "p-$historyPage"
      viewAt > 0L -> "view-$viewAt"
      else -> ""
    }
  }
}
