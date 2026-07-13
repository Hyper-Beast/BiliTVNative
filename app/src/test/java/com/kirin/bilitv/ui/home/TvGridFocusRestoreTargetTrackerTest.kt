package com.kirin.bilitv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TvGridFocusRestoreTargetTrackerTest {
  @Test
  fun keepsOriginalTargetWhileSameRestoreRequestIsActive() {
    val tracker = TvGridFocusRestoreTargetTracker()

    assertEquals(7, tracker.targetIndex(requestKey = 1, currentIndex = 7))
    assertEquals(7, tracker.targetIndex(requestKey = 1, currentIndex = 0))
  }

  @Test
  fun acceptsNewTargetForNextRestoreRequest() {
    val tracker = TvGridFocusRestoreTargetTracker()
    tracker.targetIndex(requestKey = 1, currentIndex = 7)

    assertEquals(3, tracker.targetIndex(requestKey = 2, currentIndex = 3))
  }
}
