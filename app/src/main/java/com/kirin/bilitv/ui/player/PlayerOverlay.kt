package com.kirin.bilitv.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.kirin.bilitv.core.player.VideoshotData
import com.kirin.bilitv.core.player.VideoshotFrame
import com.kirin.bilitv.ui.common.ClockOverlay
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
}

@Composable
internal fun BoxScope.PlayerOverlay(
  request: PlaybackRequest,
  info: PlaybackInfo,
  metadata: PlaybackVideoMetadata?,
  sidePanelVideos: List<VideoSummary>,
  sidePanelLoading: Boolean,
  upVideoOrder: String,
  upFollowed: Boolean,
  upFollowLoading: Boolean,
  playbackPaused: Boolean,
  seekPreviewSpritesEnabled: Boolean,
  videoshotData: VideoshotData?,
  videoshotSprites: Map<String, ImageBitmap>,
  onlineCountText: String,
  currentCodecText: String,
  showUnfollowConfirm: Boolean,
  unfollowConfirmFocusedConfirm: Boolean,
  controlsVisible: Boolean,
  focusedControl: PlayerControl,
  progressFocused: Boolean,
  activePanel: PlayerPanel,
  focusedPanelIndex: Int,
  playbackSpeed: Float,
  danmakuSettings: DanmakuSettings,
  positionMs: Long,
  durationMs: Long,
  bufferedPercentage: Long,
  airJumpSegments: List<AirJumpSegment>,
  previewPositionMs: Long?,
  showClock: Boolean,
) {
  if (controlsVisible) {
    PlayerTopOverlay(
      request = request,
      title = info.title,
      showClock = showClock,
      modifier = Modifier.align(Alignment.TopCenter),
    )
    PlayerBottomOverlay(
      request = request,
      info = info,
      focusedControl = focusedControl,
      progressFocused = progressFocused,
      positionMs = positionMs,
      durationMs = durationMs,
      bufferedPercentage = bufferedPercentage,
      airJumpSegments = airJumpSegments,
      previewPositionMs = previewPositionMs,
      danmakuSettings = danmakuSettings,
      onlineCountText = onlineCountText,
      currentCodecText = currentCodecText,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  } else {
    if (showClock) {
      ClockOverlay(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(
            top = BiliSizing.ClockOverlayTopPadding,
            end = BiliSizing.ClockOverlayEndPadding,
          ),
      )
    }
    MiniProgressBar(
      positionMs = positionMs,
      durationMs = durationMs,
      airJumpSegments = airJumpSegments,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }

  if (previewPositionMs != null) {
    SeekPreviewOverlay(
      previewPositionMs = previewPositionMs,
      durationMs = durationMs,
      videoshotData = if (seekPreviewSpritesEnabled) videoshotData else null,
      videoshotSprites = videoshotSprites,
      modifier = Modifier.align(Alignment.Center),
    )
  } else if (playbackPaused) {
    PauseIndicatorOverlay(modifier = Modifier.align(Alignment.Center))
  }

  if (activePanel != PlayerPanel.None) {
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
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.Episodes -> PlayerEpisodesPanel(
        episodes = metadata?.pages.orEmpty(),
        currentCid = request.cid,
        focusedIndex = focusedPanelIndex,
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
        modifier = Modifier.align(Alignment.CenterEnd),
      )
      PlayerPanel.None -> Unit
    }
  }

  if (showUnfollowConfirm) {
    UnfollowConfirmDialog(
      focusedConfirm = unfollowConfirmFocusedConfirm,
      modifier = Modifier.align(Alignment.Center),
    )
  }
}

@Composable
private fun PauseIndicatorOverlay(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .size(BiliSizing.PlayerPauseIndicatorSize)
      .clip(CircleShape)
      .background(BiliColors.OverlayStrong),
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
  positionMs: Long,
  durationMs: Long,
  bufferedPercentage: Long,
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
        positionMs = positionMs,
        durationMs = durationMs,
        bufferedPercentage = bufferedPercentage,
        airJumpSegments = airJumpSegments,
        isFocused = progressFocused,
        previewPositionMs = previewPositionMs,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(BiliSpacing.Xl))
      Text(
        text = "${(previewPositionMs ?: positionMs).toPlayerTime()} / ${durationMs.toPlayerTime()}",
        color = BiliColors.TextPrimary,
        fontSize = BiliTypography.PlayerTime,
        fontWeight = FontWeight.Bold,
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
  Box(
    modifier = Modifier
      .size(BiliSizing.PlayerControlIconButtonSize)
      .clip(RoundedCornerShape(BiliRadius.Card))
      .background(if (focused) BiliColors.PlayerControlFocused else BiliColors.PlayerControlIdle),
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
private fun TvProgressBar(
  positionMs: Long,
  durationMs: Long,
  bufferedPercentage: Long,
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
  positionMs: Long,
  durationMs: Long,
  airJumpSegments: List<AirJumpSegment>,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerMiniProgressHeight),
  ) {
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
    }
    return
  }

  Column(
    modifier = modifier
      .width(BiliSizing.PlayerSeekPreviewWidth)
      .height(BiliSizing.PlayerSeekPreviewHeight)
      .clip(RoundedCornerShape(BiliRadius.Panel))
      .background(BiliColors.OverlayStrong)
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
  }
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
    val srcX = frame.x.coerceIn(0, (image.width - 1).coerceAtLeast(0))
    val srcY = frame.y.coerceIn(0, (image.height - 1).coerceAtLeast(0))
    val srcWidth = frame.width.coerceAtMost(image.width - srcX).coerceAtLeast(1)
    val srcHeight = frame.height.coerceAtMost(image.height - srcY).coerceAtLeast(1)
    drawImage(
      image = image,
      srcOffset = IntOffset(srcX, srcY),
      srcSize = IntSize(srcWidth, srcHeight),
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
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val performancePolicy = LocalBiliPerformancePolicy.current
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
      .background(BiliColors.PlayerPanel),
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
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val performancePolicy = LocalBiliPerformancePolicy.current
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
      .background(BiliColors.PlayerPanel),
  ) {
    if (showUploaderHeader) {
      UploaderPanelHeader(
        request = request,
        order = upVideoOrder,
        followed = upFollowed,
        followLoading = upFollowLoading,
        focusedIndex = focusedIndex,
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
    )
  }
}

@Composable
private fun UpPanelChip(
  text: String,
  focused: Boolean,
  selected: Boolean,
) {
  val shape = RoundedCornerShape(BiliRadius.Pill)
  Box(
    modifier = Modifier
      .height(BiliSizing.PlayerPanelChipHeight)
      .clip(shape)
      .background(
        when {
          selected -> BiliColors.BiliPink
          focused -> BiliColors.PlayerPanelFocused
          else -> BiliColors.PlayerControlIdle
        },
      )
      .then(
        if (focused) {
          Modifier.border(BorderStroke(BiliFocus.RestingBorderWidth, BiliColors.TextPrimary), shape)
        } else {
          Modifier
        },
      )
      .padding(horizontal = BiliSpacing.Md),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.PlayerSettingValue,
      fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun UnfollowConfirmDialog(
  focusedConfirm: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .width(BiliSizing.PlayerUnfollowDialogWidth)
      .clip(RoundedCornerShape(BiliRadius.Panel))
      .background(BiliColors.OverlayScrim)
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
        modifier = Modifier.weight(1f),
      )
      ConfirmDialogButton(
        text = stringResource(R.string.player_unfollow_confirm_action),
        focused = focusedConfirm,
        destructive = true,
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
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(BiliRadius.Card)
  Box(
    modifier = modifier
      .height(BiliSizing.PlayerUnfollowDialogButtonHeight)
      .clip(shape)
      .background(
        when {
          focused && destructive -> BiliColors.BiliPink
          focused -> BiliColors.PlayerPanelFocused
          else -> BiliColors.PlayerControlIdle
        },
      )
      .then(
        if (focused) {
          Modifier.border(BorderStroke(BiliFocus.RestingBorderWidth, BiliColors.TextPrimary), shape)
        } else {
          Modifier
        },
      )
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
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerEpisodeRowHeight)
      .background(if (focused) BiliColors.PlayerPanelFocused else BiliColors.Transparent),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .width(BiliSizing.PlayerEpisodeAccentWidth)
        .fillMaxHeight()
        .background(
          when {
            selected -> BiliColors.BiliPink
            focused -> BiliColors.PlayerFocusGlow
            else -> BiliColors.Transparent
          },
        ),
    )
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
      .padding(horizontal = BiliSpacing.Sm, vertical = BiliSpacing.Xs)
      .height(BiliSizing.PlayerPanelVideoRowHeight)
      .clip(shape)
      .background(if (focused) BiliColors.PlayerPanelFocused else BiliColors.Transparent)
      .then(
        if (focused) {
          Modifier.border(BorderStroke(BiliFocus.RestingBorderWidth, BiliColors.TextPrimary), shape)
        } else {
          Modifier
        },
      )
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
      if (viewText.isNotBlank() || danmakuText.isNotBlank()) {
        Row(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .background(BiliColors.OverlayStrong, RoundedCornerShape(BiliRadius.Card))
            .padding(horizontal = BiliSpacing.Xs, vertical = BiliSpacing.Xs),
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
      }
      if (video.duration > 0) {
        Text(
          text = (video.duration.toLong() * 1000L).toPlayerTime(),
          color = BiliColors.TextPrimary,
          fontSize = BiliTypography.CardOverlay,
          maxLines = 1,
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .background(BiliColors.OverlayStrong, RoundedCornerShape(BiliRadius.Card))
            .padding(horizontal = BiliSpacing.Xs, vertical = BiliSpacing.Xs),
        )
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
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .width(BiliSizing.PlayerSettingsPanelWidth)
      .fillMaxHeight()
      .background(BiliColors.PlayerPanel),
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
    Column(modifier = Modifier.fillMaxWidth()) {
      when (activePanel) {
        PlayerPanel.Main -> MainSettingsRows(
          focusedIndex = focusedIndex,
          quality = info.selectedQuality.description.withCodecLabel(currentCodecText),
          playbackSpeed = playbackSpeed,
          danmakuSettings = danmakuSettings,
        )
        PlayerPanel.Quality -> QualityRows(
          focusedIndex = focusedIndex,
          qualities = info.qualities,
          currentQuality = info.selectedQuality,
        )
        PlayerPanel.Danmaku -> DanmakuRows(
          focusedIndex = focusedIndex,
          settings = danmakuSettings,
        )
        PlayerPanel.Speed -> SpeedRows(
          focusedIndex = focusedIndex,
          playbackSpeed = playbackSpeed,
        )
        PlayerPanel.Episodes,
        PlayerPanel.UpVideos,
        PlayerPanel.RelatedVideos,
        PlayerPanel.None -> Unit
      }
    }
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

@Composable
private fun DanmakuRows(
  focusedIndex: Int,
  settings: DanmakuSettings,
) {
  val rows = listOf(
    DanmakuSettingRow(R.string.player_settings_danmaku_toggle, R.drawable.ic_player_subtitles, if (settings.enabled) stringResource(R.string.player_value_on) else stringResource(R.string.player_value_off)),
    DanmakuSettingRow(R.string.player_settings_danmaku_opacity, R.drawable.ic_player_subtitles, "%.1f".format(Locale.US, settings.opacity), adjustable = true),
    DanmakuSettingRow(R.string.player_settings_danmaku_font_size, R.drawable.ic_player_subtitles, settings.fontSize.toString(), adjustable = true),
    DanmakuSettingRow(R.string.player_settings_danmaku_area, R.drawable.ic_player_subtitles, settings.area.areaText(), adjustable = true),
    DanmakuSettingRow(R.string.player_settings_danmaku_speed, R.drawable.ic_player_speed, settings.speed.toString(), adjustable = true),
    DanmakuSettingRow(R.string.player_settings_danmaku_top, R.drawable.ic_player_subtitles, if (settings.allowTop) stringResource(R.string.player_value_on) else stringResource(R.string.player_value_off)),
    DanmakuSettingRow(R.string.player_settings_danmaku_bottom, R.drawable.ic_player_subtitles, if (settings.allowBottom) stringResource(R.string.player_value_on) else stringResource(R.string.player_value_off)),
  )
  rows.forEachIndexed { index, row ->
    SettingsRow(
      iconRes = row.iconRes,
      title = stringResource(row.titleRes),
      value = row.value,
      focused = focusedIndex == index,
      adjustable = row.adjustable,
    )
  }
}

private data class DanmakuSettingRow(
  val titleRes: Int,
  @param:DrawableRes val iconRes: Int,
  val value: String,
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
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(BiliSizing.PlayerSettingsRowHeight)
      .background(if (focused) BiliColors.PlayerPanelFocused else BiliColors.Transparent)
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
          modifier = Modifier.size(BiliSizing.PlayerSettingsChevronSize),
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
          modifier = Modifier.size(BiliSizing.PlayerSettingsChevronSize),
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

private val PlayerPanel.titleRes: Int
  get() = when (this) {
    PlayerPanel.Main -> R.string.player_settings_title
    PlayerPanel.Quality -> R.string.player_settings_quality
    PlayerPanel.Danmaku -> R.string.player_settings_danmaku
    PlayerPanel.Speed -> R.string.player_settings_speed
    PlayerPanel.Episodes -> R.string.player_panel_episodes
    PlayerPanel.UpVideos -> R.string.player_panel_up_videos
    PlayerPanel.RelatedVideos -> R.string.player_panel_related_videos
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
  return when {
    this >= 1f -> stringResource(R.string.player_area_full)
    this >= 0.75f -> stringResource(R.string.player_area_three_quarters)
    this >= 0.5f -> stringResource(R.string.player_area_half)
    else -> stringResource(R.string.player_area_quarter)
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

private const val SeekPreviewSpriteScale = 2f
private const val SeekPreviewSpriteMaxWidth = 360f
private const val SeekPreviewSpriteMaxHeight = 220f
