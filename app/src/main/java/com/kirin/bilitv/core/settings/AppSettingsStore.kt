package com.kirin.bilitv.core.settings

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kirin.bilitv.core.i18n.ChineseTextVariant
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackQualityPreference
import com.kirin.bilitv.core.storage.biliDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsStore(private val context: Context) {
  private val defaultLowSpecMode by lazy(LazyThreadSafetyMode.NONE) {
    context.shouldEnableLowSpecModeByDefault()
  }

  val settings: Flow<AppSettings> = context.biliDataStore.data.map { preferences ->
    val enabledSections = preferences[Keys.EnabledHomeSections]
      ?.mapNotNull(HomeSection::fromKey)
      ?.toSet()
      ?.takeIf { sections -> sections.isNotEmpty() }
      ?: HomeSection.DefaultOrder.toSet()

    val autoConfirmOnFocus = preferences[Keys.AutoConfirmOnFocus] ?: false
    val autoRefreshOnSwitch = autoConfirmOnFocus && (preferences[Keys.AutoRefreshOnSwitch] ?: false)

    AppSettings(
      lowSpecMode = preferences[Keys.LowSpecMode] ?: defaultLowSpecMode,
      chineseTextVariant = ChineseTextVariant.fromKey(preferences[Keys.ChineseTextVariant]),
      playbackQualityPreference = PlaybackQualityPreference.fromKey(preferences[Keys.PlaybackQualityPreference]),
      playbackCodecPreference = PlaybackCodecPreference.fromKey(preferences[Keys.PlaybackCodecPreference]),
      seekPreviewSpritesEnabled = preferences[Keys.SeekPreviewSpritesEnabled] ?: true,
      airJumpAssistantEnabled = preferences[Keys.AirJumpAssistantEnabled] ?: true,
      confirmPlaybackExit = preferences[Keys.ConfirmPlaybackExit] ?: true,
      autoPlayNextEpisode = preferences[Keys.AutoPlayNextEpisode] ?: false,
      autoPlayRelatedVideo = preferences[Keys.AutoPlayRelatedVideo] ?: false,
      autoReturnHomeOnCompletion = preferences[Keys.AutoReturnHomeOnCompletion] ?: false,
      showClock = preferences[Keys.ShowClock] ?: true,
      autoConfirmOnFocus = autoConfirmOnFocus,
      autoRefreshOnSwitch = autoRefreshOnSwitch,
      enabledHomeSections = enabledSections,
    )
  }

  suspend fun setLowSpecMode(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.LowSpecMode] = enabled
    }
  }

  suspend fun setChineseTextVariant(variant: ChineseTextVariant) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.ChineseTextVariant] = variant.key
    }
  }

  suspend fun setSeekPreviewSpritesEnabled(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.SeekPreviewSpritesEnabled] = enabled
    }
  }

  suspend fun setPlaybackCodecPreference(preference: PlaybackCodecPreference) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.PlaybackCodecPreference] = preference.key
    }
  }

  suspend fun setPlaybackQualityPreference(preference: PlaybackQualityPreference) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.PlaybackQualityPreference] = preference.key
    }
  }

  suspend fun setAirJumpAssistantEnabled(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AirJumpAssistantEnabled] = enabled
    }
  }

  suspend fun setConfirmPlaybackExit(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.ConfirmPlaybackExit] = enabled
    }
  }

  suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AutoPlayNextEpisode] = enabled
    }
  }

  suspend fun setAutoPlayRelatedVideo(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AutoPlayRelatedVideo] = enabled
    }
  }

  suspend fun setAutoReturnHomeOnCompletion(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AutoReturnHomeOnCompletion] = enabled
    }
  }

  suspend fun setShowClock(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.ShowClock] = enabled
    }
  }

  suspend fun setAutoConfirmOnFocus(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AutoConfirmOnFocus] = enabled
      if (!enabled) {
        preferences[Keys.AutoRefreshOnSwitch] = false
      }
    }
  }

  suspend fun setAutoRefreshOnSwitch(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AutoRefreshOnSwitch] = enabled && (preferences[Keys.AutoConfirmOnFocus] ?: false)
    }
  }

  suspend fun setHomeSectionEnabled(section: HomeSection, enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      val current = preferences[Keys.EnabledHomeSections]
        ?.mapNotNull(HomeSection::fromKey)
        ?.toMutableSet()
        ?: HomeSection.DefaultOrder.toMutableSet()

      if (enabled) {
        current.add(section)
      } else if (current.size > 1) {
        current.remove(section)
      }

      preferences[Keys.EnabledHomeSections] = current.map { item -> item.key }.toSet()
    }
  }

  private object Keys {
    val LowSpecMode = booleanPreferencesKey("low_spec_mode")
    val ChineseTextVariant = stringPreferencesKey("chinese_text_variant")
    val PlaybackQualityPreference = stringPreferencesKey("playback_quality_preference")
    val PlaybackCodecPreference = stringPreferencesKey("playback_codec_preference")
    val SeekPreviewSpritesEnabled = booleanPreferencesKey("seek_preview_sprites_enabled")
    val AirJumpAssistantEnabled = booleanPreferencesKey("air_jump_assistant_enabled")
    val ConfirmPlaybackExit = booleanPreferencesKey("confirm_playback_exit")
    val AutoPlayNextEpisode = booleanPreferencesKey("auto_play_next_episode")
    val AutoPlayRelatedVideo = booleanPreferencesKey("auto_play_related_video")
    val AutoReturnHomeOnCompletion = booleanPreferencesKey("auto_return_home_on_completion")
    val ShowClock = booleanPreferencesKey("show_clock")
    val AutoConfirmOnFocus = booleanPreferencesKey("auto_confirm_on_focus")
    val AutoRefreshOnSwitch = booleanPreferencesKey("auto_refresh_on_switch")
    val EnabledHomeSections = stringSetPreferencesKey("enabled_home_sections")
  }
}

private fun Context.shouldEnableLowSpecModeByDefault(): Boolean {
  val activityManager = getSystemService(ActivityManager::class.java) ?: return false
  val memoryInfo = ActivityManager.MemoryInfo()
  activityManager.getMemoryInfo(memoryInfo)
  return memoryInfo.totalMem > 0L && memoryInfo.totalMem < LowSpecDefaultMemoryThresholdBytes
}

private const val LowSpecDefaultMemoryThresholdBytes = 1024L * 1024L * 1024L
