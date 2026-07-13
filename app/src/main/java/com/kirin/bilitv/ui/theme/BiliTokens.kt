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

object BiliHomeThemeColors {
  val PinkBackgroundTop = Color(0xFF120D13)
  val PinkBackgroundBottom = Color(0xFF1A1119)
  val PinkAmbientA = Color(0x3AFB7299)
  val PinkAmbientB = Color(0x2E8F4D68)
  val PinkGlassSurface = Color(0x66211A23)
  val PinkGlassSurfaceStrong = Color(0xAA2A202A)
  val PinkSidebarSurface = Color(0xB8582940)
  val PinkGlassBorder = Color(0x22FFFFFF)
  val PinkCardSurface = Color(0x661D1720)
  val PinkCardInfoSurface = Color(0xB81B151D)
  val PinkCardFocusedSurface = Color(0xCC2C222D)

  val BlackBackgroundTop = Color(0xFF040608)
  val BlackBackgroundBottom = Color(0xFF080B0F)
  val BlackAmbientA = Color(0x202A2D32)
  val BlackAmbientB = Color(0x1A1A1D22)
  val BlackGlassSurface = Color(0x660B0D11)
  val BlackGlassSurfaceStrong = Color(0xB8121418)
  val BlackSidebarSurface = Color(0xD00A0D11)
  val BlackGlassBorder = Color(0x26FFFFFF)
  val BlackCardSurface = Color(0x700D1014)
  val BlackCardInfoSurface = Color(0xC00B0D11)
  val BlackCardFocusedSurface = Color(0xD1181B20)
  val BlackTextPrimary = Color(0xFFF7F7FA)
  val BlackTextSecondary = Color(0xB8FFFFFF)
  val BlackTextTertiary = Color(0x75FFFFFF)
  val BlackShine = Color(0xFFE2E8F0)

  val GrayAccent = Color(0xFFFF8CAD)
  val GrayBackgroundTop = Color(0xFF151C22)
  val GrayBackgroundBottom = Color(0xFF22262B)
  val GrayAmbientA = Color(0x374C6374)
  val GrayAmbientB = Color(0x2A3F5060)
  val GrayGlassSurface = Color(0x5C58636D)
  val GrayGlassSurfaceStrong = Color(0x8A2E3740)
  val GraySidebarSurface = Color(0xA84B5661)
  val GrayGlassBorder = Color(0x4AFFFFFF)
  val GrayCardSurface = Color(0x50313A43)
  val GrayCardInfoSurface = Color(0x98212831)
  val GrayCardFocusedSurface = Color(0xC23A4650)
  val GrayTextPrimary = Color(0xFFF7FAFC)
  val GrayTextSecondary = Color(0xCFE7EDF2)
  val GrayTextTertiary = Color(0x8CCFD8DF)
  val GrayShine = Color(0xFF7E8994)

  val BlueGrayAccent = Color(0xFF8DC7FF)
  val BlueGrayBackgroundTop = Color(0xFF0C1722)
  val BlueGrayBackgroundBottom = Color(0xFF111C26)
  val BlueGrayAmbientA = Color(0x335DA7E8)
  val BlueGrayAmbientB = Color(0x2635D0BA)
  val BlueGrayGlassSurface = Color(0x66202D38)
  val BlueGrayGlassSurfaceStrong = Color(0xA8192631)
  val BlueGraySidebarSurface = Color(0xB827526C)
  val BlueGrayGlassBorder = Color(0x2AFFFFFF)
  val BlueGrayCardSurface = Color(0x6419232D)
  val BlueGrayCardInfoSurface = Color(0xBA121B24)
  val BlueGrayCardFocusedSurface = Color(0xD1253440)
  val BlueGrayTextPrimary = Color(0xFFF5FAFF)
  val BlueGrayTextSecondary = Color(0xC7D9E8F4)
  val BlueGrayTextTertiary = Color(0x82C1D3E0)
  val BlueGrayShine = Color(0xFF5DA7E8)
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
  val None = 0.dp
  val Card = 8.dp
  val Panel = 12.dp
  val Pill = 999.dp
  val Sidebar = 30.dp
}

object BiliSizing {
  val SidebarWidth = 76.dp
  val NavItemHeight = 48.dp
  val NavIconSize = 24.dp
  val SidebarNavGroupTopPadding = 58.dp
  val SidebarNavGroupSpacing = 26.dp
  val AccountAvatarSize = 40.dp
  val AccountAvatarContainerSize = 50.dp
  val AccountProfileAvatarSize = 96.dp
  val AccountProfilePanelWidth = 520.dp
  val AccountProfilePanelHeight = 180.dp
  val AccountVipBadgeSize = 20.dp
  val AccountProfileVipBadgeSize = 34.dp
  val ContentPadding = 16.dp
  val VideoCardWidth = 248.dp
  const val VideoGridColumns = 4
  val VideoGridSpacing = 10.dp
  val VideoGridHorizontalPadding = 0.dp
  val HomeVideoGridTopPadding = 24.dp
  val HomeVideoGridTopBleed = 16.dp
  val SearchVideoGridHorizontalPadding = 16.dp
  val TouchPullRefreshTriggerDistance = 72.dp
  val TouchPullRefreshMaxDistance = 112.dp
  val TouchPullRefreshSpinnerSize = 28.dp
  val TouchPullRefreshIndicatorTopPadding = 0.dp
  val TouchPullRefreshContentTravel = 58.dp
  val VideoGridBottomPadding = 72.dp
  val VideoCardMinHeight = 240.dp
  const val VideoThumbnailAspectRatio = 16f / 9f
  val VideoTextHeight = 62.dp
  val TitleMarqueeVelocity = 40.dp
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
  val HomeSectionTabHeight = 40.dp
  val HomeSectionCapsuleHeight = 58.dp
  val HomeSectionCapsuleTopOffset = 6.dp
  val HomeSectionCapsuleHorizontalPadding = 16.dp
  val HomeSectionCapsuleVerticalPadding = 7.dp
  val HomeSectionCapsuleItemSpacing = 20.dp
  val HomeSectionTabMinWidth = 72.dp
  val HomeSectionTabCompactMinWidth = 44.dp
  val SearchKeyboardPanelWidth = 380.dp
  val SearchInputHeight = 44.dp
  val SearchKeyboardButtonHeight = 48.dp
  val SearchTouchInputHeight = 56.dp
  val SearchTouchActionButtonWidth = 86.dp
  val SearchTouchTopPadding = 20.dp
  val SearchTouchContentTopPadding = 18.dp
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
  val PlayerContentPanelWidth = 500.dp
  val PlayerSettingsHeaderHeight = 72.dp
  val PlayerUpPanelHeaderHeight = 80.dp
  val PlayerSettingsRowHeight = 76.dp
  val PlayerEpisodeRowHeight = 54.dp
  val PlayerEpisodeAccentWidth = 4.dp
  val PlayerPanelVideoRowHeight = 132.dp
  val PlayerPanelVideoThumbnailWidth = 208.dp
  val PlayerPanelVideoThumbnailHeight = 117.dp
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
  val PlayerTouchEdgeSwipeWidth = 80.dp
  val PlayerTouchBottomGestureIgnoreHeight = 44.dp
  val PlayerTouchFeedbackCompactSize = 80.dp
  val PlayerTouchFeedbackSize = 96.dp
  val PlayerTouchFeedbackRefinedSize = 108.dp
  val PlayerTouchFeedbackIconSize = 40.dp
  val PlayerTouchFeedbackRefinedIconSize = 46.dp
  val PlayerTouchFeedbackRingStroke = 5.dp
  val PlayerTouchSpeedBoostTopPadding = 16.dp
  val PlayerTouchSpeedBoostWidth = 116.dp
  val PlayerTouchSpeedBoostHeight = 40.dp
  val PlayerTouchSpeedBoostIconSize = 22.dp
  val PlayerTouchSpeedBoostHorizontalPadding = 16.dp
  val PlayerTouchSpeedBoostVerticalPadding = 8.dp
  val PlayerTouchTopGradientHeight = 176.dp
  val PlayerTouchTopPadding = 28.dp
  val PlayerTouchOverlayHorizontalPadding = 24.dp
  val PlayerTouchBottomOverlayHorizontalPadding = 0.dp
  val PlayerTouchBackButtonSize = 52.dp
  val PlayerTouchBackIconSize = 28.dp
  val PlayerTouchControlBarHeight = 76.dp
  val PlayerTouchControlBarHorizontalPadding = 22.dp
  val PlayerTouchControlBarBottomPadding = 16.dp
  val PlayerTouchActionButtonSize = 46.dp
  val PlayerTouchActionIconSize = 24.dp
  val PlayerTouchSliderHeight = 34.dp
  val PlayerTouchSliderTrackHeight = 7.dp
  val PlayerTouchSliderThumbSize = 22.dp
  val PlayerTouchSliderExpandedThumbSize = 30.dp
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
  val SearchInput = 18.sp
  val HomeSectionTab = 19.sp
  val HomeSectionTabLineHeight = 23.sp
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
  const val FocusMs = 150
  const val FocusOutMs = 100
  const val FocusScrollMs = 300
  const val FocusScrollDelayMs = 60L
  const val FocusScrollSettleMs = 120L
  const val FocusScrollMinDeltaPx = 50
  const val FocusedCoverBlurDelayMs = 90L
  const val FocusShineMs = 700
  const val FocusShineInitialDelayMs = 2_000L
  const val FocusShineRepeatDelayMs = 5_000L
  const val TitleMarqueeInitialDelayMs = 350
  const val TitleMarqueeRepeatDelayMs = 900
  const val PanelMs = 180
  const val OverlayMs = 160
  const val FocusSpringDampingRatio = 0.86f
  const val FocusSpringStiffness = 560f
  const val PlayerControlsAutoHideMs = 4_000L
  const val PlayerProgressUpdateMs = 500L
  const val PlayerSeekPreviewAutoCommitMs = 1_200L
  const val PlayerClockUpdateMs = 30_000L
  const val TouchPullRefreshSpinnerRotationMs = 720
  const val PlaybackHeroTransitionMs = 500
  const val PlaybackTransitionScrimInMs = 30
  const val PlaybackTransitionScrimHoldMs = 5
  const val PlaybackTransitionScrimOutMs = 30
  val FocusEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
  val FocusScrollEasing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f)
  val PlaybackHeroEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}

object BiliFocus {
  const val CardScale = 1.055f
  const val CinematicCardScale = 1.072f
  const val CinematicNavScale = 1.035f
  const val CoverHighlightAlpha = 0.08f
  const val CinematicCoverHighlightAlpha = 0.06f
  const val FocusedCoverBlurAlpha = 0.10f
  const val FocusedZIndex = 1f
  const val ShadowAlpha = 0.34f
  const val CinematicFocusedBorderAlpha = 0.54f
  const val CinematicCardGlassBorderAlpha = 0f
  const val CinematicCardGlassOuterAlpha = 0.22f
  const val CinematicCardGlassOuterSheenAlpha = 0.34f
  const val CinematicCardGlassInnerAlpha = 0.16f
  const val CinematicCardGlassInnerSheenAlpha = 0.38f
  const val LiquidGlassFocusedBorderAlpha = 0.36f
  const val LiquidGlassRestingBorderAlpha = 0.12f
  const val LiquidGlassCardSurfaceAlpha = 0.18f
  const val LiquidGlassCardInfoSurfaceAlpha = 0.46f
  const val LiquidGlassCardFocusedOuterAlpha = 0.38f
  const val LiquidGlassCardFocusedOuterDimAlpha = 0.08f
  const val CinematicRestingBorderAlpha = 0.14f
  const val CinematicCardRestingBorderAlpha = 0.10f
  const val CinematicFocusedBackgroundAlpha = 0.10f
  const val CinematicSelectedBackgroundAlpha = 0.08f
  const val CinematicRestingBackgroundAlpha = 0.03f
  const val FocusShineEdgeAlpha = 0.07f
  const val FocusShineCenterAlpha = 0.24f
  const val CinematicFocusGlowAlpha = 0.18f
  const val CinematicCoverVignetteAlpha = 0.28f
  const val CinematicCoverSheenAlpha = 0.18f
  const val CinematicCoverGlowAlphaMultiplier = 0.45f
  const val CinematicCardSheenEdgeAlpha = 0.06f
  const val CinematicCardSheenCenterAlpha = 0.20f
  const val CinematicCardSheenRidgeAlpha = 0.18f
  const val CinematicCardSheenHazeWidthFraction = 0.20f
  const val CinematicCardSheenCoreWidthFraction = 0.055f
  const val CinematicCardSheenRidgeWidthFraction = 0.006f
  const val CinematicCardSheenTravelPaddingMultiplier = 2f
  const val HomeBackgroundCinematicDrift = 0.5f
  const val HomeBackgroundCinematicCardSurfaceAlpha = 1f
  const val HomeBackgroundCinematicAmbientAAlpha = 0.58f
  const val HomeBackgroundCinematicAmbientAX = 0.18f
  const val HomeBackgroundCinematicAmbientAY = 0.04f
  const val HomeBackgroundCinematicAmbientADriftX = 0.04f
  const val HomeBackgroundCinematicAmbientADriftY = 0.03f
  const val HomeBackgroundCinematicAmbientARadius = 0.62f
  const val HomeBackgroundCinematicAmbientBAlpha = 0.48f
  const val HomeBackgroundCinematicAmbientBX = 0.88f
  const val HomeBackgroundCinematicAmbientBY = 0.05f
  const val HomeBackgroundCinematicAmbientBDriftX = 0.05f
  const val HomeBackgroundCinematicAmbientBDriftY = 0.04f
  const val HomeBackgroundCinematicAmbientBRadius = 0.72f
  const val HomeBackgroundCinematicAmbientCAlpha = 0.20f
  const val HomeBackgroundCinematicAmbientCX = 0.66f
  const val HomeBackgroundCinematicAmbientCY = 0.48f
  const val HomeBackgroundCinematicAmbientCDriftX = 0.03f
  const val HomeBackgroundCinematicAmbientCDriftY = 0.04f
  const val HomeBackgroundCinematicAmbientCRadius = 0.55f
  const val HomeBackgroundCinematicBokehAlpha = 0.10f
  const val HomeBackgroundCinematicBokehDriftX = 10f
  const val HomeBackgroundCinematicBokehDriftY = 4f
  const val HomeBackgroundCinematicBokehRadiusDrift = 4f
  const val HomeBackgroundRefinedAmbientAX = 0.14f
  const val HomeBackgroundRefinedAmbientAY = 0.10f
  const val HomeBackgroundRefinedAmbientARadius = 0.72f
  const val HomeBackgroundRefinedAmbientBX = 0.88f
  const val HomeBackgroundRefinedAmbientBY = 0.14f
  const val HomeBackgroundRefinedAmbientBRadius = 0.82f
  const val HomeSidebarCinematicStartAlpha = 0.96f
  const val HomeSidebarCinematicMidAlpha = 0.90f
  const val HomeSidebarCinematicEndAlpha = 0.84f
  const val HomeSidebarRefinedStartAlpha = 0.86f
  const val HomeSidebarRefinedMidAlpha = 0.78f
  const val HomeSidebarRefinedEndAlpha = 0.70f
  const val HomeSidebarCinematicBorderAlpha = 0.22f
  const val HomeSidebarLiquidGlassSurfaceAlpha = 0.62f
  const val HomeSectionCapsuleSurfaceAlpha = 0.54f
  const val HomeSectionCapsuleBorderAlpha = 0.18f
  const val HomeSectionTabFocusedSurfaceAlpha = 0.08f
  const val SettingsChipSelectedBackgroundAlpha = 0.24f
  const val PlayerTouchFeedbackSmoothSurfaceAlpha = 0.82f
  const val PlayerTouchFeedbackBalancedSurfaceAlpha = 0.92f
  const val PlayerTouchFeedbackRefinedSurfaceAlpha = 0.72f
  const val PlayerTouchFeedbackSmoothBorderAlpha = 0f
  const val PlayerTouchFeedbackBalancedBorderAlpha = 0.18f
  const val PlayerTouchFeedbackRefinedBorderAlpha = 0.36f
  const val PlayerTouchFeedbackTrackAlpha = 0.22f
  const val PlayerTouchChromeSmoothSurfaceAlpha = 0.86f
  const val PlayerTouchChromeBalancedSurfaceAlpha = 0.82f
  const val PlayerTouchChromeRefinedSurfaceAlpha = 0.68f
  const val PlayerTouchChromeSmoothBorderAlpha = 0.08f
  const val PlayerTouchChromeBalancedBorderAlpha = 0.14f
  const val PlayerTouchChromeRefinedBorderAlpha = 0.20f
  const val PlayerTouchSliderTrackAlpha = 0.16f
  const val PlayerTouchSliderBufferedAlpha = 0.40f
  const val PlayerTouchSliderThumbSurfaceAlpha = 0.12f
  const val PlaybackHeroScrimAlpha = 0.58f
  const val TouchPullRefreshSpinnerSweepDegrees = 255f
  val BorderWidth = 3.dp
  val RestingBorderWidth = 1.dp
  val RestingShadowElevation = 0.dp
  val ShadowElevation = 12.dp
  val RestingLift = 0.dp
  val CinematicCardLift = 8.dp
  val CinematicCardGlassSafeInset = 5.dp
  val CinematicCardGlassOuterWidth = 3.dp
  val CinematicCardGlassInnerWidth = 1.dp
  val CinematicCardGlassInnerInset = 2.dp
  val LiquidGlassCardFocusPadding = 4.dp
  val LiquidGlassCardBorderWidth = 0.dp
  val LiquidGlassBlurRadius = 3.dp
  val LiquidGlassRefractionHeight = 20.dp
  val LiquidGlassRefractionAmount = 34.dp
  val FocusedCoverBlurRadius = 6.dp
  val ScrollInset = 32.dp
  val FocusedRowTopPadding = 56.dp

  data class HomeBackgroundBokehDot(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
  )

  val HomeBackgroundCinematicBokehDots = listOf(
    HomeBackgroundBokehDot(xFraction = 0.50f, yFraction = 0.03f, radius = 24f),
    HomeBackgroundBokehDot(xFraction = 0.57f, yFraction = 0.11f, radius = 15f),
    HomeBackgroundBokehDot(xFraction = 0.70f, yFraction = 0.04f, radius = 22f),
    HomeBackgroundBokehDot(xFraction = 0.76f, yFraction = 0.16f, radius = 13f),
    HomeBackgroundBokehDot(xFraction = 0.84f, yFraction = 0.08f, radius = 18f),
    HomeBackgroundBokehDot(xFraction = 0.92f, yFraction = 0.19f, radius = 15f),
  )
}

object BiliSkeleton {
  const val StandardItemCount = 12
  const val LowSpecItemCount = 8
  const val TitleLongWidthFraction = 0.92f
  const val TitleShortWidthFraction = 0.68f
  const val MetaWidthFraction = 0.46f
}
