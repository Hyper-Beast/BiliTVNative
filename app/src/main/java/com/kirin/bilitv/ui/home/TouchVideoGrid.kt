package com.kirin.bilitv.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.common.VideoThumbnailPrefetcher
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.LocalHomeColors
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TouchLoadMoreRowThreshold = 2

@Composable
internal fun TouchVideoGrid(
  videos: List<VideoSummary>,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: (() -> Unit)?,
  onVideoSelected: (VideoSummary) -> Unit,
  modifier: Modifier = Modifier,
  cardMode: VideoCardMode = VideoCardMode.Standard,
  horizontalPadding: Dp = BiliSizing.VideoGridHorizontalPadding,
  topPadding: Dp = BiliFocus.ScrollInset,
  topBleed: Dp = 0.dp,
  keyFactory: (Int, VideoSummary) -> Any = { _, video -> video.bvid },
) {
  val listState = rememberLazyListState()
  val density = LocalDensity.current
  val onLoadMoreState = rememberUpdatedState(onLoadMore)
  val onRefreshState = rememberUpdatedState(onRefresh)
  val touchPullRefreshTriggerPx = with(density) { BiliSizing.TouchPullRefreshTriggerDistance.toPx() }
  val touchPullRefreshMaxPx = with(density) { BiliSizing.TouchPullRefreshMaxDistance.toPx() }
  val touchPullRefreshContentTravelPx = with(density) { BiliSizing.TouchPullRefreshContentTravel.toPx() }
  val topBleedPx = with(density) { topBleed.roundToPx() }
  var pullRefreshDistancePx by remember { mutableFloatStateOf(0f) }
  val pullRefreshProgress = if (touchPullRefreshTriggerPx <= 0f) {
    0f
  } else {
    (pullRefreshDistancePx / touchPullRefreshTriggerPx).coerceIn(0f, 1f)
  }
  val pullRefreshReady = pullRefreshDistancePx >= touchPullRefreshTriggerPx
  val pullRefreshIndicatorVisible = onRefresh != null && pullRefreshDistancePx > 0f

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns = touchVideoGridColumns(maxWidth)
    val rowCount = (videos.size + columns - 1) / columns
    val firstVisibleIndex = listState.firstVisibleItemIndex * columns

    VideoThumbnailPrefetcher(
      videos = videos,
      focusedIndex = firstVisibleIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0)),
      enabled = true,
    )

    LaunchedEffect(listState, videos.size, rowCount) {
      if (videos.isEmpty()) {
        return@LaunchedEffect
      }
      snapshotFlow {
        val lastVisibleRow = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { item -> item.index } ?: -1
        lastVisibleRow >= rowCount - TouchLoadMoreRowThreshold
      }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
          if (shouldLoadMore) {
            onLoadMoreState.value()
          }
        }
    }

    val pullRefreshConnection = remember(listState, touchPullRefreshTriggerPx, touchPullRefreshMaxPx) {
      object : NestedScrollConnection {
        private fun canPullRefresh(): Boolean {
          return onRefreshState.value != null &&
            listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0
        }

        override fun onPreScroll(
          available: Offset,
          source: NestedScrollSource,
        ): Offset {
          if (source != NestedScrollSource.UserInput || pullRefreshDistancePx <= 0f || available.y >= 0f) {
            return Offset.Zero
          }
          val consumedY = available.y.coerceAtLeast(-pullRefreshDistancePx)
          pullRefreshDistancePx = (pullRefreshDistancePx + consumedY).coerceAtLeast(0f)
          return Offset(x = 0f, y = consumedY)
        }

        override fun onPostScroll(
          consumed: Offset,
          available: Offset,
          source: NestedScrollSource,
        ): Offset {
          if (source != NestedScrollSource.UserInput || !canPullRefresh()) {
            return Offset.Zero
          }
          val dragDelta = available.y
          if (dragDelta <= 0f) {
            return Offset.Zero
          }
          pullRefreshDistancePx = (pullRefreshDistancePx + dragDelta).coerceAtMost(touchPullRefreshMaxPx)
          return Offset(x = 0f, y = dragDelta)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
          val shouldRefresh = pullRefreshDistancePx >= touchPullRefreshTriggerPx
          pullRefreshDistancePx = 0f
          if (shouldRefresh) {
            onRefreshState.value?.invoke()
          }
          return Velocity.Zero
        }
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            translationY = touchPullRefreshContentTravelPx * pullRefreshProgress
          }
          .nestedScroll(pullRefreshConnection)
          .layout { measurable, constraints ->
            if (topBleedPx <= 0) {
              val placeable = measurable.measure(constraints)
              layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
              }
            } else {
              val expandedMaxHeight = if (constraints.maxHeight == Constraints.Infinity) {
                Constraints.Infinity
              } else {
                constraints.maxHeight + topBleedPx
              }
              val placeable = measurable.measure(
                constraints.copy(maxHeight = expandedMaxHeight),
              )
              val layoutHeight = if (constraints.maxHeight == Constraints.Infinity) {
                placeable.height
              } else {
                constraints.maxHeight
              }
              layout(placeable.width, layoutHeight) {
                placeable.place(0, -topBleedPx)
              }
            }
          },
        contentPadding = PaddingValues(
          start = horizontalPadding,
          top = topPadding,
          end = horizontalPadding,
          bottom = BiliSizing.VideoGridBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(BiliSizing.VideoGridSpacing),
      ) {
        items(
          count = rowCount,
          key = { row ->
            val firstIndex = row * columns
            "touch-row-$row-${keyFactory(firstIndex, videos[firstIndex])}"
          },
          contentType = { "touch-video-row" },
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
                  modifier = Modifier.weight(1f),
                  onFocused = {
                    onFocusedIndexChange(index, video)
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
      TouchPullRefreshIndicator(
        visible = pullRefreshIndicatorVisible,
        ready = pullRefreshReady,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .zIndex(BiliFocus.FocusedZIndex),
      )
    }
  }
}

private fun touchVideoGridColumns(width: Dp): Int {
  return when {
    width < 720.dp -> 2
    width < 1040.dp -> 3
    else -> BiliSizing.VideoGridColumns
  }
}

@Composable
private fun TouchPullRefreshIndicator(
  visible: Boolean,
  ready: Boolean,
  modifier: Modifier = Modifier,
) {
  val homeColors = LocalHomeColors.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val targetVisibility = if (visible) 1f else 0f
  val visibility by if (performancePolicy.motionEnabled) {
    animateFloatAsState(
      targetValue = targetVisibility,
      animationSpec = tween(
        durationMillis = BiliMotion.FocusMs,
        easing = BiliMotion.FocusEasing,
      ),
      label = "pullRefreshVisibility",
    )
  } else {
    remember(targetVisibility) { mutableFloatStateOf(targetVisibility) }
  }
  val targetColor = if (ready) homeColors.accent else homeColors.textSecondary
  val indicatorColor by if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = targetColor,
      animationSpec = tween<Color>(
        durationMillis = BiliMotion.FocusMs,
        easing = BiliMotion.FocusEasing,
      ),
      label = "pullRefreshIndicatorColor",
    )
  } else {
    remember(targetColor) { mutableStateOf(targetColor) }
  }
  val spinnerRotation = if (visible || visibility > 0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "pullRefreshSpinner")
    val rotation by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        animation = tween(
          durationMillis = BiliMotion.TouchPullRefreshSpinnerRotationMs,
          easing = LinearEasing,
        ),
        repeatMode = RepeatMode.Restart,
      ),
      label = "pullRefreshSpinnerRotation",
    )
    rotation
  } else {
    0f
  }

  Box(
    modifier = modifier
      .padding(top = BiliSizing.TouchPullRefreshIndicatorTopPadding)
      .graphicsLayer {
        alpha = visibility
      }
      .size(BiliSizing.TouchPullRefreshSpinnerSize),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          rotationZ = spinnerRotation
        },
    ) {
      val strokeWidth = BiliFocus.BorderWidth.toPx()
      val strokeInset = strokeWidth / 2f
      drawArc(
        color = indicatorColor,
        startAngle = -90f,
        sweepAngle = BiliFocus.TouchPullRefreshSpinnerSweepDegrees,
        useCenter = false,
        topLeft = Offset(strokeInset, strokeInset),
        size = Size(
          width = size.width - strokeWidth,
          height = size.height - strokeWidth,
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )
    }
  }
}
