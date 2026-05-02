package com.kirin.bilitv.core.player

data class DanmakuSettings(
  val enabled: Boolean = true,
  val opacity: Float = 0.8f,
  val fontSize: Int = 28,
  val area: Float = 0.5f,
  val speed: Int = 5,
  val allowTop: Boolean = true,
  val allowBottom: Boolean = true,
)
