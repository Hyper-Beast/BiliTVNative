package com.kirin.bilitv.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.common.FeedStatusScreen
import com.kirin.bilitv.ui.common.VideoGridSkeleton
import com.kirin.bilitv.ui.glass.biliLiquidGlassSurface
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Stable
internal class RecommendFocusState {
  var focusedVideoIndex by mutableIntStateOf(0)
  var focusedVideoKey by mutableStateOf("")

  fun reset() {
    focusedVideoIndex = 0
    focusedVideoKey = ""
  }
}

@Composable
internal fun RecommendScreen(
  viewModel: RecommendViewModel,
  focusState: RecommendFocusState,
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
  val viewState by viewModel.viewState.collectAsStateWithLifecycle()
  val sections = viewState.sections
  val selectedSectionKey = viewState.selectedSectionKey.takeIf { key -> sections.any { section -> section.key == key } }
    ?: sections.first().key
  val activeSectionKey = viewState.activeSectionKey.takeIf { key -> sections.any { section -> section.key == key } }
    ?: selectedSectionKey
  val selectedSection = sections.firstOrNull { section -> section.key == selectedSectionKey } ?: sections.first()
  val activeSection = sections.firstOrNull { section -> section.key == activeSectionKey } ?: selectedSection
  val selectedSectionFocusRequester = remember { FocusRequester() }
  val state = viewState.sectionStates[activeSection.key] ?: RecommendState.Loading

  fun resetFocusAndSelectSection(section: HomeSection, forceRefresh: Boolean) {
    focusState.reset()
    viewModel.selectSection(section = section, forceRefresh = forceRefresh)
  }

  LaunchedEffect(enabledHomeSections) {
    viewModel.setEnabledHomeSections(enabledHomeSections)
  }

  LaunchedEffect(viewModel, manualRefreshKey) {
    if (viewModel.refreshForManualKey(manualRefreshKey)) {
      focusState.reset()
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
      isSectionLoaded = { section -> section.key in viewState.loadedSectionKeys },
      selectedSectionFocusRequester = selectedSectionFocusRequester,
      onMoveLeftToNav = onMoveLeftToNav,
      onSectionSelected = { section ->
        resetFocusAndSelectSection(section = section, forceRefresh = true)
      },
      onSectionFocused = { section ->
        viewModel.previewSection(section)
        val sectionLoaded = section.key in viewState.loadedSectionKeys
        val shouldLoad = autoRefreshOnSwitch || !sectionLoaded
        if (shouldLoad) {
          resetFocusAndSelectSection(
            section = section,
            forceRefresh = autoRefreshOnSwitch && sectionLoaded,
          )
        } else if (autoConfirmOnFocus) {
          focusState.reset()
          viewModel.activateSection(section)
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
            resetFocusAndSelectSection(section = activeSection, forceRefresh = true)
          },
        )
        is RecommendState.Success -> {
          val restoredFocusIndex = currentState.videos.resolveFocusIndex(
            focusKey = focusState.focusedVideoKey,
            fallbackIndex = focusState.focusedVideoIndex,
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
            onFocusedIndexChange = { index, video ->
              focusState.focusedVideoIndex = index
              focusState.focusedVideoKey = video.focusRestoreKey()
            },
            onLoadMore = {
              viewModel.loadNextPage(activeSection)
            },
            onRefresh = {
              focusState.reset()
              viewModel.refreshActiveSection()
            },
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
  val homeColors = LocalHomeColors.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val capsuleShape = RoundedCornerShape(BiliRadius.Pill)
  val liquidGlassEnabled = performancePolicy.cinematicVisualEffectsEnabled && performancePolicy.liquidGlassCardsEnabled
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.HomeSectionCapsuleHeight),
    contentAlignment = Alignment.Center,
  ) {
    val capsuleMaxWidth = maxWidth
    val capsuleMinWidth = capsuleMaxWidth * homeSectionCapsuleMinWidthFraction(sections.size)
    val capsuleArrangement = if (sections.size <= HomeSectionCapsuleSpreadMaxCount) {
      Arrangement.SpaceEvenly
    } else {
      Arrangement.spacedBy(BiliSizing.HomeSectionCapsuleItemSpacing)
    }
    Row(
      modifier = Modifier
        .align(Alignment.Center)
        .offset(y = -BiliSizing.HomeSectionCapsuleTopOffset)
        .widthIn(min = capsuleMinWidth, max = capsuleMaxWidth)
        .clip(capsuleShape)
        .biliLiquidGlassSurface(
          enabled = liquidGlassEnabled,
          shape = capsuleShape,
          surfaceColor = homeColors.glassSurface.copy(alpha = BiliFocus.HomeSectionCapsuleSurfaceAlpha),
          borderColor = homeColors.textPrimary.copy(alpha = BiliFocus.HomeSectionCapsuleBorderAlpha),
          borderWidth = BiliFocus.RestingBorderWidth,
        )
        .padding(
          horizontal = BiliSizing.HomeSectionCapsuleHorizontalPadding,
          vertical = BiliSizing.HomeSectionCapsuleVerticalPadding,
        ),
      horizontalArrangement = capsuleArrangement,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      sections.forEachIndexed { index, section ->
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
  val homeColors = LocalHomeColors.current
  val shape = RoundedCornerShape(BiliRadius.Pill)
  val targetBorderColor = if (focused) homeColors.accent else BiliColors.Transparent
  val targetTextColor = when {
    selected -> homeColors.accent
    focused -> homeColors.textPrimary
    else -> homeColors.textSecondary
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
      .background(
        if (focused) {
          homeColors.textPrimary.copy(alpha = BiliFocus.HomeSectionTabFocusedSurfaceAlpha)
        } else {
          BiliColors.Transparent
        },
      )
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
  onFocusedIndexChange: (Int, VideoSummary) -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: () -> Unit,
  onMoveLeftToNav: () -> Boolean,
  onVideoSelected: (VideoSummary) -> Unit,
) {
  AdaptiveVideoGrid(
    videos = videos,
    firstItemFocusRequester = firstItemFocusRequester,
    restoredFocusIndex = restoredFocusIndex,
    restoreFocusRequestKey = restoreFocusRequestKey,
    onRestoreFocusHandled = onRestoreFocusHandled,
    requestInitialFocus = requestInitialFocus,
    onInitialFocusRequested = onInitialFocusRequested,
    onFocusedIndexChange = onFocusedIndexChange,
    onLoadMore = onLoadMore,
    onRefresh = onRefresh,
    onMoveLeftToNav = onMoveLeftToNav,
    onMoveUpFromFirstRow = {
      runCatching {
        selectedSectionFocusRequester.requestFocus()
      }.isSuccess
    },
    topPadding = BiliSizing.HomeVideoGridTopPadding + BiliSizing.HomeVideoGridTopBleed,
    topBleed = BiliSizing.HomeVideoGridTopBleed,
    onVideoSelected = onVideoSelected,
  )
}

private const val HomeSectionCapsuleSpreadMaxCount = 6

private fun homeSectionCapsuleMinWidthFraction(sectionCount: Int): Float = when (sectionCount) {
  0, 1 -> 0.24f
  2 -> 0.34f
  3 -> 0.44f
  4 -> 0.54f
  5 -> 0.60f
  6 -> 0.66f
  else -> 0f
}

private fun Key.isConfirmKey(): Boolean {
  return this == Key.Enter || this == Key.NumPadEnter || this == Key.DirectionCenter
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
