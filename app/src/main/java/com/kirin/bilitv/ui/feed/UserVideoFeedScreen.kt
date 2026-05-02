package com.kirin.bilitv.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.common.VideoThumbnailPrefetcher
import com.kirin.bilitv.ui.common.VideoGridSkeleton
import com.kirin.bilitv.ui.home.TvVideoGrid
import com.kirin.bilitv.ui.home.VideoCard
import com.kirin.bilitv.ui.home.VideoCardMode
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
internal class DynamicFeedUiState {
  var nextOffset by mutableStateOf("")
  var state by mutableStateOf<UserFeedState>(UserFeedState.Loading)
  var focusedVideoIndex by mutableIntStateOf(0)
  var focusedVideoKey by mutableStateOf("")
  var hasLoadedContent by mutableStateOf(false)
  var loadedOnce by mutableStateOf(false)
  var handledManualRefreshKey by mutableIntStateOf(0)
}

@Stable
internal class HistoryFeedUiState {
  var nextViewAt by mutableStateOf(0L)
  var nextMax by mutableStateOf(0L)
  var state by mutableStateOf<UserFeedState>(UserFeedState.Loading)
  var focusedVideoIndex by mutableIntStateOf(0)
  var focusedVideoKey by mutableStateOf("")
  var hasLoadedContent by mutableStateOf(false)
  var loadedOnce by mutableStateOf(false)
  var handledManualRefreshKey by mutableIntStateOf(0)
}

@Composable
internal fun DynamicFeedScreen(
  videoRepository: VideoRepository,
  isLoggedIn: Boolean,
  feedState: DynamicFeedUiState,
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

  val coroutineScope = rememberCoroutineScope()

  suspend fun loadFirstPage(forceRefresh: Boolean) {
    if (!forceRefresh && feedState.loadedOnce) {
      return
    }

    feedState.state = UserFeedState.Loading
    feedState.focusedVideoIndex = 0
    feedState.focusedVideoKey = ""
    feedState.nextOffset = ""
    feedState.state = try {
      val page = videoRepository.getDynamicFeed()
      feedState.nextOffset = page.offset
      feedState.loadedOnce = true
      if (page.videos.isEmpty()) {
        UserFeedState.Empty
      } else {
        feedState.hasLoadedContent = true
        UserFeedState.Success(
          videos = page.videos,
          loadingMore = false,
          endReached = !page.hasMore,
          loadMoreError = "",
        )
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      feedState.loadedOnce = true
      UserFeedState.Failed(error.message.orEmpty())
    }
  }

  LaunchedEffect(videoRepository, isLoggedIn, autoRefreshOnSwitch) {
    loadFirstPage(forceRefresh = autoRefreshOnSwitch)
  }

  LaunchedEffect(manualRefreshKey) {
    if (manualRefreshKey > 0 && manualRefreshKey != feedState.handledManualRefreshKey) {
      feedState.handledManualRefreshKey = manualRefreshKey
      loadFirstPage(forceRefresh = true)
    }
  }

  fun loadNextPage() {
    val currentState = feedState.state as? UserFeedState.Success ?: return
    if (currentState.loadingMore || currentState.endReached) {
      return
    }

    val offsetToLoad = feedState.nextOffset
    feedState.state = currentState.copy(loadingMore = true, loadMoreError = "")
    coroutineScope.launch {
      feedState.state = try {
        val page = videoRepository.getDynamicFeed(offset = offsetToLoad)
        feedState.nextOffset = page.offset
        val latestState = feedState.state as? UserFeedState.Success ?: return@launch
        val mergedVideos = latestState.videos.appendUnique(nextVideos = page.videos)
        if (mergedVideos.isNotEmpty()) {
          feedState.hasLoadedContent = true
        }
        latestState.copy(
          videos = mergedVideos,
          loadingMore = false,
          endReached = !page.hasMore ||
            page.videos.isEmpty() ||
            mergedVideos.size == latestState.videos.size,
          loadMoreError = "",
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        val latestState = feedState.state as? UserFeedState.Success ?: return@launch
        latestState.copy(loadingMore = false, loadMoreError = error.message.orEmpty())
      }
    }
  }

  UserFeedContent(
    state = feedState.state,
    loadingMessage = stringResource(R.string.dynamic_loading),
    emptyMessage = stringResource(R.string.dynamic_empty),
    failedMessage = { message -> stringResource(R.string.dynamic_failed_with_message, message) },
    cardMode = VideoCardMode.Dynamic,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = feedState.focusedVideoIndex,
    restoredFocusKey = feedState.focusedVideoKey,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = { index, video ->
      feedState.focusedVideoIndex = index
      feedState.focusedVideoKey = video.focusRestoreKey()
    },
    onRetry = {
      coroutineScope.launch {
        loadFirstPage(forceRefresh = true)
      }
    },
    onLoadMore = ::loadNextPage,
    onMoveLeftToNav = onMoveLeftToNav,
    onVideoSelected = onVideoSelected,
  )
}

@Composable
internal fun HistoryFeedScreen(
  videoRepository: VideoRepository,
  isLoggedIn: Boolean,
  feedState: HistoryFeedUiState,
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

  val coroutineScope = rememberCoroutineScope()

  suspend fun loadFirstPage(forceRefresh: Boolean) {
    if (!forceRefresh && feedState.loadedOnce) {
      return
    }

    feedState.state = UserFeedState.Loading
    feedState.focusedVideoIndex = 0
    feedState.focusedVideoKey = ""
    feedState.nextViewAt = 0L
    feedState.nextMax = 0L
    feedState.state = try {
      val page = videoRepository.getHistoryPage()
      feedState.nextViewAt = page.nextViewAt
      feedState.nextMax = page.nextMax
      feedState.loadedOnce = true
      if (page.videos.isEmpty()) {
        UserFeedState.Empty
      } else {
        feedState.hasLoadedContent = true
        UserFeedState.Success(
          videos = page.videos,
          loadingMore = false,
          endReached = !page.hasMore,
          loadMoreError = "",
        )
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      feedState.loadedOnce = true
      UserFeedState.Failed(error.message.orEmpty())
    }
  }

  LaunchedEffect(videoRepository, isLoggedIn, autoRefreshOnSwitch) {
    loadFirstPage(forceRefresh = autoRefreshOnSwitch)
  }

  LaunchedEffect(manualRefreshKey) {
    if (manualRefreshKey > 0 && manualRefreshKey != feedState.handledManualRefreshKey) {
      feedState.handledManualRefreshKey = manualRefreshKey
      loadFirstPage(forceRefresh = true)
    }
  }

  fun loadNextPage() {
    val currentState = feedState.state as? UserFeedState.Success ?: return
    if (currentState.loadingMore || currentState.endReached) {
      return
    }

    val viewAtToLoad = feedState.nextViewAt
    val maxToLoad = feedState.nextMax
    feedState.state = currentState.copy(loadingMore = true, loadMoreError = "")
    coroutineScope.launch {
      feedState.state = try {
        val page = videoRepository.getHistoryPage(
          viewAt = viewAtToLoad,
          max = maxToLoad,
        )
        feedState.nextViewAt = page.nextViewAt
        feedState.nextMax = page.nextMax
        val latestState = feedState.state as? UserFeedState.Success ?: return@launch
        val mergedVideos = latestState.videos.appendUnique(nextVideos = page.videos)
        if (mergedVideos.isNotEmpty()) {
          feedState.hasLoadedContent = true
        }
        latestState.copy(
          videos = mergedVideos,
          loadingMore = false,
          endReached = !page.hasMore ||
            page.videos.isEmpty() ||
            mergedVideos.size == latestState.videos.size,
          loadMoreError = "",
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        val latestState = feedState.state as? UserFeedState.Success ?: return@launch
        latestState.copy(loadingMore = false, loadMoreError = error.message.orEmpty())
      }
    }
  }

  UserFeedContent(
    state = feedState.state,
    loadingMessage = stringResource(R.string.history_loading),
    emptyMessage = stringResource(R.string.history_empty),
    failedMessage = { message -> stringResource(R.string.history_failed_with_message, message) },
    cardMode = VideoCardMode.History,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = feedState.focusedVideoIndex,
    restoredFocusKey = feedState.focusedVideoKey,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = { index, video ->
      feedState.focusedVideoIndex = index
      feedState.focusedVideoKey = video.focusRestoreKey()
    },
    onRetry = {
      coroutineScope.launch {
        loadFirstPage(forceRefresh = true)
      }
    },
    onLoadMore = ::loadNextPage,
    onMoveLeftToNav = onMoveLeftToNav,
    onVideoSelected = onVideoSelected,
  )
}

@Composable
private fun UserFeedContent(
  state: UserFeedState,
  loadingMessage: String,
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
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  TvVideoGrid(
    videos = videos,
    cardMode = cardMode,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = restoredFocusIndex,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    onFocusedIndexChange = onFocusedIndexChange,
    onLoadMore = onLoadMore,
    onMoveLeftToNav = onMoveLeftToNav,
    onVideoSelected = onVideoSelected,
    keyFactory = { index, video -> video.feedKey(index) },
  )
}

private suspend fun LazyGridState.scrollItemIntoStablePosition(
  index: Int,
  totalItems: Int,
  fallbackItemHeightPx: Int,
  scrollInsetPx: Int,
  focusedRowTopPaddingPx: Int,
  focusScale: Float,
  smoothScroll: Boolean,
) {
  val layout = layoutInfo
  val columns = layout.estimatedColumnCount()
  val row = index / columns
  val lastRow = (totalItems - 1) / columns
  val rowStartIndex = row * columns
  val viewportHeight = layout.viewportEndOffset - layout.viewportStartOffset
  val itemHeightPx = layout.visibleItemsInfo.firstOrNull { item -> item.index == index }?.size?.height
    ?: layout.visibleItemsInfo.firstOrNull()?.size?.height
    ?: fallbackItemHeightPx
  val focusOverflowPx = ((itemHeightPx * (focusScale - 1f)) / 2f).roundToInt()
  val edgeInsetPx = scrollInsetPx + focusOverflowPx
  val focusedItem = layout.visibleItemsInfo.firstOrNull { item -> item.index == index }
  if (focusedItem != null) {
    val itemTop = focusedItem.offset.y
    val viewportTop = layout.viewportStartOffset
    val viewportBottom = layout.viewportEndOffset - edgeInsetPx
    val targetTop = (layout.viewportStartOffset + focusedRowTopPaddingPx.coerceAtLeast(edgeInsetPx))
      .coerceAtMost(viewportBottom - focusedItem.size.height)
      .coerceAtLeast(viewportTop + edgeInsetPx)
    val scrollDelta = itemTop - targetTop
    if (kotlin.math.abs(scrollDelta) <= BiliMotion.FocusScrollMinDeltaPx) {
      return
    }
    if (smoothScroll) {
      animateScrollBy(scrollDelta.toFloat())
    } else {
      scroll {
        scrollBy(scrollDelta.toFloat())
      }
    }
    return
  }
  val maxTop = (viewportHeight - itemHeightPx - edgeInsetPx).coerceAtLeast(edgeInsetPx)
  val desiredTop = when (row) {
    0 -> edgeInsetPx
    lastRow -> maxTop
    else -> {
      ((viewportHeight - itemHeightPx) / 2).coerceIn(edgeInsetPx, maxTop)
    }
  }

  if (smoothScroll) {
    animateScrollToItem(index = rowStartIndex, scrollOffset = -focusedRowTopPaddingPx)
  } else {
    scrollToItem(index = rowStartIndex, scrollOffset = -focusedRowTopPaddingPx)
  }
}

private fun LazyGridState.targetIndexForDirection(
  fromIndex: Int,
  totalItems: Int,
  direction: Key,
): Int? {
  val columns = layoutInfo.estimatedColumnCount()
  val currentRow = fromIndex / columns
  val currentColumn = fromIndex % columns
  val lastIndex = totalItems - 1
  val lastRow = lastIndex / columns

  return when (direction) {
    Key.DirectionUp -> {
      if (currentRow == 0) {
        null
      } else {
        ((currentRow - 1) * columns + currentColumn).coerceAtMost(lastIndex)
      }
    }
    Key.DirectionDown -> {
      if (currentRow >= lastRow) {
        null
      } else {
        ((currentRow + 1) * columns + currentColumn).coerceAtMost(lastIndex)
      }
    }
    Key.DirectionLeft -> {
      if (currentColumn == 0) null else fromIndex - 1
    }
    Key.DirectionRight -> {
      val nextIndex = fromIndex + 1
      if (nextIndex > lastIndex || nextIndex / columns != currentRow) null else nextIndex
    }
    else -> null
  }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo.estimatedColumnCount(): Int {
  return visibleItemsInfo
    .map(LazyGridItemInfo::columnAnchor)
    .distinct()
    .count()
    .coerceAtLeast(1)
}

private val LazyGridItemInfo.columnAnchor: Int
  get() = offset.x

private fun List<VideoSummary>.appendUnique(nextVideos: List<VideoSummary>): List<VideoSummary> {
  if (nextVideos.isEmpty()) {
    return this
  }
  val knownKeys = mapIndexedTo(mutableSetOf()) { index, video -> video.feedKey(index) }
  return this + nextVideos.filterIndexed { index, video -> knownKeys.add(video.feedKey(index)) }
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

private fun VideoSummary.feedKey(index: Int): String {
  return bvid.ifBlank {
    "cid-$cid-view-$viewAt-$index"
  }
}

private fun Int.shouldLoadMore(totalItems: Int, threshold: Int): Boolean {
  return totalItems - this <= threshold
}

private const val RestoreFocusRetryCount = 8

internal sealed interface UserFeedState {
  data object Loading : UserFeedState
  data object Empty : UserFeedState
  data class Failed(val message: String) : UserFeedState
  data class Success(
    val videos: List<VideoSummary>,
    val loadingMore: Boolean,
    val endReached: Boolean,
    val loadMoreError: String,
  ) : UserFeedState
}
