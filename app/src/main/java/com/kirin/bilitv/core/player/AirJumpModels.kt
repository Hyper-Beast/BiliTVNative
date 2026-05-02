package com.kirin.bilitv.core.player

data class AirJumpSegment(
  val id: String,
  val category: String,
  val startMs: Long,
  val endMs: Long,
) {
  val durationMs: Long
    get() = (endMs - startMs).coerceAtLeast(0L)
}
