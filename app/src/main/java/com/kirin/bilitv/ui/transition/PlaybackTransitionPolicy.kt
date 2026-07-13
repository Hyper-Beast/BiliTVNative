package com.kirin.bilitv.ui.transition

internal object PlaybackTransitionPolicy {
  fun shouldAnimateEntry(
    motionEnabled: Boolean,
    thumbnailAvailable: Boolean,
  ): Boolean {
    return motionEnabled && thumbnailAvailable
  }

  fun shouldAnimateExit(
    motionEnabled: Boolean,
    sharedKeyAvailable: Boolean,
    exitFrameAvailable: Boolean,
  ): Boolean {
    return motionEnabled &&
      sharedKeyAvailable &&
      exitFrameAvailable
  }

  fun shouldRetainOutgoingContent(
    motionEnabled: Boolean,
    sharedTransitionActive: Boolean,
  ): Boolean {
    return motionEnabled && sharedTransitionActive
  }
}
