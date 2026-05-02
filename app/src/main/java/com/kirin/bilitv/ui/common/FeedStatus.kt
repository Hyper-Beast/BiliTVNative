package com.kirin.bilitv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSkeleton
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography

@Composable
fun FeedStatusScreen(
  message: String,
  actionLabel: String? = null,
  onAction: () -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = message,
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.Body,
    )
    if (actionLabel != null) {
      BiliFocusableSurface(
        scaleOnFocus = false,
        onClick = onAction,
        modifier = Modifier.padding(top = BiliSpacing.Lg),
      ) {
        Text(
          text = actionLabel,
          color = BiliColors.TextPrimary,
          fontSize = BiliTypography.BodySmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = BiliSpacing.Lg, vertical = BiliSpacing.Md),
        )
      }
    }
  }
}

@Composable
fun VideoGridSkeleton(
  modifier: Modifier = Modifier,
  count: Int? = null,
) {
  val performancePolicy = LocalBiliPerformancePolicy.current
  val itemCount = count ?: if (performancePolicy.lowSpecMode) {
    BiliSkeleton.LowSpecItemCount
  } else {
    BiliSkeleton.StandardItemCount
  }

  LazyVerticalGrid(
    columns = GridCells.Fixed(BiliSizing.VideoGridColumns),
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = BiliSizing.VideoGridHorizontalPadding,
      top = BiliFocus.ScrollInset,
      end = BiliSizing.VideoGridHorizontalPadding,
      bottom = BiliSizing.VideoGridBottomPadding,
    ),
    horizontalArrangement = Arrangement.spacedBy(BiliSizing.VideoGridSpacing),
    verticalArrangement = Arrangement.spacedBy(BiliSizing.VideoGridSpacing),
    userScrollEnabled = false,
  ) {
    items((0 until itemCount).toList()) {
      SkeletonVideoCard()
    }
  }
}

@Composable
private fun SkeletonVideoCard() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(BiliRadius.Card))
      .background(BiliColors.Surface),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(BiliSizing.VideoThumbnailAspectRatio)
        .clip(RoundedCornerShape(BiliRadius.Card))
        .background(BiliColors.SurfaceElevated),
    )
    Column(
      modifier = Modifier
        .height(BiliSizing.VideoTextHeight)
        .padding(horizontal = BiliSpacing.Sm, vertical = BiliSpacing.Xs),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
      ) {
        SkeletonLine(widthFraction = BiliSkeleton.TitleLongWidthFraction)
        SkeletonLine(widthFraction = BiliSkeleton.TitleShortWidthFraction)
      }
      SkeletonLine(widthFraction = BiliSkeleton.MetaWidthFraction)
    }
  }
}

@Composable
private fun SkeletonLine(widthFraction: Float) {
  Spacer(
    modifier = Modifier
      .fillMaxWidth(widthFraction)
      .height(BiliSpacing.Md)
      .clip(RoundedCornerShape(BiliRadius.Pill))
      .background(BiliColors.SurfaceElevated),
  )
}
