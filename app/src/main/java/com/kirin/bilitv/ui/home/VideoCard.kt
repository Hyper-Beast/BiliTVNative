package com.kirin.bilitv.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.kirin.bilitv.R
import com.kirin.bilitv.core.image.buildOwnerAvatarRequest
import com.kirin.bilitv.core.image.buildVideoThumbnailRequest
import com.kirin.bilitv.core.model.VideoCardRelativeText
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.model.durationText
import com.kirin.bilitv.core.model.isWatchCompleted
import com.kirin.bilitv.core.model.pubdateText
import com.kirin.bilitv.core.model.viewAtText
import com.kirin.bilitv.core.model.watchProgressRatio
import com.kirin.bilitv.core.model.watchProgressText
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.i18n.convertChineseText
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography

enum class VideoCardMode {
  Standard,
  Dynamic,
  History,
}

@Composable
fun VideoCard(
  video: VideoSummary,
  modifier: Modifier = Modifier,
  mode: VideoCardMode = VideoCardMode.Standard,
  onClick: () -> Unit = {},
  onFocused: () -> Unit = {},
) {
  var focused by remember { mutableStateOf(false) }
  val performancePolicy = LocalBiliPerformancePolicy.current
  val focusEffectsEnabled = performancePolicy.motionEnabled
  val title = convertChineseText(video.title)
  val titleColor = if (focusEffectsEnabled) {
    animateColorAsState(
      targetValue = if (focused) BiliColors.TextPrimary else BiliColors.TextSecondary,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCardTitleColor",
    ).value
  } else {
    if (focused) BiliColors.TextPrimary else BiliColors.TextSecondary
  }

  BiliFocusableSurface(
    modifier = modifier.fillMaxWidth(),
    scaleOnFocus = focusEffectsEnabled,
    shadowOnFocus = true,
    onClick = onClick,
    onFocused = onFocused,
    onFocusChanged = { focused = it },
  ) {
    Column {
      VideoCover(
        video = video,
        mode = mode,
        focused = focused,
        focusEffectsEnabled = focusEffectsEnabled,
      )
      Column(
        modifier = Modifier
          .height(BiliSizing.VideoTextHeight)
          .padding(horizontal = BiliSpacing.Sm, vertical = BiliSpacing.Xs),
      ) {
        Text(
          text = title,
          color = titleColor,
          fontSize = BiliTypography.CardTitle,
          lineHeight = BiliTypography.CardTitleLineHeight,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .then(
              if (focused && focusEffectsEnabled) {
                Modifier.basicMarquee(
                  iterations = Int.MAX_VALUE,
                  repeatDelayMillis = BiliMotion.TitleMarqueeRepeatDelayMs,
                  initialDelayMillis = BiliMotion.TitleMarqueeInitialDelayMs,
                  velocity = BiliSizing.TitleMarqueeVelocity,
                )
              } else {
                Modifier
              },
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        MetadataRow(
          video = video,
          mode = mode,
          focused = focused,
          focusEffectsEnabled = focusEffectsEnabled,
        )
      }
    }
  }
}

@Composable
private fun MetadataRow(
  video: VideoSummary,
  mode: VideoCardMode,
  focused: Boolean,
  focusEffectsEnabled: Boolean,
) {
  val ownerName = convertChineseText(video.ownerName)
  val relativeText = rememberVideoCardRelativeText()
  val trailingText = when (mode) {
    VideoCardMode.Standard,
    VideoCardMode.Dynamic -> video.pubdateText(relativeText)
    VideoCardMode.History -> video.viewAtText(relativeText)
  }
  val ownerColor = if (focusEffectsEnabled) {
    animateColorAsState(
      targetValue = if (focused) BiliColors.TextPrimary else BiliColors.TextSecondary,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCardOwnerColor",
    ).value
  } else {
    BiliColors.TextSecondary
  }
  val trailingColor = if (focusEffectsEnabled) {
    animateColorAsState(
      targetValue = if (focused) BiliColors.TextSecondary else BiliColors.TextTertiary,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCardTrailingColor",
    ).value
  } else {
    BiliColors.TextTertiary
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (video.ownerName.isNotBlank()) {
      OwnerAvatar(video)
      Spacer(modifier = Modifier.width(BiliSpacing.Sm))
      Text(
        text = ownerName,
        color = ownerColor,
        fontSize = BiliTypography.CardMeta,
        lineHeight = BiliTypography.CardMetaLineHeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    } else {
      Spacer(modifier = Modifier.weight(1f))
    }

    if (trailingText.isNotBlank()) {
      Spacer(modifier = Modifier.width(BiliSpacing.Sm))
      Text(
        text = trailingText,
        color = trailingColor,
        fontSize = BiliTypography.CardMeta,
        lineHeight = BiliTypography.CardMetaLineHeight,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun OwnerAvatar(video: VideoSummary) {
  val performancePolicy = LocalBiliPerformancePolicy.current
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val modifier = Modifier
    .size(BiliSizing.OwnerAvatarSize)
    .clip(CircleShape)
    .background(BiliColors.SurfaceElevated)

  if (video.ownerFace.isBlank()) {
    Box(modifier = modifier)
    return
  }

  val context = LocalContext.current
  val request = remember(
    context,
    video.ownerFace,
    performancePolicy.ownerAvatarSizePx,
    performancePolicy.ownerAvatarRgb565Enabled,
    performancePolicy.imageMemoryCacheEnabled,
  ) {
    buildOwnerAvatarRequest(
      context = context,
      url = video.ownerFace,
      sizePx = performancePolicy.ownerAvatarSizePx,
      allowRgb565 = performancePolicy.ownerAvatarRgb565Enabled,
      memoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled,
    )
  }

  AsyncImage(
    model = request,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    placeholder = fallbackPainter,
    error = fallbackPainter,
    modifier = modifier,
  )
}

@Composable
private fun VideoCover(
  video: VideoSummary,
  mode: VideoCardMode,
  focused: Boolean,
  focusEffectsEnabled: Boolean,
) {
  val context = LocalContext.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val request = remember(
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
  val fallbackPainter = ColorPainter(BiliColors.SurfaceElevated)
  val title = convertChineseText(video.title)
  val badge = convertChineseText(video.badge)
  val highlightAlpha = if (focusEffectsEnabled) {
    animateFloatAsState(
      targetValue = if (focused) BiliFocus.CoverHighlightAlpha else 0f,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCoverHighlight",
    ).value
  } else {
    0f
  }
  val blurAlpha = if (focusEffectsEnabled && performancePolicy.focusedCoverBlurEnabled) {
    animateFloatAsState(
      targetValue = if (focused) BiliFocus.FocusedCoverBlurAlpha else 0f,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "videoCoverBlur",
    ).value
  } else {
    0f
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(BiliSizing.VideoThumbnailAspectRatio)
      .clip(RoundedCornerShape(BiliRadius.Card))
      .background(BiliColors.SurfaceElevated),
  ) {
    AsyncImage(
      model = request,
      contentDescription = title,
      contentScale = ContentScale.Crop,
      placeholder = fallbackPainter,
      error = fallbackPainter,
      modifier = Modifier.fillMaxSize(),
    )

    if (blurAlpha > 0f) {
      AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        modifier = Modifier
          .fillMaxSize()
          .blur(BiliFocus.FocusedCoverBlurRadius)
          .graphicsLayer { alpha = blurAlpha },
      )
    }

    if (highlightAlpha > 0f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.White.copy(alpha = highlightAlpha)),
      )
    }

    if (badge.isNotBlank()) {
      VideoBadge(
        text = badge,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(BiliSpacing.Xs),
      )
    }

    BottomScrim(modifier = Modifier.align(Alignment.BottomCenter))

    when (mode) {
      VideoCardMode.Standard,
      VideoCardMode.Dynamic -> StandardCoverMetadata(
        video = video,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = BiliSpacing.Sm, vertical = BiliSpacing.Sm),
      )
      VideoCardMode.History -> HistoryCoverMetadata(
        video = video,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}

@Composable
private fun VideoBadge(text: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .height(BiliSizing.VideoBadgeMinHeight)
      .clip(RoundedCornerShape(BiliRadius.Card))
      .background(BiliColors.BiliPink)
      .padding(horizontal = BiliSpacing.Sm),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.CardBadge,
      lineHeight = BiliTypography.CardBadgeLineHeight,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun BottomScrim(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
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
}

@Composable
private fun StandardCoverMetadata(video: VideoSummary, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      VideoMetric(
        iconRes = R.drawable.ic_video_play_count,
        contentDescription = stringResource(R.string.video_play_count_content_description),
        text = video.view.formatCompactCountText(),
      )
      if (video.danmaku > 0) {
        Spacer(modifier = Modifier.width(BiliSpacing.Sm))
        VideoMetric(
          iconRes = R.drawable.ic_video_danmaku_count,
          contentDescription = stringResource(R.string.video_danmaku_count_content_description),
          text = video.danmaku.formatCompactCountText(),
        )
      }
    }
    Text(
      text = video.durationText(),
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.CardOverlay,
      lineHeight = BiliTypography.CardOverlayLineHeight,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun VideoMetric(
  iconRes: Int,
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
      lineHeight = BiliTypography.CardOverlayLineHeight,
      maxLines = 1,
    )
  }
}

@Composable
private fun HistoryCoverMetadata(video: VideoSummary, modifier: Modifier = Modifier) {
  val completedText = stringResource(R.string.video_watch_completed)
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = BiliSpacing.Sm),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val pageText = if (video.historyVideos > 1 && video.historyPage > 0) {
        "P${video.historyPage}"
      } else {
        ""
      }

      if (pageText.isNotBlank()) {
        HistoryOverlayBadge(
          text = pageText,
        )
        Spacer(modifier = Modifier.weight(1f))
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }

      if (!video.isLive) {
        if (video.isWatchCompleted()) {
          HistoryOverlayBadge(text = completedText)
        } else {
          Text(
            text = video.watchProgressText(completedText),
            color = BiliColors.TextPrimary,
            fontSize = BiliTypography.CardOverlay,
            lineHeight = BiliTypography.CardOverlayLineHeight,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(BiliSpacing.Xs))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(BiliSizing.VideoProgressBarHeight)
        .background(BiliColors.ProgressTrack),
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(video.watchProgressRatio())
          .background(BiliColors.BiliPink),
      )
    }
  }
}

@Composable
private fun HistoryOverlayBadge(text: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .height(BiliSizing.VideoBadgeMinHeight)
      .clip(RoundedCornerShape(BiliRadius.Card))
      .background(BiliColors.BiliPink)
      .padding(horizontal = BiliSpacing.Sm),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = BiliColors.TextPrimary,
      fontSize = BiliTypography.CardBadge,
      lineHeight = BiliTypography.CardBadgeLineHeight,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

@Composable
private fun rememberVideoCardRelativeText(): VideoCardRelativeText {
  val viewedSuffixFormat = stringResource(R.string.video_viewed_suffix)
  val minutesAgoFormat = stringResource(R.string.video_relative_minutes_ago)
  val hoursAgoFormat = stringResource(R.string.video_relative_hours_ago)
  val yesterday = stringResource(R.string.video_relative_yesterday)
  val daysAgoFormat = stringResource(R.string.video_relative_days_ago)
  return remember(
    viewedSuffixFormat,
    minutesAgoFormat,
    hoursAgoFormat,
    yesterday,
    daysAgoFormat,
  ) {
    VideoCardRelativeText(
      viewedSuffixFormat = viewedSuffixFormat,
      minutesAgoFormat = minutesAgoFormat,
      hoursAgoFormat = hoursAgoFormat,
      yesterday = yesterday,
      daysAgoFormat = daysAgoFormat,
    )
  }
}

@Composable
private fun Int.formatCompactCountText(): String {
  return when {
    this >= 100_000_000 -> stringResource(R.string.video_count_yi, this / 100_000_000.0)
    this >= 10_000 -> stringResource(R.string.video_count_wan, this / 10_000.0)
    else -> toString()
  }
}
