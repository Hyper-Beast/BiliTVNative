package com.kirin.bilitv.core.player

enum class PlaybackQualityPreference(
  val key: String,
  val requestedQualityId: Int,
) {
  Highest("highest", 127),
  Q1080("1080", 80),
  Q720("720", 64),
  Q480("480", 32);

  companion object {
    fun fromKey(key: String?): PlaybackQualityPreference {
      return entries.firstOrNull { preference -> preference.key == key } ?: Highest
    }
  }
}
