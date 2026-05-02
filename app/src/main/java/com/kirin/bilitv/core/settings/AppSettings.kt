package com.kirin.bilitv.core.settings

import com.kirin.bilitv.core.i18n.ChineseTextVariant
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackQualityPreference

data class AppSettings(
  val lowSpecMode: Boolean = false,
  val chineseTextVariant: ChineseTextVariant = ChineseTextVariant.Simplified,
  val playbackQualityPreference: PlaybackQualityPreference = PlaybackQualityPreference.Highest,
  val playbackCodecPreference: PlaybackCodecPreference = PlaybackCodecPreference.Auto,
  val seekPreviewSpritesEnabled: Boolean = true,
  val airJumpAssistantEnabled: Boolean = true,
  val confirmPlaybackExit: Boolean = true,
  val autoPlayNextEpisode: Boolean = false,
  val autoPlayRelatedVideo: Boolean = false,
  val autoReturnHomeOnCompletion: Boolean = false,
  val showClock: Boolean = true,
  val autoConfirmOnFocus: Boolean = false,
  val autoRefreshOnSwitch: Boolean = false,
  val enabledHomeSections: Set<HomeSection> = HomeSection.DefaultOrder.toSet(),
)

data class AppPerformancePolicy(
  val lowSpecMode: Boolean,
  val motionEnabled: Boolean,
  val smoothScrollingEnabled: Boolean,
  val videoThumbnailWidthPx: Int,
  val videoThumbnailHeightPx: Int,
  val videoThumbnailRgb565Enabled: Boolean,
  val ownerAvatarSizePx: Int,
  val ownerAvatarRgb565Enabled: Boolean,
  val imageMemoryCacheEnabled: Boolean,
  val videoThumbnailPrefetchCount: Int,
  val focusShadowEnabled: Boolean,
  val loadMoreFocusThreshold: Int,
  val focusedCoverBlurEnabled: Boolean,
) {
  companion object {
    val Standard = AppPerformancePolicy(
      lowSpecMode = false,
      motionEnabled = true,
      smoothScrollingEnabled = true,
      videoThumbnailWidthPx = 480,
      videoThumbnailHeightPx = 270,
      videoThumbnailRgb565Enabled = false,
      ownerAvatarSizePx = 72,
      ownerAvatarRgb565Enabled = false,
      imageMemoryCacheEnabled = true,
      videoThumbnailPrefetchCount = 16,
      focusShadowEnabled = true,
      loadMoreFocusThreshold = 16,
      focusedCoverBlurEnabled = true,
    )

    val LowSpec = AppPerformancePolicy(
      lowSpecMode = true,
      motionEnabled = false,
      smoothScrollingEnabled = false,
      videoThumbnailWidthPx = 320,
      videoThumbnailHeightPx = 180,
      videoThumbnailRgb565Enabled = true,
      ownerAvatarSizePx = 48,
      ownerAvatarRgb565Enabled = true,
      imageMemoryCacheEnabled = false,
      videoThumbnailPrefetchCount = 0,
      focusShadowEnabled = false,
      loadMoreFocusThreshold = 6,
      focusedCoverBlurEnabled = false,
    )

    fun fromSettings(settings: AppSettings): AppPerformancePolicy {
      return if (settings.lowSpecMode) LowSpec else Standard
    }
  }
}
