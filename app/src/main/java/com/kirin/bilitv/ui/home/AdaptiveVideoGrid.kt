package com.kirin.bilitv.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.input.InputMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliSizing

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AdaptiveVideoGrid(
  videos: List<VideoSummary>,
  firstItemFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: (() -> Unit)? = null,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
  modifier: Modifier = Modifier,
  cardMode: VideoCardMode = VideoCardMode.Standard,
  requestInitialFocus: Boolean = false,
  onInitialFocusRequested: () -> Unit = {},
  onMoveUpFromFirstRow: () -> Boolean = { true },
  onBackKey: (() -> Boolean)? = null,
  horizontalPadding: Dp = BiliSizing.VideoGridHorizontalPadding,
  topPadding: Dp = BiliFocus.ScrollInset,
  topBleed: Dp = 0.dp,
  keyFactory: (Int, VideoSummary) -> Any = { _, video -> video.bvid },
) {
  when (LocalInteractionProfile.current.inputMode) {
    InputMode.Touch -> TouchVideoGrid(
      videos = videos,
      onFocusedIndexChange = onFocusedIndexChange,
      onLoadMore = onLoadMore,
      onRefresh = onRefresh,
      onVideoSelected = onVideoSelected,
      modifier = modifier,
      cardMode = cardMode,
      horizontalPadding = horizontalPadding,
      topPadding = topPadding,
      topBleed = topBleed,
      keyFactory = keyFactory,
    )

    InputMode.Remote -> TvVideoGrid(
      videos = videos,
      firstItemFocusRequester = firstItemFocusRequester,
      restoredFocusIndex = restoredFocusIndex,
      restoreFocusRequestKey = restoreFocusRequestKey,
      onRestoreFocusHandled = onRestoreFocusHandled,
      onFocusedIndexChange = onFocusedIndexChange,
      onLoadMore = onLoadMore,
      onMoveLeftToNav = onMoveLeftToNav,
      onVideoSelected = onVideoSelected,
      modifier = modifier,
      cardMode = cardMode,
      requestInitialFocus = requestInitialFocus,
      onInitialFocusRequested = onInitialFocusRequested,
      onMoveUpFromFirstRow = onMoveUpFromFirstRow,
      onBackKey = onBackKey,
      horizontalPadding = horizontalPadding,
      topPadding = topPadding,
      topBleed = topBleed,
      keyFactory = keyFactory,
    )
  }
}
