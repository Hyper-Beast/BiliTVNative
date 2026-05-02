package com.kirin.bilitv.core.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.kirin.bilitv.core.storage.biliDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DanmakuSettingsStore(private val context: Context) {
  val settings: Flow<DanmakuSettings> = context.biliDataStore.data.map { preferences ->
    DanmakuSettings(
      enabled = preferences[Keys.Enabled] ?: true,
      opacity = (preferences[Keys.Opacity] ?: 0.8f).coerceIn(0.1f, 1f),
      fontSize = (preferences[Keys.FontSize] ?: DefaultFontSize).coerceAtLeast(MinFontSize),
      area = (preferences[Keys.Area] ?: 0.5f).coerceIn(0.25f, 1f),
      speed = (preferences[Keys.Speed] ?: DefaultSpeed).coerceIn(MinSpeed, MaxSpeed),
      allowTop = preferences[Keys.AllowTop] ?: true,
      allowBottom = preferences[Keys.AllowBottom] ?: true,
    )
  }

  suspend fun setSettings(settings: DanmakuSettings) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.Enabled] = settings.enabled
      preferences[Keys.Opacity] = settings.opacity.coerceIn(0.1f, 1f)
      preferences[Keys.FontSize] = settings.fontSize.coerceAtLeast(MinFontSize)
      preferences[Keys.Area] = settings.area.coerceIn(0.25f, 1f)
      preferences[Keys.Speed] = settings.speed.coerceIn(MinSpeed, MaxSpeed)
      preferences[Keys.AllowTop] = settings.allowTop
      preferences[Keys.AllowBottom] = settings.allowBottom
    }
  }

  suspend fun setEnabled(enabled: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.Enabled] = enabled
    }
  }

  suspend fun setOpacity(opacity: Float) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.Opacity] = opacity.coerceIn(0.1f, 1f)
    }
  }

  suspend fun setFontSize(fontSize: Int) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.FontSize] = fontSize.coerceAtLeast(MinFontSize)
    }
  }

  suspend fun setArea(area: Float) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.Area] = area.coerceIn(0.25f, 1f)
    }
  }

  suspend fun setSpeed(speed: Int) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.Speed] = speed.coerceIn(MinSpeed, MaxSpeed)
    }
  }

  suspend fun setAllowTop(allowTop: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AllowTop] = allowTop
    }
  }

  suspend fun setAllowBottom(allowBottom: Boolean) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.AllowBottom] = allowBottom
    }
  }

  private object Keys {
    val Enabled = booleanPreferencesKey("player_danmaku_enabled")
    val Opacity = floatPreferencesKey("player_danmaku_opacity")
    val FontSize = intPreferencesKey("player_danmaku_font_size")
    val Area = floatPreferencesKey("player_danmaku_area")
    val Speed = intPreferencesKey("player_danmaku_speed")
    val AllowTop = booleanPreferencesKey("player_danmaku_allow_top")
    val AllowBottom = booleanPreferencesKey("player_danmaku_allow_bottom")
  }

  private companion object {
    const val MinFontSize = 16
    const val DefaultFontSize = 28
    const val MinSpeed = 3
    const val DefaultSpeed = 5
    const val MaxSpeed = 7
  }
}
