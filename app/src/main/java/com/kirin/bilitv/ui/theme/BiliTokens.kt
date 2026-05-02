package com.kirin.bilitv.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object BiliColors {
  val VideoBlack = Color(0xFF000000)
  val BiliPink = Color(0xFFFB7299)
  val Aqua = Color(0xFF35D0BA)
  val AirJumpGreen = Color(0xFF35D66B)
  val Background = Color(0xFF101014)
  val Surface = Color(0xFF1A1A20)
  val SurfaceElevated = Color(0xFF24242C)
  val SurfaceSelected = Color(0xFF302833)
  val OverlayScrim = Color(0xE6000000)
  val OverlayStrong = Color(0xCC000000)
  val OverlayTransparent = Color(0x00000000)
  val ProgressTrack = Color(0x4DFFFFFF)
  val ProgressBuffered = Color(0x3DFFFFFF)
  val PlayerPanel = Color(0xF21F1F1F)
  val PlayerPanelDivider = Color(0x1AFFFFFF)
  val PlayerPanelFocused = Color(0x4DFB7299)
  val PlayerControlIdle = Color(0x1AFFFFFF)
  val PlayerControlFocused = Color(0xCCFB7299)
  val PlayerFocusGlow = Color(0x99FFFFFF)
  val TextPrimary = Color(0xFFFFFFFF)
  val TextSecondary = Color(0xB3FFFFFF)
  val TextTertiary = Color(0x80FFFFFF)
  val Transparent = Color(0x00000000)
}

object BiliSpacing {
  val Xxs = 2.dp
  val Xs = 4.dp
  val Sm = 8.dp
  val Md = 12.dp
  val Lg = 16.dp
  val Xl = 24.dp
  val Xxl = 32.dp
}

object BiliRadius {
  val Card = 8.dp
  val Panel = 12.dp
  val Pill = 999.dp
}

object BiliSizing {
  val SidebarWidth = 88.dp
  val NavItemHeight = 56.dp
  val NavIconSize = 28.dp
  val AccountAvatarSize = 48.dp
  val AccountAvatarContainerSize = 60.dp
  val AccountProfileAvatarSize = 96.dp
  val AccountProfilePanelWidth = 520.dp
  val AccountProfilePanelHeight = 180.dp
  val AccountVipBadgeSize = 24.dp
  val AccountProfileVipBadgeSize = 34.dp
  val ContentPadding = 16.dp
  val VideoCardWidth = 248.dp
  const val VideoGridColumns = 4
  val VideoGridSpacing = 12.dp
  val VideoGridHorizontalPadding = 0.dp
  val SearchVideoGridHorizontalPadding = 16.dp
  val VideoGridBottomPadding = 72.dp
  val VideoCardMinHeight = 240.dp
  const val VideoThumbnailAspectRatio = 16f / 9f
  val VideoTextHeight = 62.dp
  val TitleMarqueeVelocity = 30.dp
  val VideoCoverGradientHeight = 60.dp
  val VideoBadgeMinHeight = 22.dp
  val VideoProgressBarHeight = 3.dp
  val VideoOverlayIconSize = 14.dp
  val OwnerAvatarSize = 20.dp
  val SettingsRowHeight = 96.dp
  val SettingsChipHeight = 44.dp
  val SettingsCodecValueWidth = 112.dp
  val SettingsHomeSectionGridHeight = 156.dp
  const val SettingsHomeSectionColumns = 4
  val HomeSectionTabHeight = 32.dp
  val HomeSectionTabMinWidth = 72.dp
  val HomeSectionTabCompactMinWidth = 44.dp
  val HomeSectionClockReservedWidth = 176.dp
  val SearchKeyboardPanelWidth = 380.dp
  val SearchInputHeight = 56.dp
  val SearchKeyboardButtonHeight = 48.dp
  val LoginQrContainerSize = 220.dp
  val LoginQrImageSize = 180.dp
  val PlayerControlButtonWidth = 112.dp
  val ClockOverlayTopPadding = 4.dp
  val ClockOverlayEndPadding = 10.dp
  val PlayerOverlayHorizontalPadding = 40.dp
  val PlayerTopPadding = 20.dp
  val PlayerTopTimeEndPadding = 18.dp
  val PlayerTopTimeReservedWidth = 150.dp
  val PlayerTopGradientHeight = 132.dp
  val PlayerBottomGradientHeight = 196.dp
  val PlayerBottomPadding = 25.dp
  val PlayerProgressTouchHeight = 40.dp
  val PlayerProgressHeight = 4.dp
  val PlayerProgressFocusedHeight = 6.dp
  val PlayerProgressKnobSize = 16.dp
  val PlayerProgressFocusedKnobSize = 20.dp
  val PlayerMiniProgressHeight = 3.dp
  val PlayerControlIconButtonSize = 60.dp
  val PlayerControlIconSize = 36.dp
  val PlayerStatusMinWidth = 84.dp
  val PlayerSettingsPanelWidth = 350.dp
  val PlayerContentPanelWidth = 400.dp
  val PlayerSettingsHeaderHeight = 72.dp
  val PlayerUpPanelHeaderHeight = 80.dp
  val PlayerSettingsRowHeight = 76.dp
  val PlayerEpisodeRowHeight = 54.dp
  val PlayerEpisodeAccentWidth = 4.dp
  val PlayerPanelVideoRowHeight = 88.dp
  val PlayerPanelVideoThumbnailWidth = 140.dp
  val PlayerPanelVideoThumbnailHeight = 80.dp
  val PlayerPanelAvatarSize = 48.dp
  val PlayerPanelChipHeight = 36.dp
  val PlayerUnfollowDialogWidth = 420.dp
  val PlayerUnfollowDialogButtonHeight = 44.dp
  val PlayerSettingsDividerHeight = 1.dp
  val PlayerSettingsIconSize = 24.dp
  val PlayerSettingsChevronSize = 20.dp
  val PlayerSeekPreviewWidth = 240.dp
  val PlayerSeekPreviewHeight = 96.dp
  val PlayerPauseIndicatorSize = 96.dp
}

object BiliTypography {
  val ScreenTitle = 32.sp
  val SectionTitle = 18.sp
  val Body = 18.sp
  val BodySmall = 15.sp
  val Nav = 17.sp
  val AccountVipBadge = 13.sp
  val AccountVipBadgeLineHeight = 18.sp
  val AccountProfileVipBadge = 22.sp
  val AccountProfileVipBadgeLineHeight = 22.sp
  val SearchInput = 22.sp
  val HomeSectionTab = 15.sp
  val HomeSectionTabLineHeight = 18.sp
  val CardTitle = 14.sp
  val CardTitleLineHeight = 19.sp
  val CardMeta = 12.sp
  val CardMetaLineHeight = 16.sp
  val CardOverlay = 12.sp
  val CardOverlayLineHeight = 16.sp
  val CardBadge = 11.sp
  val CardBadgeLineHeight = 15.sp
  val PlayerTitle = 22.sp
  val PlayerMeta = 18.sp
  val PlayerTime = 22.sp
  val PlayerStatus = 14.sp
  val PlayerPanelTitle = 20.sp
  val PlayerSettingTitle = 15.sp
  val PlayerSettingValue = 13.sp
  val PlayerSeekPreview = 28.sp
}

object BiliMotion {
  const val FocusMs = 220
  const val FocusOutMs = 150
  const val FocusScrollDelayMs = 0L
  const val FocusScrollMinDeltaPx = 50
  const val TitleMarqueeInitialDelayMs = 500
  const val TitleMarqueeRepeatDelayMs = 2_000
  const val PanelMs = 180
  const val OverlayMs = 160
  const val FocusSpringDampingRatio = 0.72f
  const val FocusSpringStiffness = 320f
  const val PlayerControlsAutoHideMs = 4_000L
  const val PlayerProgressUpdateMs = 500L
  const val PlayerSeekPreviewAutoCommitMs = 1_200L
  const val PlayerClockUpdateMs = 30_000L
  val FocusEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}

object BiliFocus {
  const val CardScale = 1.08f
  const val CoverHighlightAlpha = 0.10f
  const val FocusedCoverBlurAlpha = 0.16f
  const val FocusedZIndex = 1f
  const val ShadowAlpha = 0.42f
  val BorderWidth = 3.dp
  val RestingBorderWidth = 1.dp
  val RestingShadowElevation = 0.dp
  val ShadowElevation = 18.dp
  val FocusedCoverBlurRadius = 8.dp
  val ScrollInset = 20.dp
  val FocusedRowTopPadding = 56.dp
}

object BiliSkeleton {
  const val StandardItemCount = 12
  const val LowSpecItemCount = 8
  const val TitleLongWidthFraction = 0.92f
  const val TitleShortWidthFraction = 0.68f
  const val MetaWidthFraction = 0.46f
}
