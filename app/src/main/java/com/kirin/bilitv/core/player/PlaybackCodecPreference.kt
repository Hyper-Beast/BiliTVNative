package com.kirin.bilitv.core.player

enum class PlaybackCodecPreference(val key: String) {
  Auto("auto"),
  H264("h264"),
  H265("h265"),
  Av1("av1");

  companion object {
    fun fromKey(key: String?): PlaybackCodecPreference {
      return entries.firstOrNull { preference -> preference.key == key } ?: Auto
    }
  }
}
