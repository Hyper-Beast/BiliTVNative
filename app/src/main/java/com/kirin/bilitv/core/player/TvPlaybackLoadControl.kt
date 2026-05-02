package com.kirin.bilitv.core.player

import androidx.media3.exoplayer.DefaultLoadControl

fun createTvPlaybackLoadControl(): DefaultLoadControl {
  return DefaultLoadControl.Builder()
    .setBufferDurationsMs(
      MinBufferMs,
      MaxBufferMs,
      BufferForPlaybackMs,
      BufferForPlaybackAfterRebufferMs,
    )
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
}

private const val MinBufferMs = 30_000
private const val MaxBufferMs = 90_000
private const val BufferForPlaybackMs = 1_500
private const val BufferForPlaybackAfterRebufferMs = 4_000
