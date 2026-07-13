package com.kirin.bilitv.ui.transition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTransitionPolicyTest {
  @Test
  fun smoothModeDisablesEntryExitAndOutgoingRetention() {
    assertFalse(
      PlaybackTransitionPolicy.shouldAnimateEntry(
        motionEnabled = false,
        thumbnailAvailable = true,
      ),
    )
    assertFalse(
      PlaybackTransitionPolicy.shouldAnimateExit(
        motionEnabled = false,
        sharedKeyAvailable = true,
        exitFrameAvailable = true,
      ),
    )
    assertFalse(
      PlaybackTransitionPolicy.shouldRetainOutgoingContent(
        motionEnabled = false,
        sharedTransitionActive = true,
      ),
    )
  }

  @Test
  fun exitAnimationRequiresCurrentFrame() {
    assertFalse(
      PlaybackTransitionPolicy.shouldAnimateExit(
        motionEnabled = true,
        sharedKeyAvailable = true,
        exitFrameAvailable = false,
      ),
    )
    assertTrue(
      PlaybackTransitionPolicy.shouldAnimateExit(
        motionEnabled = true,
        sharedKeyAvailable = true,
        exitFrameAvailable = true,
      ),
    )
  }
}
