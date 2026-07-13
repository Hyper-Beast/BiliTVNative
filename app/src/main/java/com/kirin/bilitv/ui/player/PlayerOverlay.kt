package com.kirin.bilitv.ui.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.kirin.bilitv.R
import com.kirin.bilitv.core.image.buildOwnerAvatarRequest
import com.kirin.bilitv.core.image.buildVideoThumbnailRequest
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.player.AirJumpSegment
import com.kirin.bilitv.core.player.DanmakuSettings
import com.kirin.bilitv.core.player.PlaybackInfo
import com.kirin.bilitv.core.player.PlaybackEpisode
import com.kirin.bilitv.core.player.PlaybackQuality
import com.kirin.bilitv.core.player.PlaybackRequest
import com.kirin.bilitv.core.player.PlaybackVideoMetadata
import com.kirin.bilitv.core.player.PlayerComment
import com.kirin.bilitv.core.player.VideoshotData
import com.kirin.bilitv.core.player.VideoshotFrame
import com.kirin.bilitv.ui.common.ClockOverlay
import com.kirin.bilitv.ui.glass.biliLiquidGlassSurface
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class PlayerControl {
  Episodes,
  Up,
  Related,
  Settings,
}

internal enum class PlayerPanel {
  None,
  Main,
  Quality,
  Danmaku,
  Speed,
  Episodes,
  UpVideos,
  RelatedVideos,
  Comments,
}

private enum class PlayerTouchAction {
  Episodes,
  Up,
  Related,
  Comments,
  Settings,
}

@Composable
internal fun BoxScope.PlayerTvChrome(
  request: PlaybackRequest,
  info: PlaybackInfo,
  onlineCountText: String,
  currentCodecText: String,
  controlsVisible: Boolean,
  focusedControl: PlayerControl,
  progressFocused: Boolean,
  danmakuSettings: DanmakuSettings,
  positionState: State<Long>,
  durationState: State<Long>,
  bufferedPercentageState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  previewPositionMs: Long?,
  showClock: Boolean,
  clockText: String,
  showMiniProgressBar: Boolean,
) {
  if (controlsVisible) {
    PlayerTopOverlay(
      request = request,
      title = info.title,
      showClock = showClock,
      clockText = clockText,
      modifier = Modifier.align(Alignment.TopCenter),
    )
    PlayerBottomOverlay(
      request = request,
      info = info,
      focusedControl = focusedControl,
      progressFocused = progressFocused,
      positionState = positionState,
      durationState = durationState,
      bufferedPercentageState = bufferedPercentageState,
      airJumpSegments = airJumpSegments,
      previewPositionMs = previewPositionMs,
      danmakuSettings = danmakuSettings,
      onlineCountText = onlineCountText,
      currentCodecText = currentCodecText,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  } else {
    PlayerPassiveStatusChrome(
      positionState = positionState,
      durationState = durationState,
      airJumpSegments = airJumpSegments,
      showClock = showClock,
      clockText = clockText,
      showMiniProgressBar = showMiniProgressBar,
    )
  }
}

@Composable
internal fun BoxScope.PlayerPassiveStatusChrome(
  positionState: State<Long>,
  durationState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  showClock: Boolean,
  clockText: String,
  showMiniProgressBar: Boolean,
) {
  if (showClock) {
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
  if (showMiniProgressBar) {
    MiniProgressBar(
      positionState = positionState,
      durationState = durationState,
      airJumpSegments = airJumpSegments,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }
}

@Composable
internal fun BoxScope.PlayerSharedOverlay(
  request: PlaybackRequest,
  info: PlaybackInfo,
  metadata: PlaybackVideoMetadata?,
  sidePanelVideos: List<VideoSummary>,
  sidePanelLoading: Boolean,
  comments: List<PlayerComment>,
  commentsLoading: Boolean,
  upVideoOrder: String,
  upFollowed: Boolean,
  upFollowLoading: Boolean,
  playbackPaused: Boolean,
  seekPreviewSpritesEnabled: Boolean,
  videoshotData: VideoshotData?,
  videoshotSprites: Map<String, ImageBitmap>,
  currentCodecText: String,
  showUnfollowConfirm: Boolean,
  unfollowConfirmFocusedConfirm: Boolean,
  activePanel: PlayerPanel,
  focusedPanelIndex: Int,
  playbackSpeed: Float,
  danmakuSettings: DanmakuSettings,
  durationState: State<Long>,
  previewPositionMs: Long?,
  previewDeltaMs: Long? = null,
  onDismissPanel: (() -> Unit)? = null,
  onSettingsRowClick: ((Int) -> Unit)? = null,
  onSettingsRowAdjust: ((Int, Int) -> Unit)? = null,
  onEpisodeClick: ((Int) -> Unit)? = null,
  onVideoClick: ((Int) -> Unit)? = null,
  onUpSortClick: (() -> Unit)? = null,
  onUpFollowClick: (() -> Unit)? = null,
  onUnfollowCancel: (() -> Unit)? = null,
  onUnfollowConfirm: (() -> Unit)? = null,
) {
  if (previewPositionMs != null) {
    SeekPreviewOverlay(
      previewPositionMs = previewPositionMs,
      durationMs = durationState.value,
      deltaMs = previewDeltaMs,
      videoshotData = if (seekPreviewSpritesEnabled) videoshotData else null,
      videoshotSprites = videoshotSprites,
      modifier = Modifier.align(Alignment.Center),
    )
  } else if (playbackPaused) {
    PauseIndicatorOverlay(modifier = Modifier.align(Alignment.Center))
  }

  if (activePanel != PlayerPanel.None) {
    if (onDismissPanel != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismissPanel,
          ),
      )
    }
    when (activePanel) {
      PlayerPanel.Main,
      PlayerPanel.Quality,
      PlayerPanel.Danmaku,
      PlayerPanel.Speed -> PlayerSettingsPanel(
        activePanel = activePanel,
        focusedIndex = focusedPanelIndex,
        info = info,
        currentCodecText = currentCodecText,
        playbackSpeed = playbackSpeed,
        danmakuSettings = danmakuSettings,
        onRowClick = onSettingsRowClick,
        onRowAdjust = onSettingsRowAdjust,
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.Episodes -> PlayerEpisodesPanel(
        episodes = metadata?.pages.orEmpty(),
        currentCid = request.cid,
        focusedIndex = focusedPanelIndex,
        onEpisodeClick = onEpisodeClick,
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.UpVideos -> PlayerVideoListPanel(
        titleRes = R.string.player_panel_up_videos,
        request = request,
        videos = sidePanelVideos,
        loading = sidePanelLoading,
        focusedIndex = focusedPanelIndex,
        showUploaderHeader = true,
        upVideoOrder = upVideoOrder,
        upFollowed = upFollowed,
        upFollowLoading = upFollowLoading,
        onVideoClick = onVideoClick,
        onSortClick = onUpSortClick,
        onFollowClick = onUpFollowClick,
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.RelatedVideos -> PlayerVideoListPanel(
        titleRes = R.string.player_panel_related_videos,
        request = request,
        videos = sidePanelVideos,
        loading = sidePanelLoading,
        focusedIndex = focusedPanelIndex,
        showUploaderHeader = false,
        upVideoOrder = upVideoOrder,
        upFollowed = upFollowed,
        upFollowLoading = upFollowLoading,
        onVideoClick = onVideoClick,
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.Comments -> PlayerCommentsPanel(
        comments = comments,
        loading = commentsLoading,
        focusedIndex = focusedPanelIndex,
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.None -> Unit
    }
  }

  if (showUnfollowConfirm) {
    UnfollowConfirmDialog(
      focusedConfirm = unfollowConfirmFocusedConfirm,
      onCancel = onUnfollowCancel,
      onConfirm = onUnfollowConfirm,
      modifier = Modifier.align(Alignment.Center),
    )
  }
}

@Composable
internal fun BoxScope.PlayerTouchOverlay(
  request: PlaybackRequest,
  info: PlaybackInfo,
  controlsVisible: Boolean,
  activePanel: PlayerPanel,
  positionState: State<Long>,
  durationState: State<Long>,
  bufferedPercentageState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  previewPositionMs: Long?,
  showClock: Boolean,
  clockText: String,
  onBack: () -> Unit,
  onSeekStart: () -> Unit,
  onSeekPreview: (Long) -> Unit,
  onSeekCommit: (Long) -> Unit,
  onSeekCancel: () -> Unit,
  onPanelSelected: (PlayerPanel) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!controlsVisible || activePanel != PlayerPanel.None) return

  Box(modifier = modifier) {
    PlayerTouchTopOverlay(
      request = request,
      title = info.title,
      showClock = showClock,
      clockText = clockText,
      onBack = onBack,
      modifier = Modifier.align(Alignment.TopCenter),
    )
    PlayerTouchBottomOverlay(
      activePanel = activePanel,
      positionState = positionState,
      durationState = durationState,
      bufferedPercentageState = bufferedPercentageState,
      airJumpSegments = airJumpSegments,
      previewPositionMs = previewPositionMs,
      onSeekStart = onSeekStart,
      onSeekPreview = onSeekPreview,
      onSeekCommit = onSeekCommit,
      onSeekCancel = onSeekCancel,
      onPanelSelected = onPanelSelected,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }
}

@Composable
private fun PlayerTouchTopOverlay(
  request: PlaybackRequest,
  title: String,
  showClock: Boolean,
  clockText: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayTitle = convertChineseText(title.ifBlank { request.title })
  val ownerName = convertChineseText(request.ownerName)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerTouchTopGradientHeight)
      .background(
        Brush.verticalGradient(
          colors = listOf(BiliColors.OverlayStrong, BiliColors.OverlayTransparent),
        ),
      ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = BiliSizing.PlayerTouchOverlayHorizontalPadding,
          top = BiliSizing.PlayerTouchTopPadding,
          end = BiliSizing.PlayerTouchOverlayHorizontalPadding,
        ),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
    ) {
      PlayerTouchIconButton(
        iconRes = R.drawable.ic_player_chevron_left,
        contentDescription = stringResource(R.string.player_control_back),
        active = false,
        buttonSize = BiliSizing.PlayerTouchBackButtonSize,
        iconSize = BiliSizing.PlayerTouchBackIconSize,
        onClick = onBack,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
      ) {
        Text(
          text = displayTitle,
          color = BiliColors.TextPrimary,
          fontSize = BiliTypography.PlayerTitle,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (ownerName.isNotBlank()) {
            PlayerMetaItem(
              iconRes = R.drawable.ic_nav_account,
              text = ownerName,
              modifier = Modifier.weight(1f, fill = false),
            )
          }
          request.formatPubdate()?.let { pubdate ->
            PlayerMetaItem(
              iconRes = R.drawable.ic_player_calendar,
              text = stringResource(R.string.player_meta_pubdate, pubdate),
            )
          }
          if (request.viewCount > 0) {
            PlayerMetaItem(
              iconRes = R.drawable.ic_video_play_count,
              text = stringResource(R.string.player_meta_view_count, request.viewCount.formatCompactCountText()),
            )
          }
        }
      }
      if (showClock) {
        ClockOverlay(clockText = clockText)
      }
    }
  }
}

@Composable
private fun PlayerTouchBottomOverlay(
  activePanel: PlayerPanel,
  positionState: State<Long>,
  durationState: State<Long>,
  bufferedPercentageState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  previewPositionMs: Long?,
  onSeekStart: () -> Unit,
  onSeekPreview: (Long) -> Unit,
  onSeekCommit: (Long) -> Unit,
  onSeekCancel: () -> Unit,
  onPanelSelected: (PlayerPanel) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerBottomGradientHeight)
      .background(
        Brush.verticalGradient(
          colors = listOf(BiliColors.OverlayTransparent, BiliColors.OverlayStrong),
        ),
      )
      .padding(
        start = BiliSizing.PlayerTouchBottomOverlayHorizontalPadding,
        end = BiliSizing.PlayerTouchBottomOverlayHorizontalPadding,
        bottom = BiliSizing.PlayerTouchControlBarBottomPadding,
      ),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val shape = RoundedCornerShape(BiliRadius.Panel)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(BiliSizing.PlayerTouchControlBarHeight)
        .playerTouchChromeSurface(shape = shape, active = false)
        .padding(horizontal = BiliSizing.PlayerTouchControlBarHorizontalPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
    ) {
      PlayerTouchSlider(
        positionState = positionState,
        durationState = durationState,
        bufferedPercentageState = bufferedPercentageState,
        airJumpSegments = airJumpSegments,
        previewPositionMs = previewPositionMs,
        onSeekStart = onSeekStart,
        onSeekPreview = onSeekPreview,
        onSeekCommit = onSeekCommit,
        onSeekCancel = onSeekCancel,
        modifier = Modifier.weight(1f),
      )
      PlayerTouchTimeText(
        positionState = positionState,
        durationState = durationState,
        previewPositionMs = previewPositionMs,
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PlayerTouchAction.entries.forEach { action ->
          PlayerTouchIconButton(
            iconRes = action.iconRes,
            contentDescription = stringResource(action.labelRes),
            active = action.isActive(activePanel),
            onClick = { onPanelSelected(action.panel) },
          )
        }
      }
    }
  }
}

@Composable
private fun PlayerTouchSlider(
  positionState: State<Long>,
  durationState: State<Long>,
  bufferedPercentageState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  previewPositionMs: Long?,
  onSeekStart: () -> Unit,
  onSeekPreview: (Long) -> Unit,
  onSeekCommit: (Long) -> Unit,
  onSeekCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var dragging by remember { mutableStateOf(false) }
  var dragTargetMs by remember { mutableLongStateOf(0L) }
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerTouchSliderHeight)
      .pointerInput(durationState.value) {
        fun targetAt(x: Float): Long {
          val durationMs = durationState.value
          if (durationMs <= 0L || size.width <= 0) return 0L
          return ((x.coerceIn(0f, size.width.toFloat()) / size.width.toFloat()) * durationMs).toLong()
            .coerceIn(0L, durationMs)
        }

        detectDragGestures(
          onDragStart = { offset ->
            val durationMs = durationState.value
            if (durationMs <= 0L) return@detectDragGestures
            dragging = true
            dragTargetMs = targetAt(offset.x)
            onSeekStart()
            onSeekPreview(dragTargetMs)
          },
          onDrag = { change, _ ->
            if (!dragging) return@detectDragGestures
            change.consume()
            dragTargetMs = targetAt(change.position.x)
            onSeekPreview(dragTargetMs)
          },
          onDragEnd = {
            if (dragging) {
              onSeekCommit(dragTargetMs)
            }
            dragging = false
          },
          onDragCancel = {
            if (dragging) {
              onSeekCancel()
            }
            dragging = false
          },
        )
      },
  ) {
    val durationMs = durationState.value
    val buffered = (bufferedPercentageState.value / 100f).coerceIn(0f, 1f)
    val displayedPositionMs = when {
      dragging -> dragTargetMs
      previewPositionMs != null -> previewPositionMs
      else -> positionState.value
    }
    val progress = progressFraction(displayedPositionMs, durationMs)
    val barHeight = BiliSizing.PlayerTouchSliderTrackHeight.toPx()
    val centerY = size.height / 2f
    val radius = barHeight / 2f
    val thumbSize = if (dragging) {
      BiliSizing.PlayerTouchSliderExpandedThumbSize.toPx()
    } else {
      BiliSizing.PlayerTouchSliderThumbSize.toPx()
    }
    val knobCenterX = (size.width * progress).coerceIn(thumbSize / 2f, size.width - thumbSize / 2f)

    drawRoundBar(1f, centerY, barHeight, radius, BiliColors.TextPrimary.copy(alpha = BiliFocus.PlayerTouchSliderTrackAlpha))
    drawRoundBar(buffered, centerY, barHeight, radius, BiliColors.TextPrimary.copy(alpha = BiliFocus.PlayerTouchSliderBufferedAlpha))
    drawRoundBar(progress, centerY, barHeight, radius, BiliColors.BiliPink)
    drawAirJumpSegments(airJumpSegments, durationMs, centerY, barHeight, radius)
    drawCircle(
      color = BiliColors.TextPrimary.copy(alpha = BiliFocus.PlayerTouchSliderThumbSurfaceAlpha),
      radius = thumbSize / 2f,
      center = Offset(knobCenterX, centerY),
    )
    drawCircle(
      color = BiliColors.BiliPink,
      radius = thumbSize * 0.28f,
      center = Offset(knobCenterX, centerY),
    )
  }
}

@Composable
private fun PlayerTouchTimeText(
  positionState: State<Long>,
  durationState: State<Long>,
  previewPositionMs: Long?,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = (previewPositionMs ?: positionState.value).toPlayerTime(),
      color = BiliColors.BiliPink,
      fontSize = BiliTypography.PlayerTime,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
    Text(
      text = " / ${durationState.value.toPlayerTime()}",
      color = BiliColors.TextTertiary,
      fontSize = BiliTypography.PlayerTime,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun PlayerTouchIconButton(
  @DrawableRes iconRes: Int,
  contentDescription: String,
  active: Boolean,
  onClick: () -> Unit,
  buttonSize: androidx.compose.ui.unit.Dp = BiliSizing.PlayerTouchActionButtonSize,
  iconSize: androidx.compose.ui.unit.Dp = BiliSizing.PlayerTouchActionIconSize,
) {
  val shape = CircleShape
  Box(
    modifier = Modifier
      .size(buttonSize)
      .playerTouchChromeSurface(shape = shape, active = active)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = if (active) BiliColors.BiliPink else BiliColors.TextPrimary,
      modifier = Modifier.size(iconSize),
    )
  }
}

@Composable
private fun Modifier.playerTouchChromeSurface(
  shape: Shape,
  active: Boolean,
): Modifier {
  val performancePolicy = LocalBiliPerformancePolicy.current
  val refined = performancePolicy.cinematicVisualEffectsEnabled
  val surfaceAlpha = when {
    performancePolicy.lowSpecMode -> BiliFocus.PlayerTouchChromeSmoothSurfaceAlpha
    refined -> BiliFocus.PlayerTouchChromeRefinedSurfaceAlpha
    else -> BiliFocus.PlayerTouchChromeBalancedSurfaceAlpha
  }
  val borderAlpha = when {
    active -> BiliFocus.PlayerTouchChromeRefinedBorderAlpha
    performancePolicy.lowSpecMode -> BiliFocus.PlayerTouchChromeSmoothBorderAlpha
    refined -> BiliFocus.PlayerTouchChromeRefinedBorderAlpha
    else -> BiliFocus.PlayerTouchChromeBalancedBorderAlpha
  }
  return clip(shape).biliLiquidGlassSurface(
    enabled = refined && performancePolicy.liquidGlassCardsEnabled,
    shape = shape,
    surfaceColor = BiliColors.PlayerPanel.copy(alpha = surfaceAlpha),
    borderColor = (if (active) BiliColors.BiliPink else BiliColors.TextPrimary).copy(alpha = borderAlpha),
    borderWidth = BiliFocus.RestingBorderWidth,
  )
}

@Composable
private fun PauseIndicatorOverlay(modifier: Modifier = Modifier) {
  val shape = CircleShape
  Box(
    modifier = modifier
      .size(BiliSizing.PlayerPauseIndicatorSize)
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = true,
        surfaceColor = BiliColors.OverlayStrong,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val barWidth = size.width * 0.16f
      val barHeight = size.height * 0.48f
      val gap = size.width * 0.14f
      val left = (size.width - barWidth * 2f - gap) / 2f
      val top = (size.height - barHeight) / 2f
      val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
      drawRoundRect(
        color = BiliColors.TextPrimary,
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
        cornerRadius = radius,
      )
      drawRoundRect(
        color = BiliColors.TextPrimary,
        topLeft = Offset(left + barWidth + gap, top),
        size = Size(barWidth, barHeight),
        cornerRadius = radius,
      )
    }
  }
}

@Composable
private fun PlayerTopOverlay(
  request: PlaybackRequest,
  title: String,
  showClock: Boolean,
  clockText: String,
  modifier: Modifier = Modifier,
) {
  val displayTitle = convertChineseText(title.ifBlank { request.title })
  val ownerName = convertChineseText(request.ownerName)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerTopGradientHeight)
      .background(
        Brush.verticalGradient(
          colors = listOf(BiliColors.OverlayStrong, BiliColors.OverlayTransparent),
        ),
      ),
  ) {
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .fillMaxWidth()
        .padding(
          start = BiliSizing.PlayerOverlayHorizontalPadding,
          top = BiliSizing.PlayerTopPadding,
          end = if (showClock) BiliSizing.PlayerTopTimeReservedWidth else BiliSizing.PlayerOverlayHorizontalPadding,
        ),
      verticalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
    ) {
      Text(
        text = displayTitle,
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerTitle,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      val pubdate = request.formatPubdate()
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (ownerName.isNotBlank()) {
          PlayerMetaItem(
            iconRes = R.drawable.ic_nav_account,
            text = ownerName,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
        if (pubdate != null) {
          PlayerMetaItem(
            iconRes = R.drawable.ic_player_calendar,
            text = stringResource(R.string.player_meta_pubdate, pubdate),
          )
        }
        if (request.viewCount > 0) {
          PlayerMetaItem(
            iconRes = R.drawable.ic_video_play_count,
            text = stringResource(R.string.player_meta_view_count, request.viewCount.formatCompactCountText()),
          )
        }
      }
    }
    if (showClock) {
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

@Composable
private fun PlayerMetaItem(
  @DrawableRes iconRes: Int,
  text: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = null,
      tint = BiliColors.TextSecondary,
      modifier = Modifier.size(BiliSizing.VideoOverlayIconSize),
    )
    Spacer(modifier = Modifier.width(BiliSpacing.Xs))
    Text(
      text = text,
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.PlayerMeta,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun PlayerBottomOverlay(
  request: PlaybackRequest,
  info: PlaybackInfo,
  focusedControl: PlayerControl,
  progressFocused: Boolean,
  positionState: State<Long>,
  durationState: State<Long>,
  bufferedPercentageState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  previewPositionMs: Long?,
  danmakuSettings: DanmakuSettings,
  onlineCountText: String,
  currentCodecText: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerBottomGradientHeight)
      .background(
        Brush.verticalGradient(
          colors = listOf(BiliColors.OverlayTransparent, BiliColors.OverlayStrong),
        ),
      )
      .padding(
        start = BiliSizing.PlayerOverlayHorizontalPadding,
        end = BiliSizing.PlayerOverlayHorizontalPadding,
        bottom = BiliSizing.PlayerBottomPadding,
      ),
    verticalArrangement = Arrangement.Bottom,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TvProgressBar(
        positionState = positionState,
        durationState = durationState,
        bufferedPercentageState = bufferedPercentageState,
        airJumpSegments = airJumpSegments,
        isFocused = progressFocused,
        previewPositionMs = previewPositionMs,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(BiliSpacing.Xl))
      PlayerTimeText(
        positionState = positionState,
        durationState = durationState,
        previewPositionMs = previewPositionMs,
      )
    }
    Spacer(modifier = Modifier.height(BiliSpacing.Lg))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PlayerControl.entries.forEachIndexed { index, control ->
        PlayerIconButton(
          iconRes = control.iconRes,
          contentDescription = stringResource(control.labelRes),
          focused = !progressFocused && focusedControl == control,
        )
        if (index != PlayerControl.entries.lastIndex) {
          Spacer(modifier = Modifier.width(BiliSpacing.Xl))
        }
      }
      Spacer(modifier = Modifier.weight(1f))
      PlayerStatusTexts(
        request = request,
        info = info,
        danmakuSettings = danmakuSettings,
        onlineCountText = onlineCountText,
        currentCodecText = currentCodecText,
      )
    }
  }
}

@Composable
private fun PlayerIconButton(
  @DrawableRes iconRes: Int,
  contentDescription: String,
  focused: Boolean,
) {
  val shape = RoundedCornerShape(BiliRadius.Card)
  Box(
    modifier = Modifier
      .size(BiliSizing.PlayerControlIconButtonSize)
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = focused,
        surfaceColor = if (focused) BiliColors.PlayerControlFocused else BiliColors.PlayerControlIdle,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = BiliColors.TextPrimary,
      modifier = Modifier.size(BiliSizing.PlayerControlIconSize),
    )
  }
}

@Composable
private fun Modifier.playerLiquidGlassSurface(
  shape: Shape,
  focused: Boolean,
  surfaceColor: Color,
): Modifier {
  val performancePolicy = LocalBiliPerformancePolicy.current
  val liquidGlassEnabled = performancePolicy.cinematicVisualEffectsEnabled && performancePolicy.liquidGlassCardsEnabled
  return biliLiquidGlassSurface(
    enabled = liquidGlassEnabled,
    shape = shape,
    surfaceColor = surfaceColor,
    borderColor = BiliColors.TextPrimary.copy(
      alpha = if (focused) {
        BiliFocus.LiquidGlassFocusedBorderAlpha
      } else {
        BiliFocus.LiquidGlassRestingBorderAlpha
      },
    ),
    borderWidth = BiliFocus.RestingBorderWidth,
  )
}

@Composable
private fun Modifier.playerFocusedLiquidGlassSurface(
  shape: Shape,
  focused: Boolean,
  surfaceColor: Color = BiliColors.PlayerPanelFocused,
): Modifier {
  return if (focused) {
    clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = true,
        surfaceColor = surfaceColor,
      )
  } else {
    background(BiliColors.Transparent)
  }
}

@Composable
private fun PlayerStatusTexts(
  request: PlaybackRequest,
  info: PlaybackInfo,
  danmakuSettings: DanmakuSettings,
  onlineCountText: String,
  currentCodecText: String,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Lg),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onlineCountText.isNotBlank()) {
      Text(
        text = stringResource(R.string.player_online_status, onlineCountText),
        color = BiliColors.TextSecondary,
        fontSize = BiliTypography.PlayerStatus,
        maxLines = 1,
      )
    }
    val danmakuText = when {
      !danmakuSettings.enabled -> stringResource(R.string.player_danmaku_off)
      request.danmakuCount > 0 -> stringResource(
        R.string.player_danmaku_count_status,
        request.danmakuCount.formatCompactCountText(),
      )
      else -> stringResource(R.string.player_danmaku_on)
    }
    Text(
      text = danmakuText,
      color = if (danmakuSettings.enabled) BiliColors.TextSecondary else BiliColors.TextTertiary,
      fontSize = BiliTypography.PlayerStatus,
      maxLines = 1,
    )
    Text(
      text = info.selectedQuality.description.withCodecLabel(currentCodecText),
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.PlayerStatus,
      maxLines = 1,
    )
  }
}

@Composable
private fun PlayerTimeText(
  positionState: State<Long>,
  durationState: State<Long>,
  previewPositionMs: Long?,
) {
  Text(
    text = "${(previewPositionMs ?: positionState.value).toPlayerTime()} / ${durationState.value.toPlayerTime()}",
    color = BiliColors.TextPrimary,
    fontSize = BiliTypography.PlayerTime,
    fontWeight = FontWeight.Bold,
  )
}

@Composable
private fun TvProgressBar(
  positionState: State<Long>,
  durationState: State<Long>,
  bufferedPercentageState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  isFocused: Boolean,
  previewPositionMs: Long?,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerProgressTouchHeight),
  ) {
    val positionMs = positionState.value
    val durationMs = durationState.value
    val bufferedPercentage = bufferedPercentageState.value
    val progress = progressFraction(positionMs, durationMs)
    val buffered = (bufferedPercentage / 100f).coerceIn(0f, 1f)
    val preview = previewPositionMs?.let { progressFraction(it, durationMs) }
    val barHeight = if (isFocused) {
      BiliSizing.PlayerProgressFocusedHeight.toPx()
    } else {
      BiliSizing.PlayerProgressHeight.toPx()
    }
    val centerY = size.height / 2f
    val radius = barHeight / 2f
    val knobSize = if (isFocused) {
      BiliSizing.PlayerProgressFocusedKnobSize.toPx()
    } else {
      BiliSizing.PlayerProgressKnobSize.toPx()
    }

    drawRoundBar(1f, centerY, barHeight, radius, BiliColors.ProgressTrack)
    drawRoundBar(buffered, centerY, barHeight, radius, BiliColors.ProgressBuffered)
    drawRoundBar(progress, centerY, barHeight, radius, BiliColors.BiliPink)
    drawAirJumpSegments(airJumpSegments, durationMs, centerY, barHeight, radius)

    if (preview != null) {
      val previewX = preview * size.width
      val previewKnobRadius = knobSize * 0.95f
      val previewKnobCenterX = previewX.coerceIn(previewKnobRadius, size.width - previewKnobRadius)
      drawCircle(
        color = BiliColors.PlayerFocusGlow,
        radius = previewKnobRadius,
        center = Offset(previewKnobCenterX, centerY),
      )
      drawCircle(
        color = BiliColors.TextPrimary,
        radius = knobSize * 0.48f,
        center = Offset(previewKnobCenterX, centerY),
      )
      drawCircle(
        color = BiliColors.BiliPink,
        radius = knobSize * 0.22f,
        center = Offset(previewKnobCenterX, centerY),
      )
    } else {
      val x = progress * size.width
      val knobRadius = if (isFocused) knobSize * 0.62f else knobSize * 0.5f
      val knobCenterX = x.coerceIn(knobRadius, size.width - knobRadius)
      drawCircle(
        color = if (isFocused) BiliColors.PlayerFocusGlow else BiliColors.BiliPink,
        radius = knobRadius,
        center = Offset(knobCenterX, centerY),
      )
      drawCircle(
        color = BiliColors.BiliPink,
        radius = knobSize * 0.38f,
        center = Offset(knobCenterX, centerY),
      )
    }
  }
}

@Composable
private fun MiniProgressBar(
  positionState: State<Long>,
  durationState: State<Long>,
  airJumpSegments: List<AirJumpSegment>,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerMiniProgressHeight),
  ) {
    val positionMs = positionState.value
    val durationMs = durationState.value
    val centerY = size.height / 2f
    val barHeight = size.height
    val radius = barHeight / 2f
    drawRoundBar(1f, centerY, barHeight, radius, BiliColors.ProgressTrack)
    drawRoundBar(progressFraction(positionMs, durationMs), centerY, barHeight, radius, BiliColors.BiliPink)
    drawAirJumpSegments(airJumpSegments, durationMs, centerY, barHeight, radius)
  }
}

@Composable
private fun SeekPreviewOverlay(
  previewPositionMs: Long,
  durationMs: Long,
  deltaMs: Long?,
  videoshotData: VideoshotData?,
  videoshotSprites: Map<String, ImageBitmap>,
  modifier: Modifier = Modifier,
) {
  val frame = videoshotData?.frameAt(previewPositionMs, durationMs)
  val spriteBitmap = frame?.let { videoshotSprites[it.imageUrl] }
  if (frame != null && spriteBitmap != null) {
    Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      VideoshotFramePreview(
        image = spriteBitmap,
        frame = frame,
      )
      Spacer(modifier = Modifier.height(BiliSpacing.Md))
      Text(
        text = "${previewPositionMs.toPlayerTime()} / ${durationMs.toPlayerTime()}",
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerStatus,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
          .clip(RoundedCornerShape(BiliRadius.Pill))
          .background(BiliColors.OverlayStrong)
          .padding(horizontal = BiliSpacing.Lg, vertical = BiliSpacing.Sm),
      )
      SeekPreviewDeltaText(deltaMs = deltaMs)
    }
    return
  }

  val shape = RoundedCornerShape(BiliRadius.Panel)
  Column(
    modifier = modifier
      .width(BiliSizing.PlayerSeekPreviewWidth)
      .height(BiliSizing.PlayerSeekPreviewHeight)
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = true,
        surfaceColor = BiliColors.OverlayStrong,
      )
      .padding(BiliSpacing.Lg),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = stringResource(R.string.player_seek_preview),
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.PlayerStatus,
      maxLines = 1,
    )
    Text(
      text = "${previewPositionMs.toPlayerTime()} / ${durationMs.toPlayerTime()}",
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerSeekPreview,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
    SeekPreviewDeltaText(deltaMs = deltaMs)
  }
}

@Composable
private fun SeekPreviewDeltaText(deltaMs: Long?) {
  val deltaSeconds = ((deltaMs ?: 0L) / 1000L).toInt()
  if (deltaSeconds == 0) return
  Text(
    text = stringResource(
      if (deltaSeconds > 0) {
        R.string.player_touch_seek_delta_forward
      } else {
        R.string.player_touch_seek_delta_backward
      },
      abs(deltaSeconds),
    ),
    color = if (deltaSeconds > 0) BiliColors.AirJumpGreen else BiliColors.BiliPink,
    fontSize = BiliTypography.PlayerStatus,
    fontWeight = FontWeight.Bold,
    maxLines = 1,
    modifier = Modifier
      .clip(RoundedCornerShape(BiliRadius.Pill))
      .background(BiliColors.OverlayStrong)
      .padding(horizontal = BiliSpacing.Md, vertical = BiliSpacing.Xs),
  )
}

@Composable
private fun VideoshotFramePreview(
  image: ImageBitmap,
  frame: VideoshotFrame,
) {
  val rawDisplayWidth = frame.width * SeekPreviewSpriteScale
  val rawDisplayHeight = frame.height * SeekPreviewSpriteScale
  val fitRatio = min(
    1f,
    min(
      SeekPreviewSpriteMaxWidth / rawDisplayWidth,
      SeekPreviewSpriteMaxHeight / rawDisplayHeight,
    ),
  )
  val renderScale = SeekPreviewSpriteScale * fitRatio
  val displayWidth = (frame.width * renderScale).dp
  val displayHeight = (frame.height * renderScale).dp

  Box(
    modifier = Modifier
      .width(displayWidth)
      .height(displayHeight)
      .clip(RoundedCornerShape(BiliRadius.Card))
      .background(BiliColors.SurfaceElevated),
    contentAlignment = Alignment.Center,
  ) {
    VideoshotCroppedCanvas(
      image = image,
      frame = frame,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
private fun VideoshotCroppedCanvas(
  image: ImageBitmap,
  frame: VideoshotFrame,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val metadataSpriteWidth = frame.spriteWidth.coerceAtLeast(frame.width).coerceAtLeast(1)
    val metadataSpriteHeight = frame.spriteHeight.coerceAtLeast(frame.height).coerceAtLeast(1)
    val sourceScaleX = image.width.toFloat() / metadataSpriteWidth.toFloat()
    val sourceScaleY = image.height.toFloat() / metadataSpriteHeight.toFloat()
    val srcX = (frame.x * sourceScaleX).roundToInt().coerceIn(0, (image.width - 1).coerceAtLeast(0))
    val srcY = (frame.y * sourceScaleY).roundToInt().coerceIn(0, (image.height - 1).coerceAtLeast(0))
    val srcRight = ((frame.x + frame.width) * sourceScaleX).roundToInt().coerceIn(srcX + 1, image.width)
    val srcBottom = ((frame.y + frame.height) * sourceScaleY).roundToInt().coerceIn(srcY + 1, image.height)
    drawImage(
      image = image,
      srcOffset = IntOffset(srcX, srcY),
      srcSize = IntSize(srcRight - srcX, srcBottom - srcY),
      dstOffset = IntOffset.Zero,
      dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
    )
  }
}

@Composable
private fun PlayerEpisodesPanel(
  episodes: List<PlaybackEpisode>,
  currentCid: Long,
  focusedIndex: Int,
  onEpisodeClick: ((Int) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val shape = RoundedCornerShape(topStart = BiliRadius.Panel, bottomStart = BiliRadius.Panel)
  LaunchedEffect(focusedIndex, episodes.size) {
    if (episodes.isNotEmpty()) {
      val target = focusedIndex.coerceIn(0, episodes.lastIndex)
      if (performancePolicy.smoothScrollingEnabled) {
        listState.animateScrollToItem(target)
      } else {
        listState.scrollToItem(target)
      }
    }
  }

  Column(
    modifier = modifier
      .width(BiliSizing.PlayerSettingsPanelWidth)
      .fillMaxHeight()
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = false,
        surfaceColor = BiliColors.PlayerPanel,
      ),
  ) {
    PlayerPanelTitleRow(titleRes = R.string.player_panel_episodes)
    PlayerPanelDivider()
    if (episodes.isEmpty()) {
      PlayerPanelLoadingOrEmpty(loading = false)
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
      ) {
        itemsIndexed(episodes) { index, episode ->
          EpisodeRow(
            title = convertChineseText(episode.panelTitle(index)),
            focused = focusedIndex == index,
            selected = episode.cid == currentCid,
            onClick = onEpisodeClick?.let { click -> { click(index) } },
          )
        }
      }
    }
  }
}

@Composable
private fun PlayerVideoListPanel(
  titleRes: Int,
  request: PlaybackRequest,
  videos: List<VideoSummary>,
  loading: Boolean,
  focusedIndex: Int,
  showUploaderHeader: Boolean,
  upVideoOrder: String,
  upFollowed: Boolean,
  upFollowLoading: Boolean,
  onVideoClick: ((Int) -> Unit)? = null,
  onSortClick: (() -> Unit)? = null,
  onFollowClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val shape = RoundedCornerShape(topStart = BiliRadius.Panel, bottomStart = BiliRadius.Panel)
  val focusedVideoIndex = if (showUploaderHeader) focusedIndex - UpPanelHeaderItemCount else focusedIndex
  val scrollRevealPaddingPx = with(LocalDensity.current) { BiliSpacing.Sm.roundToPx() }
  LaunchedEffect(focusedVideoIndex, videos.size, scrollRevealPaddingPx) {
    if (videos.isNotEmpty() && focusedVideoIndex >= 0) {
      val target = focusedVideoIndex.coerceIn(0, videos.lastIndex)
      val layoutInfo = listState.layoutInfo
      val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item -> item.index == target }
      if (targetItem == null) {
        if (performancePolicy.smoothScrollingEnabled) {
          listState.animateScrollToItem(target)
        } else {
          listState.scrollToItem(target)
        }
      } else {
        val viewportStart = layoutInfo.viewportStartOffset + scrollRevealPaddingPx
        val viewportEnd = layoutInfo.viewportEndOffset - scrollRevealPaddingPx
        val itemStart = targetItem.offset
        val itemEnd = targetItem.offset + targetItem.size
        val scrollDelta = when {
          itemStart < viewportStart -> itemStart - viewportStart
          itemEnd > viewportEnd -> itemEnd - viewportEnd
          else -> 0
        }
        if (scrollDelta != 0) {
          if (performancePolicy.smoothScrollingEnabled) {
            listState.animateScrollBy(scrollDelta.toFloat())
          } else {
            listState.scroll {
              scrollBy(scrollDelta.toFloat())
            }
          }
        }
      }
    }
  }

  Column(
    modifier = modifier
      .width(BiliSizing.PlayerContentPanelWidth)
      .fillMaxHeight()
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = false,
        surfaceColor = BiliColors.PlayerPanel,
      ),
  ) {
    if (showUploaderHeader) {
      UploaderPanelHeader(
        request = request,
        order = upVideoOrder,
        followed = upFollowed,
        followLoading = upFollowLoading,
        focusedIndex = focusedIndex,
        onSortClick = onSortClick,
        onFollowClick = onFollowClick,
      )
    } else {
      PlayerPanelTitleRow(titleRes = titleRes)
    }
    PlayerPanelDivider()
    when {
      loading || videos.isEmpty() -> PlayerPanelLoadingOrEmpty(loading = loading)
      else -> LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
      ) {
        itemsIndexed(
          items = videos,
          key = { _, video -> video.bvid },
        ) { index, video ->
          VideoPanelRow(
            video = video,
            focused = if (showUploaderHeader) focusedIndex == index + UpPanelHeaderItemCount else focusedIndex == index,
            showOwnerName = !showUploaderHeader,
            onClick = onVideoClick?.let { click -> { click(index) } },
          )
        }
      }
    }
  }
}

@Composable
private fun PlayerCommentsPanel(
  comments: List<PlayerComment>,
  loading: Boolean,
  focusedIndex: Int,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(topStart = BiliRadius.Panel, bottomStart = BiliRadius.Panel)
  val listState = rememberLazyListState()
  val performancePolicy = LocalBiliPerformancePolicy.current
  LaunchedEffect(focusedIndex, comments.size) {
    if (comments.isNotEmpty()) {
      val target = focusedIndex.coerceIn(0, comments.lastIndex)
      if (performancePolicy.smoothScrollingEnabled) {
        listState.animateScrollToItem(target)
      } else {
        listState.scrollToItem(target)
      }
    }
  }

  Column(
    modifier = modifier
      .width(BiliSizing.PlayerContentPanelWidth)
      .fillMaxHeight()
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = false,
        surfaceColor = BiliColors.PlayerPanel,
      ),
  ) {
    PlayerPanelTitleRow(titleRes = R.string.player_panel_comments)
    PlayerPanelDivider()
    when {
      loading || comments.isEmpty() -> PlayerPanelLoadingOrEmpty(loading = loading)
      else -> LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
      ) {
        itemsIndexed(
          items = comments,
          key = { index, comment -> comment.rpid.takeIf { it > 0L } ?: index },
        ) { index, comment ->
          CommentPanelRow(
            comment = comment,
            focused = focusedIndex == index,
          )
        }
      }
    }
  }
}

@Composable
private fun PlayerPanelTitleRow(titleRes: Int) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerSettingsHeaderHeight)
      .padding(horizontal = BiliSpacing.Xl),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(titleRes),
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerPanelTitle,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun UploaderPanelHeader(
  request: PlaybackRequest,
  order: String,
  followed: Boolean,
  followLoading: Boolean,
  focusedIndex: Int,
  onSortClick: (() -> Unit)? = null,
  onFollowClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val ownerName = convertChineseText(request.ownerName)
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val avatarRequest = remember(
    context,
    request.ownerFace,
    performancePolicy.ownerAvatarSizePx,
    performancePolicy.ownerAvatarRgb565Enabled,
    performancePolicy.imageMemoryCacheEnabled,
  ) {
    buildOwnerAvatarRequest(
      context = context,
      url = request.ownerFace,
      sizePx = performancePolicy.ownerAvatarSizePx,
      allowRgb565 = performancePolicy.ownerAvatarRgb565Enabled,
      memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
    )
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerUpPanelHeaderHeight)
      .padding(horizontal = BiliSpacing.Lg, vertical = BiliSpacing.Md),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (request.ownerFace.isNotBlank()) {
      AsyncImage(
        model = avatarRequest,
        contentDescription = ownerName,
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = Modifier
          .size(BiliSizing.PlayerPanelAvatarSize)
          .clip(CircleShape)
          .background(BiliColors.SurfaceElevated),
      )
    } else {
      Box(
        modifier = Modifier
          .size(BiliSizing.PlayerPanelAvatarSize)
          .clip(CircleShape)
          .background(BiliColors.SurfaceElevated),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_nav_account),
          contentDescription = null,
          tint = BiliColors.TextSecondary,
          modifier = Modifier.size(BiliSizing.PlayerSettingsIconSize),
        )
      }
    }
    Spacer(modifier = Modifier.width(BiliSpacing.Md))
    Text(
      text = ownerName.ifBlank { stringResource(R.string.player_panel_unknown_up) },
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerPanelTitle,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(BiliSpacing.Sm))
    UpPanelChip(
      text = stringResource(if (order == UpVideoOrderLatest) R.string.player_up_sort_latest else R.string.player_up_sort_hot),
      focused = focusedIndex == UpFocusSort,
      selected = true,
      onClick = onSortClick,
    )
    Spacer(modifier = Modifier.width(BiliSpacing.Sm))
    UpPanelChip(
      text = when {
        followLoading -> stringResource(R.string.player_up_follow_loading)
        followed -> stringResource(R.string.player_up_followed)
        else -> stringResource(R.string.player_up_follow)
      },
      focused = focusedIndex == UpFocusFollow,
      selected = followed,
      onClick = onFollowClick,
    )
  }
}

@Composable
private fun UpPanelChip(
  text: String,
  focused: Boolean,
  selected: Boolean,
  onClick: (() -> Unit)? = null,
) {
  val shape = RoundedCornerShape(BiliRadius.Pill)
  val performancePolicy = LocalBiliPerformancePolicy.current
  val liquidGlassEnabled = performancePolicy.cinematicVisualEffectsEnabled && performancePolicy.liquidGlassCardsEnabled
  val surfaceColor = when {
    focused -> BiliColors.PlayerPanelFocused
    selected -> BiliColors.BiliPink.copy(alpha = UpPanelChipSelectedSurfaceAlpha)
    else -> BiliColors.PlayerControlIdle
  }
  val borderColor = when {
    focused -> BiliColors.TextPrimary.copy(alpha = UpPanelChipFocusedBorderAlpha)
    selected -> BiliColors.BiliPink.copy(alpha = UpPanelChipSelectedBorderAlpha)
    else -> BiliColors.TextPrimary.copy(alpha = UpPanelChipRestingBorderAlpha)
  }
  Box(
    modifier = Modifier
      .height(BiliSizing.PlayerPanelChipHeight)
      .clip(shape)
      .biliLiquidGlassSurface(
        enabled = liquidGlassEnabled,
        shape = shape,
        surfaceColor = surfaceColor,
        borderColor = borderColor,
        borderWidth = if (focused) BiliFocus.BorderWidth else BiliFocus.RestingBorderWidth,
      )
      .optionalTouchClick(onClick)
      .padding(horizontal = BiliSpacing.Md),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = if (selected && !focused) {
        BiliColors.BiliPink
      } else {
        BiliColors.TextPrimary
      },
      fontSize = BiliTypography.PlayerSettingValue,
      fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun CommentPanelRow(
  comment: PlayerComment,
  focused: Boolean,
) {
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val authorName = convertChineseText(comment.authorName)
  val message = convertChineseText(comment.message)
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val avatarRequest = remember(
    context,
    comment.authorAvatar,
    performancePolicy.ownerAvatarSizePx,
    performancePolicy.ownerAvatarRgb565Enabled,
    performancePolicy.imageMemoryCacheEnabled,
  ) {
    buildOwnerAvatarRequest(
      context = context,
      url = comment.authorAvatar,
      sizePx = performancePolicy.ownerAvatarSizePx,
      allowRgb565 = performancePolicy.ownerAvatarRgb565Enabled,
      memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
    )
  }
  val separator = stringResource(R.string.player_meta_separator)
  val likeText = if (comment.likeCount > 0) {
    stringResource(R.string.player_comment_like_count, comment.likeCount.formatCompactCountText())
  } else {
    ""
  }
  val replyText = if (comment.replyCount > 0) {
    stringResource(R.string.player_comment_reply_count, comment.replyCount.formatCompactCountText())
  } else {
    ""
  }
  val metaText = listOf(
    comment.commentTimeText(),
    likeText,
    replyText,
  )
    .filter(String::isNotBlank)
    .joinToString(separator)
  val shape = RoundedCornerShape(BiliRadius.Card)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = BiliSpacing.Xs, vertical = BiliSpacing.Xxs)
      .playerFocusedLiquidGlassSurface(shape = shape, focused = focused)
      .padding(BiliSpacing.Md),
    verticalAlignment = Alignment.Top,
  ) {
    if (comment.authorAvatar.isNotBlank()) {
      AsyncImage(
        model = avatarRequest,
        contentDescription = authorName,
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = Modifier
          .size(BiliSizing.PlayerPanelAvatarSize)
          .clip(CircleShape)
          .background(BiliColors.SurfaceElevated),
      )
    } else {
      Box(
        modifier = Modifier
          .size(BiliSizing.PlayerPanelAvatarSize)
          .clip(CircleShape)
          .background(BiliColors.SurfaceElevated),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_nav_account),
          contentDescription = null,
          tint = BiliColors.TextSecondary,
          modifier = Modifier.size(BiliSizing.PlayerSettingsIconSize),
        )
      }
    }
    Spacer(modifier = Modifier.width(BiliSpacing.Md))
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(BiliSpacing.Xs),
    ) {
      Text(
        text = authorName.ifBlank { stringResource(R.string.player_panel_unknown_up) },
        color = if (focused) BiliColors.BiliPink else BiliColors.TextSecondary,
        fontSize = BiliTypography.PlayerSettingValue,
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = message,
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerSettingTitle,
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
      )
      if (metaText.isNotBlank()) {
        Text(
          text = metaText,
          color = BiliColors.TextTertiary,
          fontSize = BiliTypography.PlayerSettingValue,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun UnfollowConfirmDialog(
  focusedConfirm: Boolean,
  onCancel: (() -> Unit)? = null,
  onConfirm: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(BiliRadius.Panel)
  Column(
    modifier = modifier
      .width(BiliSizing.PlayerUnfollowDialogWidth)
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = false,
        surfaceColor = BiliColors.OverlayScrim,
      )
      .padding(BiliSpacing.Xl),
    verticalArrangement = Arrangement.spacedBy(BiliSpacing.Lg),
  ) {
    Text(
      text = stringResource(R.string.player_unfollow_confirm_title),
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerPanelTitle,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
    Text(
      text = stringResource(R.string.player_unfollow_confirm_message),
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.BodySmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
      modifier = Modifier.fillMaxWidth(),
    ) {
      ConfirmDialogButton(
        text = stringResource(R.string.player_unfollow_confirm_cancel),
        focused = !focusedConfirm,
        destructive = false,
        onClick = onCancel,
        modifier = Modifier.weight(1f),
      )
      ConfirmDialogButton(
        text = stringResource(R.string.player_unfollow_confirm_action),
        focused = focusedConfirm,
        destructive = true,
        onClick = onConfirm,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun ConfirmDialogButton(
  text: String,
  focused: Boolean,
  destructive: Boolean,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(BiliRadius.Card)
  val surfaceColor = when {
    focused && destructive -> BiliColors.BiliPink
    focused -> BiliColors.PlayerPanelFocused
    else -> BiliColors.PlayerControlIdle
  }
  Box(
    modifier = modifier
      .height(BiliSizing.PlayerUnfollowDialogButtonHeight)
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = focused,
        surfaceColor = surfaceColor,
      )
      .optionalTouchClick(onClick)
      .padding(horizontal = BiliSpacing.Md),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerSettingTitle,
      fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun PlayerPanelDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerSettingsDividerHeight)
      .background(BiliColors.PlayerPanelDivider),
  )
}

@Composable
private fun PlayerPanelLoadingOrEmpty(loading: Boolean) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(BiliSpacing.Lg),
    ) {
      if (loading) {
        CircularProgressIndicator(color = BiliColors.BiliPink)
      }
      Text(
        text = stringResource(if (loading) R.string.player_panel_loading else R.string.player_panel_empty),
        color = BiliColors.TextSecondary,
        fontSize = BiliTypography.PlayerStatus,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun EpisodeRow(
  title: String,
  focused: Boolean,
  selected: Boolean,
  onClick: (() -> Unit)? = null,
) {
  val shape = RoundedCornerShape(BiliRadius.Card)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerEpisodeRowHeight)
      .playerFocusedLiquidGlassSurface(shape = shape, focused = focused)
      .optionalTouchClick(onClick),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      color = if (selected) BiliColors.BiliPink else BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerSettingTitle,
      fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = BiliSpacing.Lg),
    )
  }
}

@Composable
private fun VideoPanelRow(
  video: VideoSummary,
  focused: Boolean,
  showOwnerName: Boolean,
  onClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val title = convertChineseText(video.title)
  val ownerName = convertChineseText(video.ownerName)
  val imageRequest = remember(
    context,
    video.pic,
    performancePolicy.videoThumbnailWidthPx,
    performancePolicy.videoThumbnailHeightPx,
    performancePolicy.videoThumbnailRgb565Enabled,
    performancePolicy.imageMemoryCacheEnabled,
  ) {
    buildVideoThumbnailRequest(
      context = context,
      url = video.pic,
      widthPx = performancePolicy.videoThumbnailWidthPx,
      heightPx = performancePolicy.videoThumbnailHeightPx,
      allowRgb565 = performancePolicy.videoThumbnailRgb565Enabled,
      memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
    )
  }
  val separator = stringResource(R.string.player_meta_separator)
  val viewText = if (video.view > 0) video.view.formatCompactCountText() else ""
  val danmakuText = if (video.danmaku > 0) video.danmaku.formatCompactCountText() else ""
  val pubdateText = video.panelPubdateText()
  val metaText = listOf(
    ownerName.takeIf { showOwnerName }.orEmpty(),
    pubdateText,
  )
    .filter(String::isNotBlank)
    .joinToString(separator)

  val shape = RoundedCornerShape(BiliRadius.Card)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = BiliSpacing.Xs, vertical = BiliSpacing.Xxs)
      .height(BiliSizing.PlayerPanelVideoRowHeight)
      .playerFocusedLiquidGlassSurface(shape = shape, focused = focused)
      .optionalTouchClick(onClick)
      .padding(BiliSpacing.Sm),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .width(BiliSizing.PlayerPanelVideoThumbnailWidth)
        .height(BiliSizing.PlayerPanelVideoThumbnailHeight)
        .clip(RoundedCornerShape(BiliRadius.Card))
        .background(BiliColors.SurfaceElevated),
    ) {
      AsyncImage(
        model = imageRequest,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
          modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(BiliSizing.VideoCoverGradientHeight)
          .background(
            Brush.verticalGradient(
              colors = listOf(
                BiliColors.OverlayTransparent,
                BiliColors.OverlayScrim,
              ),
            ),
          ),
      )
      if (viewText.isNotBlank() || danmakuText.isNotBlank() || video.duration > 0) {
        Row(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = BiliSpacing.Sm, vertical = BiliSpacing.Sm),
          horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (viewText.isNotBlank()) {
              VideoPanelMetric(
                iconRes = R.drawable.ic_video_play_count,
                contentDescription = stringResource(R.string.video_play_count_content_description),
                text = viewText,
              )
            }
            if (danmakuText.isNotBlank()) {
              if (viewText.isNotBlank()) {
                Spacer(modifier = Modifier.width(BiliSpacing.Sm))
              }
              VideoPanelMetric(
                iconRes = R.drawable.ic_video_danmaku_count,
                contentDescription = stringResource(R.string.video_danmaku_count_content_description),
                text = danmakuText,
              )
            }
          }
          if (video.duration > 0) {
            Text(
              text = (video.duration.toLong() * 1000L).toPlayerTime(),
              color = BiliColors.TextPrimary,
              fontSize = BiliTypography.CardOverlay,
              maxLines = 1,
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.width(BiliSpacing.Md))
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = title,
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerSettingTitle,
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (metaText.isNotBlank()) {
        Spacer(modifier = Modifier.height(BiliSpacing.Xs))
        Text(
          text = metaText,
          color = BiliColors.TextTertiary,
          fontSize = BiliTypography.PlayerSettingValue,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun VideoPanelMetric(
  @DrawableRes iconRes: Int,
  contentDescription: String,
  text: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = BiliColors.TextSecondary,
      modifier = Modifier.size(BiliSizing.VideoOverlayIconSize),
    )
    Spacer(modifier = Modifier.width(BiliSpacing.Xs))
    Text(
      text = text,
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.CardOverlay,
      maxLines = 1,
    )
  }
}

@Composable
private fun PlayerSettingsPanel(
  activePanel: PlayerPanel,
  focusedIndex: Int,
  info: PlaybackInfo,
  currentCodecText: String,
  playbackSpeed: Float,
  danmakuSettings: DanmakuSettings,
  onRowClick: ((Int) -> Unit)? = null,
  onRowAdjust: ((Int, Int) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(topStart = BiliRadius.Panel, bottomStart = BiliRadius.Panel)
  val listState = rememberLazyListState()
  val performancePolicy = LocalBiliPerformancePolicy.current
  val rowCount = activePanel.settingsRowCount(info)
  LaunchedEffect(activePanel, focusedIndex, rowCount) {
    if (rowCount > 0) {
      val target = focusedIndex.coerceIn(0, rowCount - 1)
      if (performancePolicy.smoothScrollingEnabled) {
        listState.animateScrollToItem(target)
      } else {
        listState.scrollToItem(target)
      }
    }
  }
  Column(
    modifier = modifier
      .width(BiliSizing.PlayerSettingsPanelWidth)
      .fillMaxHeight()
      .clip(shape)
      .playerLiquidGlassSurface(
        shape = shape,
        focused = false,
        surfaceColor = BiliColors.PlayerPanel,
      ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(BiliSizing.PlayerSettingsHeaderHeight)
        .padding(horizontal = BiliSpacing.Xl),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(activePanel.titleRes),
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerPanelTitle,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.weight(1f))
      if (activePanel == PlayerPanel.Main) {
        Icon(
          painter = painterResource(R.drawable.ic_nav_settings),
          contentDescription = null,
          tint = BiliColors.TextTertiary,
          modifier = Modifier.size(BiliSizing.PlayerSettingsIconSize),
        )
      }
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(BiliSizing.PlayerSettingsDividerHeight)
        .background(BiliColors.PlayerPanelDivider),
    )
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
    ) {
      when (activePanel) {
        PlayerPanel.Main -> {
          item(key = "quality") {
            SettingsRow(
              iconRes = R.drawable.ic_player_hd,
              title = stringResource(R.string.player_settings_quality),
              value = info.selectedQuality.description.withCodecLabel(currentCodecText),
              focused = focusedIndex == 0,
              trailingChevron = true,
              onClick = onRowClick?.let { click -> { click(0) } },
            )
          }
          item(key = "danmaku") {
            SettingsRow(
              iconRes = R.drawable.ic_player_subtitles,
              title = stringResource(R.string.player_settings_danmaku),
              value = if (danmakuSettings.enabled) stringResource(R.string.player_value_on) else stringResource(R.string.player_value_off),
              focused = focusedIndex == 1,
              trailingChevron = true,
              onClick = onRowClick?.let { click -> { click(1) } },
            )
          }
          item(key = "speed") {
            SettingsRow(
              iconRes = R.drawable.ic_player_speed,
              title = stringResource(R.string.player_settings_speed),
              value = playbackSpeed.speedText(),
              focused = focusedIndex == 2,
              trailingChevron = true,
              onClick = onRowClick?.let { click -> { click(2) } },
            )
          }
        }
        PlayerPanel.Quality -> {
          val qualities = info.qualities.ifEmpty { listOf(info.selectedQuality) }
          itemsIndexed(qualities, key = { _, quality -> quality.id }) { index, quality ->
            SettingsRow(
              iconRes = R.drawable.ic_player_hd,
              title = convertChineseText(quality.description),
              value = if (quality.id == info.selectedQuality.id) stringResource(R.string.player_value_current) else "",
              focused = focusedIndex == index,
              trailingCheck = quality.id == info.selectedQuality.id,
              onClick = onRowClick?.let { click -> { click(index) } },
            )
          }
        }
        PlayerPanel.Danmaku -> {
          val rows = danmakuSettingRows(danmakuSettings)
          itemsIndexed(rows) { index, row ->
            SettingsRow(
              iconRes = row.iconRes,
              title = stringResource(row.titleRes),
              value = row.valueRes?.let { valueRes -> stringResource(valueRes) } ?: row.value,
              focused = focusedIndex == index,
              adjustable = row.adjustable,
              onClick = onRowClick?.let { click -> { click(index) } },
              onAdjust = if (row.adjustable) {
                onRowAdjust?.let { adjust -> { delta -> adjust(index, delta) } }
              } else {
                null
              },
            )
          }
        }
        PlayerPanel.Speed -> {
          itemsIndexed(PlayerSpeedOptions) { index, speed ->
            val selected = speed == playbackSpeed
            SettingsRow(
              iconRes = R.drawable.ic_player_speed,
              title = speed.speedText(),
              value = if (selected) stringResource(R.string.player_value_current) else "",
              focused = focusedIndex == index,
              trailingCheck = selected,
              onClick = onRowClick?.let { click -> { click(index) } },
            )
          }
        }
        PlayerPanel.Episodes,
        PlayerPanel.UpVideos,
        PlayerPanel.RelatedVideos,
        PlayerPanel.Comments,
        PlayerPanel.None -> Unit
      }
    }
  }
}

private fun PlayerPanel.settingsRowCount(info: PlaybackInfo): Int {
  return when (this) {
    PlayerPanel.Main -> 3
    PlayerPanel.Quality -> info.qualities.size.coerceAtLeast(1)
    PlayerPanel.Danmaku -> DanmakuSettingsRowCount
    PlayerPanel.Speed -> PlayerSpeedOptions.size
    PlayerPanel.Episodes,
    PlayerPanel.UpVideos,
    PlayerPanel.RelatedVideos,
    PlayerPanel.Comments,
    PlayerPanel.None -> 0
  }
}

@Composable
private fun MainSettingsRows(
  focusedIndex: Int,
  quality: String,
  playbackSpeed: Float,
  danmakuSettings: DanmakuSettings,
) {
  SettingsRow(
    iconRes = R.drawable.ic_player_hd,
    title = stringResource(R.string.player_settings_quality),
    value = quality,
    focused = focusedIndex == 0,
    trailingChevron = true,
  )
  SettingsRow(
    iconRes = R.drawable.ic_player_subtitles,
    title = stringResource(R.string.player_settings_danmaku),
    value = if (danmakuSettings.enabled) stringResource(R.string.player_value_on) else stringResource(R.string.player_value_off),
    focused = focusedIndex == 1,
    trailingChevron = true,
  )
  SettingsRow(
    iconRes = R.drawable.ic_player_speed,
    title = stringResource(R.string.player_settings_speed),
    value = playbackSpeed.speedText(),
    focused = focusedIndex == 2,
    trailingChevron = true,
  )
}

@Composable
private fun QualityRows(
  focusedIndex: Int,
  qualities: List<PlaybackQuality>,
  currentQuality: PlaybackQuality,
) {
  qualities.ifEmpty { listOf(currentQuality) }.forEachIndexed { index, quality ->
    SettingsRow(
      iconRes = R.drawable.ic_player_hd,
      title = convertChineseText(quality.description),
      value = if (quality.id == currentQuality.id) stringResource(R.string.player_value_current) else "",
      focused = focusedIndex == index,
      trailingCheck = quality.id == currentQuality.id,
    )
  }
}

private fun danmakuSettingRows(settings: DanmakuSettings): List<DanmakuSettingRow> {
  return listOf(
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_toggle,
      iconRes = R.drawable.ic_player_subtitles,
      valueRes = if (settings.enabled) R.string.player_value_on else R.string.player_value_off,
    ),
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_opacity,
      iconRes = R.drawable.ic_player_subtitles,
      value = "%.1f".format(Locale.US, settings.opacity),
      adjustable = true,
    ),
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_font_size,
      iconRes = R.drawable.ic_player_subtitles,
      value = settings.fontSize.toString(),
      adjustable = true,
    ),
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_area,
      iconRes = R.drawable.ic_player_subtitles,
      valueRes = settings.area.areaTextRes(),
      adjustable = true,
    ),
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_speed,
      iconRes = R.drawable.ic_player_speed,
      value = settings.speed.toString(),
      adjustable = true,
    ),
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_top,
      iconRes = R.drawable.ic_player_subtitles,
      valueRes = if (settings.allowTop) R.string.player_value_on else R.string.player_value_off,
    ),
    DanmakuSettingRow(
      titleRes = R.string.player_settings_danmaku_bottom,
      iconRes = R.drawable.ic_player_subtitles,
      valueRes = if (settings.allowBottom) R.string.player_value_on else R.string.player_value_off,
    ),
  )
}

private data class DanmakuSettingRow(
  @param:StringRes val titleRes: Int,
  @param:DrawableRes val iconRes: Int,
  val value: String = "",
  @param:StringRes val valueRes: Int? = null,
  val adjustable: Boolean = false,
)

@Composable
private fun SpeedRows(
  focusedIndex: Int,
  playbackSpeed: Float,
) {
  PlayerSpeedOptions.forEachIndexed { index, speed ->
    val selected = speed == playbackSpeed
    SettingsRow(
      iconRes = R.drawable.ic_player_speed,
      title = speed.speedText(),
      value = if (selected) stringResource(R.string.player_value_current) else "",
      focused = focusedIndex == index,
      trailingCheck = selected,
    )
  }
}

@Composable
private fun SettingsRow(
  @DrawableRes iconRes: Int,
  title: String,
  value: String,
  focused: Boolean,
  trailingChevron: Boolean = false,
  trailingCheck: Boolean = false,
  adjustable: Boolean = false,
  onClick: (() -> Unit)? = null,
  onAdjust: ((Int) -> Unit)? = null,
) {
  val shape = RoundedCornerShape(BiliRadius.Card)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerSettingsRowHeight)
      .playerFocusedLiquidGlassSurface(shape = shape, focused = focused)
      .optionalTouchClick(onClick)
      .padding(horizontal = BiliSpacing.Lg),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(BiliSizing.PlayerSettingsIconSize)
        .clip(CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = if (focused) BiliColors.BiliPink else BiliColors.TextSecondary,
        modifier = Modifier.size(BiliSizing.PlayerSettingsIconSize),
      )
    }
    Spacer(modifier = Modifier.width(BiliSpacing.Md))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = if (trailingCheck) BiliColors.BiliPink else BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerSettingTitle,
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (value.isNotBlank() && !adjustable) {
        Text(
          text = value,
          color = if (trailingCheck) BiliColors.BiliPink else BiliColors.TextTertiary,
          fontSize = BiliTypography.PlayerSettingValue,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (adjustable && value.isNotBlank()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_player_chevron_left),
          contentDescription = null,
          tint = if (focused) BiliColors.BiliPink else BiliColors.TextTertiary,
          modifier = Modifier
            .size(BiliSizing.PlayerSettingsChevronSize)
            .optionalTouchClick(onAdjust?.let { adjust -> { adjust(-1) } }),
        )
        Spacer(modifier = Modifier.width(BiliSpacing.Sm))
        Text(
          text = value,
          color = if (focused) BiliColors.BiliPink else BiliColors.TextTertiary,
          fontSize = BiliTypography.PlayerSettingValue,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(BiliSpacing.Sm))
        Icon(
          painter = painterResource(R.drawable.ic_player_chevron_right),
          contentDescription = null,
          tint = if (focused) BiliColors.BiliPink else BiliColors.TextTertiary,
          modifier = Modifier
            .size(BiliSizing.PlayerSettingsChevronSize)
            .optionalTouchClick(onAdjust?.let { adjust -> { adjust(1) } }),
        )
      }
    }
    when {
      trailingCheck -> Icon(
        painter = painterResource(R.drawable.ic_player_check),
        contentDescription = null,
        tint = BiliColors.BiliPink,
        modifier = Modifier.size(BiliSizing.PlayerSettingsChevronSize),
      )
      trailingChevron -> Icon(
        painter = painterResource(R.drawable.ic_player_chevron_right),
        contentDescription = null,
        tint = BiliColors.TextTertiary,
        modifier = Modifier.size(BiliSizing.PlayerSettingsChevronSize),
      )
    }
  }
}

private fun Modifier.optionalTouchClick(onClick: (() -> Unit)?): Modifier {
  return if (onClick == null) this else clickable(onClick = onClick)
}

private fun DrawScope.drawRoundBar(
  fraction: Float,
  centerY: Float,
  height: Float,
  radius: Float,
  color: Color,
) {
  drawRoundRect(
    color = color,
    topLeft = Offset(0f, centerY - height / 2f),
    size = Size(size.width * fraction.coerceIn(0f, 1f), height),
    cornerRadius = CornerRadius(radius, radius),
  )
}

private fun DrawScope.drawAirJumpSegments(
  segments: List<AirJumpSegment>,
  durationMs: Long,
  centerY: Float,
  height: Float,
  radius: Float,
) {
  if (durationMs <= 0L || segments.isEmpty()) return
  segments.forEach { segment ->
    val startFraction = progressFraction(segment.startMs, durationMs)
    val endFraction = progressFraction(segment.endMs, durationMs)
    if (endFraction <= startFraction) return@forEach
    val left = (size.width * startFraction).coerceIn(0f, size.width)
    val right = (size.width * endFraction).coerceIn(left, size.width)
    if (right <= left) return@forEach
    drawRoundRect(
      color = BiliColors.AirJumpGreen,
      topLeft = Offset(left, centerY - height / 2f),
      size = Size(right - left, height),
      cornerRadius = CornerRadius(radius, radius),
    )
  }
}

private fun progressFraction(positionMs: Long, durationMs: Long): Float {
  return if (durationMs > 0L) {
    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }
}

private val PlayerControl.iconRes: Int
  @DrawableRes
  get() = when (this) {
    PlayerControl.Episodes -> R.drawable.ic_player_playlist
    PlayerControl.Up -> R.drawable.ic_nav_account
    PlayerControl.Related -> R.drawable.ic_player_related
    PlayerControl.Settings -> R.drawable.ic_nav_settings
  }

private val PlayerControl.labelRes: Int
  get() = when (this) {
    PlayerControl.Episodes -> R.string.player_control_episodes
    PlayerControl.Up -> R.string.player_control_up
    PlayerControl.Related -> R.string.player_control_related
    PlayerControl.Settings -> R.string.player_control_settings
  }

private val PlayerTouchAction.iconRes: Int
  @DrawableRes
  get() = when (this) {
    PlayerTouchAction.Episodes -> R.drawable.ic_player_playlist
    PlayerTouchAction.Up -> R.drawable.ic_nav_account
    PlayerTouchAction.Related -> R.drawable.ic_player_related
    PlayerTouchAction.Comments -> R.drawable.ic_player_subtitles
    PlayerTouchAction.Settings -> R.drawable.ic_nav_settings
  }

private val PlayerTouchAction.labelRes: Int
  @StringRes
  get() = when (this) {
    PlayerTouchAction.Episodes -> R.string.player_control_episodes
    PlayerTouchAction.Up -> R.string.player_control_up
    PlayerTouchAction.Related -> R.string.player_control_related
    PlayerTouchAction.Comments -> R.string.player_control_comments
    PlayerTouchAction.Settings -> R.string.player_control_settings
  }

private val PlayerTouchAction.panel: PlayerPanel
  get() = when (this) {
    PlayerTouchAction.Episodes -> PlayerPanel.Episodes
    PlayerTouchAction.Up -> PlayerPanel.UpVideos
    PlayerTouchAction.Related -> PlayerPanel.RelatedVideos
    PlayerTouchAction.Comments -> PlayerPanel.Comments
    PlayerTouchAction.Settings -> PlayerPanel.Main
  }

private fun PlayerTouchAction.isActive(activePanel: PlayerPanel): Boolean {
  return when (this) {
    PlayerTouchAction.Episodes -> activePanel == PlayerPanel.Episodes
    PlayerTouchAction.Up -> activePanel == PlayerPanel.UpVideos
    PlayerTouchAction.Related -> activePanel == PlayerPanel.RelatedVideos
    PlayerTouchAction.Comments -> activePanel == PlayerPanel.Comments
    PlayerTouchAction.Settings -> activePanel == PlayerPanel.Main ||
      activePanel == PlayerPanel.Quality ||
      activePanel == PlayerPanel.Danmaku ||
      activePanel == PlayerPanel.Speed
  }
}

private val PlayerPanel.titleRes: Int
  get() = when (this) {
    PlayerPanel.Main -> R.string.player_settings_title
    PlayerPanel.Quality -> R.string.player_settings_quality
    PlayerPanel.Danmaku -> R.string.player_settings_danmaku
    PlayerPanel.Speed -> R.string.player_settings_speed
    PlayerPanel.Episodes -> R.string.player_panel_episodes
    PlayerPanel.UpVideos -> R.string.player_panel_up_videos
    PlayerPanel.RelatedVideos -> R.string.player_panel_related_videos
    PlayerPanel.Comments -> R.string.player_panel_comments
    PlayerPanel.None -> R.string.player_settings_title
  }

private fun String.withCodecLabel(codec: String): String {
  return if (codec.isBlank()) this else "$this($codec)"
}

private fun PlaybackEpisode.panelTitle(index: Int): String {
  val pageIndex = page.takeIf { it > 0 } ?: index + 1
  return if (title.isBlank()) "P$pageIndex" else "P$pageIndex $title"
}

private fun VideoSummary.panelPubdateText(): String {
  if (pubdate <= 0L) return ""
  val date = Date(pubdate * 1000L)
  return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

private fun PlayerComment.commentTimeText(): String {
  if (ctime <= 0L) return ""
  val date = Date(ctime * 1000L)
  return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

@Composable
private fun PlaybackRequest.formatPubdate(): String? {
  if (pubdate <= 0L) return null
  val date = Date(pubdate * 1000L)
  return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

@Composable
private fun Int.formatCompactCountText(): String {
  return when {
    this >= 100_000_000 -> stringResource(R.string.player_count_yi, this / 100_000_000.0)
    this >= 10_000 -> stringResource(R.string.player_count_wan, this / 10_000.0)
    else -> toString()
  }
}

@Composable
private fun Float.areaText(): String {
  return stringResource(areaTextRes())
}

@StringRes
private fun Float.areaTextRes(): Int {
  return when {
    this >= 1f -> R.string.player_area_full
    this >= 0.75f -> R.string.player_area_three_quarters
    this >= 0.5f -> R.string.player_area_half
    else -> R.string.player_area_quarter
  }
}

@Composable
private fun Float.speedText(): String {
  return when (this) {
    0.5f -> stringResource(R.string.player_speed_050)
    0.75f -> stringResource(R.string.player_speed_075)
    1.0f -> stringResource(R.string.player_speed_100)
    1.25f -> stringResource(R.string.player_speed_125)
    1.5f -> stringResource(R.string.player_speed_150)
    2.0f -> stringResource(R.string.player_speed_200)
    else -> stringResource(R.string.player_speed_value, this)
  }
}

internal fun Long.toPlayerTime(): String {
  val totalSeconds = (this / 1000L).coerceAtLeast(0L)
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0L) {
    "%d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
  } else {
    "%02d:%02d".format(Locale.US, minutes, seconds)
  }
}

internal val PlayerSpeedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

internal const val UpVideoOrderLatest = "pubdate"
internal const val UpVideoOrderHot = "click"
internal const val UpFocusSort = 0
internal const val UpFocusFollow = 1
internal const val UpPanelHeaderItemCount = 2

private const val UpPanelChipSelectedSurfaceAlpha = 0.16f
private const val UpPanelChipFocusedBorderAlpha = 0.82f
private const val UpPanelChipSelectedBorderAlpha = 0.54f
private const val UpPanelChipRestingBorderAlpha = 0.16f
private const val DanmakuSettingsRowCount = 7
private const val SeekPreviewSpriteScale = 2f
private const val SeekPreviewSpriteMaxWidth = 360f
private const val SeekPreviewSpriteMaxHeight = 220f
