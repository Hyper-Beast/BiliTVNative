package com.kirin.bilitv.ui.home

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.common.VideoThumbnailPrefetcher
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliSizing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TvGridRestoreFocusRetryCount = 8

private val TvGridBringIntoViewSpec = object : BringIntoViewSpec {
  override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
    val childEnd = offset + size
    return when {
      offset < 0f && childEnd > containerSize -> 0f
      offset < 0f -> offset
      childEnd > containerSize -> childEnd - containerSize
      else -> 0f
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TvVideoGrid(
  videos: List<VideoSummary>,
  firstItemFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
  modifier: Modifier = Modifier,
  cardMode: VideoCardMode = VideoCardMode.Standard,
  requestInitialFocus: Boolean = false,
  onInitialFocusRequested: () -> Unit = {},
  onMoveUpFromFirstRow: () -> Boolean = { true },
  onBackKey: (() -> Boolean)? = null,
  horizontalPadding: Dp = BiliSizing.VideoGridHorizontalPadding,
  keyFactory: (Int, VideoSummary) -> Any = { _, video -> video.bvid },
) {
  val columns = BiliSizing.VideoGridColumns
  val rowCount = (videos.size + columns - 1) / columns
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val density = LocalDensity.current
  val focusScrollInsetPx = with(density) { BiliFocus.ScrollInset.roundToPx() }
  val focusedRowTopPaddingPx = with(density) { BiliFocus.FocusedRowTopPadding.roundToPx() }
  val videoCardFallbackHeightPx = with(density) { BiliSizing.VideoCardMinHeight.roundToPx() }
  val restoredItemFocusRequester = remember { FocusRequester() }
  val restoreTargetIndex = restoredFocusIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
  val itemFocusRequesters = remember(videos.size, firstItemFocusRequester, restoredItemFocusRequester, restoreTargetIndex) {
    List(videos.size) { index ->
      when (index) {
        0 -> firstItemFocusRequester
        restoreTargetIndex -> restoredItemFocusRequester
        else -> FocusRequester()
      }
    }
  }
  var focusScrollJob by remember { mutableStateOf<Job?>(null) }
  var focusedIndex by remember { mutableIntStateOf(-1) }
  var pendingVerticalFocusRow by remember { mutableIntStateOf(-1) }

  VideoThumbnailPrefetcher(
    videos = videos,
    focusedIndex = if (focusedIndex >= 0) focusedIndex else restoredFocusIndex,
  )

  suspend fun scrollRow(row: Int, smoothScroll: Boolean) {
    listState.scrollRowIntoStablePosition(
      row = row,
      totalRows = rowCount,
      fallbackItemHeightPx = videoCardFallbackHeightPx,
      scrollInsetPx = focusScrollInsetPx,
      focusedRowTopPaddingPx = focusedRowTopPaddingPx,
      focusScale = if (performancePolicy.motionEnabled) BiliFocus.CardScale else 1f,
      smoothScroll = smoothScroll,
    )
  }

  LaunchedEffect(restoreFocusRequestKey, restoredFocusIndex, videos.size) {
    if (restoreFocusRequestKey <= 0 || videos.isEmpty()) {
      return@LaunchedEffect
    }
    val targetIndex = restoredFocusIndex.coerceIn(0, videos.lastIndex)
    scrollRow(targetIndex / columns, smoothScroll = false)
    repeat(TvGridRestoreFocusRetryCount) {
      withFrameNanos { }
      val focused = runCatching {
        itemFocusRequesters[targetIndex].requestFocus()
      }.getOrDefault(false)
      if (focused) {
        onRestoreFocusHandled(restoreFocusRequestKey)
        return@LaunchedEffect
      }
    }
    onRestoreFocusHandled(restoreFocusRequestKey)
  }

  LaunchedEffect(videos.size, requestInitialFocus) {
    if (requestInitialFocus && videos.isNotEmpty()) {
      withFrameNanos { }
      runCatching {
        firstItemFocusRequester.requestFocus()
      }
      onInitialFocusRequested()
    }
  }

  fun focusItem(index: Int): Boolean {
    return runCatching {
      itemFocusRequesters[index].requestFocus()
    }.isSuccess
  }

  fun scrollRowIfNeeded(row: Int) {
    focusScrollJob?.cancel()
    focusScrollJob = coroutineScope.launch {
      withFrameNanos { }
      scrollRow(row, smoothScroll = performancePolicy.smoothScrollingEnabled)
    }
  }

  fun moveFocus(fromIndex: Int, direction: Key): Boolean {
    val currentRow = fromIndex / columns
    val currentColumn = fromIndex % columns
    val lastIndex = videos.lastIndex
    val lastRow = lastIndex / columns

    if (direction == Key.DirectionUp && currentRow == 0) {
      return onMoveUpFromFirstRow()
    }
    if (direction == Key.DirectionLeft && currentColumn == 0) {
      return onMoveLeftToNav()
    }

    val targetIndex = when (direction) {
      Key.DirectionUp -> ((currentRow - 1) * columns + currentColumn).coerceAtMost(lastIndex).takeIf { currentRow > 0 }
      Key.DirectionDown -> ((currentRow + 1) * columns + currentColumn).coerceAtMost(lastIndex).takeIf { currentRow < lastRow }
      Key.DirectionLeft -> (fromIndex - 1).takeIf { currentColumn > 0 }
      Key.DirectionRight -> (fromIndex + 1).takeIf { currentColumn < columns - 1 && it <= lastIndex && it / columns == currentRow }
      else -> null
    } ?: return direction == Key.DirectionDown || direction == Key.DirectionRight

    if (direction == Key.DirectionLeft || direction == Key.DirectionRight) {
      return focusItem(targetIndex)
    }

    pendingVerticalFocusRow = targetIndex / columns
    return focusItem(targetIndex)
  }

  CompositionLocalProvider(LocalBringIntoViewSpec provides TvGridBringIntoViewSpec) {
    LazyColumn(
      state = listState,
      modifier = modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = horizontalPadding,
        top = BiliFocus.ScrollInset,
        end = horizontalPadding,
        bottom = BiliSizing.VideoGridBottomPadding,
      ),
      verticalArrangement = Arrangement.spacedBy(BiliSizing.VideoGridSpacing),
    ) {
      items(
        count = rowCount,
        key = { row ->
          val firstIndex = row * columns
          "row-$row-${keyFactory(firstIndex, videos[firstIndex])}"
        },
        contentType = { "video-row" },
      ) { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BiliSizing.VideoGridSpacing),
        ) {
          repeat(columns) { column ->
            val index = row * columns + column
            if (index < videos.size) {
              val video = videos[index]
              VideoCard(
                video = video,
                mode = cardMode,
                modifier = Modifier
                  .weight(1f)
                  .focusRequester(itemFocusRequesters[index])
                  .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                      return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                      Key.Back -> onBackKey?.invoke() ?: false
                      Key.DirectionUp,
                      Key.DirectionDown,
                      Key.DirectionLeft,
                      Key.DirectionRight -> moveFocus(index, event.key)
                      else -> false
                    }
                  },
                onFocused = {
                  val focusChanged = focusedIndex != index
                  val pendingRow = pendingVerticalFocusRow
                  pendingVerticalFocusRow = -1
                  focusedIndex = index
                  onFocusedIndexChange(index, video)
                  if (index.shouldLoadMore(
                      totalItems = videos.size,
                      threshold = performancePolicy.loadMoreFocusThreshold,
                    )
                  ) {
                    onLoadMore()
                  }
                  if (focusChanged && pendingRow == row) {
                    scrollRowIfNeeded(row)
                  }
                },
                onClick = {
                  onFocusedIndexChange(index, video)
                  onVideoSelected(video)
                },
              )
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

private suspend fun LazyListState.scrollRowIntoStablePosition(
  row: Int,
  totalRows: Int,
  fallbackItemHeightPx: Int,
  scrollInsetPx: Int,
  focusedRowTopPaddingPx: Int,
  focusScale: Float,
  smoothScroll: Boolean,
) {
  val safeRow = row.coerceIn(0, (totalRows - 1).coerceAtLeast(0))
  val layout = layoutInfo
  val viewportTop = layout.viewportStartOffset
  val viewportBottom = layout.viewportEndOffset
  val itemHeightPx = layout.visibleItemsInfo.firstOrNull { item -> item.index == safeRow }?.size
    ?: layout.visibleItemsInfo.firstOrNull()?.size
    ?: fallbackItemHeightPx
  val focusOverflowPx = ((itemHeightPx * (focusScale - 1f)) / 2f).roundToInt()
  val edgeInsetPx = scrollInsetPx + focusOverflowPx
  val focusedRow = layout.visibleItemsInfo.firstOrNull { item -> item.index == safeRow }

  if (focusedRow != null) {
    val targetTop = (viewportTop + focusedRowTopPaddingPx.coerceAtLeast(edgeInsetPx))
      .coerceAtMost(viewportBottom - edgeInsetPx - focusedRow.size)
      .coerceAtLeast(viewportTop + edgeInsetPx)
    val scrollDelta = focusedRow.offset - targetTop
    if (abs(scrollDelta) <= BiliMotion.FocusScrollMinDeltaPx) {
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

  if (smoothScroll) {
    animateScrollToItem(safeRow, scrollOffset = -focusedRowTopPaddingPx)
  } else {
    scrollToItem(safeRow, scrollOffset = -focusedRowTopPaddingPx)
  }
}

private fun Int.shouldLoadMore(totalItems: Int, threshold: Int): Boolean {
  return this >= totalItems - threshold
}
