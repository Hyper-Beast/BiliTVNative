package com.kirin.bilitv.core.player

import android.graphics.Color

data class DanmakuEntry(
  val showAtMs: Long,
  val text: String,
  val mode: DanmakuMode,
  val color: Int = Color.WHITE,
)

enum class DanmakuMode {
  Scroll,
  Top,
  Bottom,
}
