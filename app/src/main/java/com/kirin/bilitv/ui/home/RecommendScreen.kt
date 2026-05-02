package com.kirin.bilitv.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.common.VideoThumbnailPrefetcher
import com.kirin.bilitv.ui.common.VideoGridSkeleton
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
internal class RecommendUiState {
  var selectedSectionKey by mutableStateOf("")
  var activeSectionKey by mutableStateOf("")
  var sectionStates by mutableStateOf<Map<String, RecommendState>>(emptyMap())
  var loadedSectionKeys by mutableStateOf(emptySet<String>())
  var sectionRefreshKeys by mutableStateOf<Map<String, Int>>(emptyMap())
  var loadRequest by mutableStateOf<RecommendLoadRequest?>(null)
  var nextLoadRequestId by mutableIntStateOf(0)
  var handledManualRefreshKey by mutableIntStateOf(0)
  var focusedVideoIndex by mutableIntStateOf(0)
  var focusedVideoKey by mutableStateOf("")
}

@Composable
internal fun RecommendScreen(
  videoRepository: VideoRepository,
  uiState: RecommendUiState,
  firstItemFocusRequester: FocusRequester,
  enabledHomeSections: Set<HomeSection>,
  autoConfirmOnFocus: Boolean,
  autoRefreshOnSwitch: Boolean,
  manualRefreshKey: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  requestInitialFocus: Boolean,
  onInitialFocusRequested: () -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val sections = remember(enabledHomeSections) {
    HomeSection.DefaultOrder.filter { section -> section in enabledHomeSections }
      .ifEmpty { listOf(HomeSection.Recommend) }
  }
  val selectedSectionKey = uiState.selectedSectionKey.takeIf { key -> sections.any { section -> section.key == key } }
    ?: sections.first().key
  val activeSectionKey = uiState.activeSectionKey.takeIf { key -> sections.any { section -> section.key == key } }
    ?: selectedSectionKey
  val selectedSection = sections.firstOrNull { section -> section.key == selectedSectionKey } ?: sections.first()
  val activeSection = sections.firstOrNull { section -> section.key == activeSectionKey } ?: selectedSection
  val selectedSectionFocusRequester = remember { FocusRequester() }
  val state = uiState.sectionStates[activeSection.key] ?: RecommendState.Loading
  val activeRefreshKey = uiState.sectionRefreshKeys[activeSection.key] ?: 0

  fun requestSectionLoad(sectionKey: String, refreshKey: Int) {
    uiState.nextLoadRequestId += 1
    uiState.loadRequest = RecommendLoadRequest(
      id = uiState.nextLoadRequestId,
      sectionKey = sectionKey,
      refreshKey = refreshKey,
    )
  }

  LaunchedEffect(sections) {
    val sectionKeys = sections.mapTo(mutableSetOf()) { section -> section.key }
    uiState.loadedSectionKeys = uiState.loadedSectionKeys.filterTo(mutableSetOf()) { key -> key in sectionKeys }
    uiState.sectionStates = uiState.sectionStates.filterKeys { key -> key in sectionKeys }
    uiState.sectionRefreshKeys = uiState.sectionRefreshKeys.filterKeys { key -> key in sectionKeys }
    if (sections.none { section -> section.key == uiState.selectedSectionKey }) {
      uiState.selectedSectionKey = sections.first().key
    }
    if (sections.none { section -> section.key == uiState.activeSectionKey }) {
      uiState.activeSectionKey = sections.first().key
      uiState.focusedVideoIndex = 0
      uiState.focusedVideoKey = ""
    }
    if (uiState.loadRequest != null && sections.none { section -> section.key == uiState.loadRequest?.sectionKey }) {
      uiState.loadRequest = null
    }

    val sectionKeyToLoad = uiState.activeSectionKey
      .takeIf { key -> sections.any { section -> section.key == key } }
      ?: sections.first().key
    if (
      uiState.loadRequest == null &&
      uiState.sectionStates[sectionKeyToLoad] == null
    ) {
      requestSectionLoad(
        sectionKey = sectionKeyToLoad,
        refreshKey = uiState.sectionRefreshKeys[sectionKeyToLoad] ?: 0,
      )
    }
  }

  LaunchedEffect(videoRepository, uiState.loadRequest) {
    val request = uiState.loadRequest ?: return@LaunchedEffect
    val sectionToLoad = sections.firstOrNull { section -> section.key == request.sectionKey }
      ?: return@LaunchedEffect
    uiState.sectionStates = uiState.sectionStates + (sectionToLoad.key to RecommendState.Loading)
    uiState.focusedVideoIndex = 0
    uiState.focusedVideoKey = ""
    val nextState = try {
      val videos = videoRepository.getHomeSectionVideos(
        section = sectionToLoad,
        page = FirstPage,
        idx = if (sectionToLoad == HomeSection.Recommend) request.refreshKey else 0,
      )
      if (videos.isEmpty()) {
        RecommendState.Empty
      } else {
        RecommendState.Success(
          videos = videos,
          nextPage = FirstPage + 1,
          loadingMore = false,
          endReached = videos.size < PageSize,
          loadMoreError = "",
        )
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      RecommendState.Failed(error.message.orEmpty())
    }
    uiState.loadedSectionKeys = uiState.loadedSectionKeys + sectionToLoad.key
    uiState.sectionStates = uiState.sectionStates + (sectionToLoad.key to nextState)
    if (uiState.loadRequest?.id == request.id) {
      uiState.loadRequest = null
    }
  }

  LaunchedEffect(manualRefreshKey) {
    if (manualRefreshKey <= 0 || manualRefreshKey == uiState.handledManualRefreshKey) {
      return@LaunchedEffect
    }
    uiState.handledManualRefreshKey = manualRefreshKey
    val nextRefreshKey = (uiState.sectionRefreshKeys[activeSection.key] ?: 0) + 1
    uiState.sectionRefreshKeys = uiState.sectionRefreshKeys + (activeSection.key to nextRefreshKey)
    requestSectionLoad(sectionKey = activeSection.key, refreshKey = nextRefreshKey)
  }

  fun loadNextPage() {
    val currentState = uiState.sectionStates[activeSection.key] as? RecommendState.Success ?: return
    if (currentState.loadingMore || currentState.endReached) {
      return
    }

    val pageToLoad = currentState.nextPage
    val sectionToLoad = activeSection
    val sectionKeyToLoad = activeSection.key
    val refreshKeyToLoad = activeRefreshKey
    uiState.sectionStates = uiState.sectionStates + (
      sectionKeyToLoad to currentState.copy(
        loadingMore = true,
        loadMoreError = "",
      )
    )

    coroutineScope.launch {
      val nextState = try {
        val nextVideos = videoRepository.getHomeSectionVideos(
          section = sectionToLoad,
          page = pageToLoad,
          idx = if (sectionToLoad == HomeSection.Recommend) {
            refreshKeyToLoad + pageToLoad - FirstPage
          } else {
            0
          },
        )
        val latestState = uiState.sectionStates[sectionKeyToLoad] as? RecommendState.Success ?: return@launch
        val mergedVideos = latestState.videos.appendUniqueByBvid(nextVideos)
        latestState.copy(
          videos = mergedVideos,
          nextPage = pageToLoad + 1,
          loadingMore = false,
          endReached = nextVideos.size < PageSize ||
            mergedVideos.size == latestState.videos.size,
          loadMoreError = "",
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        val latestState = uiState.sectionStates[sectionKeyToLoad] as? RecommendState.Success ?: return@launch
        latestState.copy(
          loadingMore = false,
          loadMoreError = error.message.orEmpty(),
        )
      }
      uiState.sectionStates = uiState.sectionStates + (sectionKeyToLoad to nextState)
    }
  }

  fun selectSection(section: HomeSection, forceRefresh: Boolean) {
    uiState.selectedSectionKey = section.key
    uiState.activeSectionKey = section.key
    uiState.focusedVideoIndex = 0
    uiState.focusedVideoKey = ""
    val hasLoadedSection = section.key in uiState.loadedSectionKeys
    if (forceRefresh || !hasLoadedSection) {
      val nextRefreshKey = if (forceRefresh) {
        (uiState.sectionRefreshKeys[section.key] ?: 0) + 1
      } else {
        uiState.sectionRefreshKeys[section.key] ?: 0
      }
      uiState.sectionRefreshKeys = uiState.sectionRefreshKeys + (section.key to nextRefreshKey)
      requestSectionLoad(sectionKey = section.key, refreshKey = nextRefreshKey)
    }
  }

  Column(
    modifier = Modifier.fillMaxSize(),
  ) {
    RecommendHeader(
      sections = sections,
      selectedSection = selectedSection,
      autoConfirmOnFocus = autoConfirmOnFocus,
      shouldAutoRefreshOnFocus = autoRefreshOnSwitch,
      isSectionLoaded = { section -> section.key in uiState.loadedSectionKeys },
      selectedSectionFocusRequester = selectedSectionFocusRequester,
      onMoveLeftToNav = onMoveLeftToNav,
      onSectionSelected = { section ->
        selectSection(section = section, forceRefresh = true)
      },
      onSectionFocused = { section ->
        uiState.selectedSectionKey = section.key
        val shouldLoad = autoRefreshOnSwitch || section.key !in uiState.loadedSectionKeys
        if (shouldLoad) {
          selectSection(section = section, forceRefresh = autoRefreshOnSwitch && section.key in uiState.loadedSectionKeys)
        } else if (autoConfirmOnFocus) {
          uiState.activeSectionKey = section.key
          uiState.focusedVideoIndex = 0
          uiState.focusedVideoKey = ""
        }
      },
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = BiliSpacing.Xs),
    ) {
      when (val currentState = state) {
        RecommendState.Loading -> VideoGridSkeleton()
        RecommendState.Empty -> FeedStatusScreen(message = stringResource(R.string.recommend_empty))
        is RecommendState.Failed -> FeedStatusScreen(
          message = stringResource(R.string.recommend_failed_with_message, currentState.message),
          actionLabel = stringResource(R.string.action_retry),
          onAction = {
            selectSection(section = activeSection, forceRefresh = true)
          },
        )
        is RecommendState.Success -> {
          val restoredFocusIndex = currentState.videos.resolveFocusIndex(
            focusKey = uiState.focusedVideoKey,
            fallbackIndex = uiState.focusedVideoIndex,
          )
          RecommendGrid(
            videos = currentState.videos,
            firstItemFocusRequester = firstItemFocusRequester,
            selectedSectionFocusRequester = selectedSectionFocusRequester,
            restoredFocusIndex = restoredFocusIndex,
            restoreFocusRequestKey = restoreFocusRequestKey,
            onRestoreFocusHandled = onRestoreFocusHandled,
            requestInitialFocus = requestInitialFocus,
            onInitialFocusRequested = onInitialFocusRequested,
            onFocusedIndexChange = { index ->
              uiState.focusedVideoIndex = index
              uiState.focusedVideoKey = currentState.videos.getOrNull(index)?.focusRestoreKey().orEmpty()
            },
            onLoadMore = ::loadNextPage,
            onMoveLeftToNav = onMoveLeftToNav,
            onVideoSelected = onVideoSelected,
          )
        }
      }
    }
  }
}

@Composable
private fun RecommendHeader(
  sections: List<HomeSection>,
  selectedSection: HomeSection,
  autoConfirmOnFocus: Boolean,
  shouldAutoRefreshOnFocus: Boolean,
  isSectionLoaded: (HomeSection) -> Boolean,
  selectedSectionFocusRequester: FocusRequester,
  onMoveLeftToNav: () -> Boolean,
  onSectionSelected: (HomeSection) -> Unit,
  onSectionFocused: (HomeSection) -> Unit,
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(end = BiliSizing.HomeSectionClockReservedWidth)
      .height(BiliSizing.HomeSectionTabHeight + BiliSpacing.Xs)
      .padding(BiliSpacing.Xs),
    horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Lg),
    contentPadding = PaddingValues(horizontal = BiliSpacing.Xs),
  ) {
    itemsIndexed(sections, key = { _, section -> section.key }) { index, section ->
      HomeSectionTab(
        section = section,
        selected = section == selectedSection,
        autoConfirmOnFocus = autoConfirmOnFocus || !isSectionLoaded(section),
        modifier = if (section == selectedSection) {
          Modifier.focusRequester(selectedSectionFocusRequester)
        } else {
          Modifier
        },
        onMoveLeftToNav = if (index == 0) onMoveLeftToNav else null,
        onClick = {
          onSectionSelected(section)
        },
        onFocused = {
          if (autoConfirmOnFocus || shouldAutoRefreshOnFocus || !isSectionLoaded(section)) {
            onSectionFocused(section)
          }
        },
      )
    }
  }
}

@Composable
private fun HomeSectionTab(
  section: HomeSection,
  selected: Boolean,
  autoConfirmOnFocus: Boolean,
  modifier: Modifier = Modifier,
  onMoveLeftToNav: (() -> Boolean)?,
  onClick: () -> Unit,
  onFocused: () -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val performancePolicy = LocalBiliPerformancePolicy.current
  val shape = RoundedCornerShape(BiliRadius.Pill)
  val targetBorderColor = if (focused) BiliColors.BiliPink else BiliColors.Transparent
  val targetTextColor = when {
    selected -> BiliColors.BiliPink
    focused -> BiliColors.TextPrimary
    else -> BiliColors.TextSecondary
  }
  val borderWidth = if (performancePolicy.motionEnabled) {
    animateDpAsState(
      targetValue = if (focused) BiliFocus.BorderWidth else BiliFocus.RestingBorderWidth,
      animationSpec = androidx.compose.animation.core.tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "homeSectionBorderWidth",
    ).value
  } else {
    if (focused) BiliFocus.BorderWidth else BiliFocus.RestingBorderWidth
  }
  val borderColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = targetBorderColor,
      animationSpec = androidx.compose.animation.core.tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "homeSectionBorder",
    ).value
  } else {
    targetBorderColor
  }
  val textColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = targetTextColor,
      animationSpec = androidx.compose.animation.core.tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "homeSectionText",
    ).value
  } else {
    targetTextColor
  }
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .height(BiliSizing.HomeSectionTabHeight)
      .widthIn(min = BiliSizing.HomeSectionTabCompactMinWidth)
      .clip(shape)
      .border(BorderStroke(borderWidth, borderColor), shape)
      .onFocusChanged { focusState ->
        focused = focusState.isFocused
        if (focusState.isFocused && autoConfirmOnFocus && !selected) {
          onFocused()
        }
      }
      .onPreviewKeyEvent { event ->
        when {
          event.type == KeyEventType.KeyDown &&
            event.key == Key.DirectionLeft &&
            onMoveLeftToNav != null -> onMoveLeftToNav()
          event.type == KeyEventType.KeyUp && event.key.isConfirmKey() -> {
            onClick()
            true
          }
          else -> false
        }
      }
      .focusable(interactionSource = interactionSource)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .padding(horizontal = BiliSpacing.Sm),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(section.titleRes()),
      color = textColor,
      fontSize = BiliTypography.HomeSectionTab,
      lineHeight = BiliTypography.HomeSectionTabLineHeight,
      fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
      textAlign = TextAlign.Center,
      maxLines = 1,
      style = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
      ),
    )
  }
}

@Composable
private fun RecommendGrid(
  videos: List<VideoSummary>,
  firstItemFocusRequester: FocusRequester,
  selectedSectionFocusRequester: FocusRequester,
  restoredFocusIndex: Int,
  restoreFocusRequestKey: Int,
  onRestoreFocusHandled: (Int) -> Unit,
  requestInitialFocus: Boolean,
  onInitialFocusRequested: () -> Unit,
  onFocusedIndexChange: (Int) -> Unit,
  onLoadMore: () -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  TvVideoGrid(
    videos = videos,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = restoredFocusIndex,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    requestInitialFocus = requestInitialFocus,
    onInitialFocusRequested = onInitialFocusRequested,
    onFocusedIndexChange = { index, _ -> onFocusedIndexChange(index) },
    onLoadMore = onLoadMore,
    onMoveLeftToNav = onMoveLeftToNav,
    onMoveUpFromFirstRow = {
      runCatching {
        selectedSectionFocusRequester.requestFocus()
      }.isSuccess
    },
    onVideoSelected = onVideoSelected,
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
    animateScrollToItem(
      index = rowStartIndex,
      scrollOffset = -focusedRowTopPaddingPx,
    )
  } else {
    scrollToItem(
      index = rowStartIndex,
      scrollOffset = -focusedRowTopPaddingPx,
    )
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

private fun Key.isConfirmKey(): Boolean {
  return this == Key.Enter || this == Key.NumPadEnter || this == Key.DirectionCenter
}

private fun List<VideoSummary>.appendUniqueByBvid(nextVideos: List<VideoSummary>): List<VideoSummary> {
  if (nextVideos.isEmpty()) {
    return this
  }
  val knownBvids = mapTo(mutableSetOf()) { video -> video.bvid }
  return this + nextVideos.filter { video -> knownBvids.add(video.bvid) }
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
      else -> ""
    }
  }
}

private fun Int.shouldLoadMore(totalItems: Int, threshold: Int): Boolean {
  return totalItems - this <= threshold
}

internal data class RecommendLoadRequest(
  val id: Int,
  val sectionKey: String,
  val refreshKey: Int,
)

@Composable
private fun InitialHomeCardFocusEffect(
  firstItemFocusRequester: FocusRequester,
  onInitialFocusRequested: () -> Unit,
) {
  LaunchedEffect(firstItemFocusRequester) {
    repeat(InitialFocusRetryCount) {
      withFrameNanos { }
      val focused = runCatching {
        firstItemFocusRequester.requestFocus()
      }.getOrDefault(false)
      if (focused) {
        onInitialFocusRequested()
        return@LaunchedEffect
      }
      delay(InitialFocusRetryDelayMs)
    }
  }
}

private const val InitialFocusRetryCount = 20
private const val InitialFocusRetryDelayMs = 50L
private const val RestoreFocusRetryCount = 8
private const val FirstPage = 1
private const val PageSize = 20

internal sealed interface RecommendState {
  data object Loading : RecommendState
  data object Empty : RecommendState
  data class Success(
    val videos: List<VideoSummary>,
    val nextPage: Int,
    val loadingMore: Boolean,
    val endReached: Boolean,
    val loadMoreError: String,
  ) : RecommendState
  data class Failed(val message: String) : RecommendState
}
