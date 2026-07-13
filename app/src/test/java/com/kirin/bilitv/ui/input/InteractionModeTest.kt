package com.kirin.bilitv.ui.input

import org.junit.Assert.assertEquals
import org.junit.Test

class InteractionModeTest {
  @Test
  fun televisionWithoutTouchscreenUsesRemoteMode() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = true,
        hasLeanback = true,
        hasTouchscreen = false,
        smallestScreenWidthDp = 720,
      ),
      expectedDeviceClass = DeviceClass.Tv,
      expectedInputMode = InputMode.Remote,
    )
  }

  @Test
  fun explicitTelevisionModeWinsOverMisreportedTouchscreen() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = true,
        hasLeanback = true,
        hasTouchscreen = true,
        smallestScreenWidthDp = 720,
      ),
      expectedDeviceClass = DeviceClass.Tv,
      expectedInputMode = InputMode.Remote,
    )
  }

  @Test
  fun ordinaryTabletUsesTouchMode() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = false,
        hasLeanback = false,
        hasTouchscreen = true,
        smallestScreenWidthDp = 800,
      ),
      expectedDeviceClass = DeviceClass.Tablet,
      expectedInputMode = InputMode.Touch,
    )
  }

  @Test
  fun leanbackTabletWithTouchscreenStaysInTouchMode() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = false,
        hasLeanback = true,
        hasTouchscreen = true,
        smallestScreenWidthDp = 800,
      ),
      expectedDeviceClass = DeviceClass.Tablet,
      expectedInputMode = InputMode.Touch,
    )
  }

  @Test
  fun leanbackDeviceWithoutTouchscreenUsesRemoteMode() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = false,
        hasLeanback = true,
        hasTouchscreen = false,
        smallestScreenWidthDp = 540,
      ),
      expectedDeviceClass = DeviceClass.Tv,
      expectedInputMode = InputMode.Remote,
    )
  }

  @Test
  fun phoneUsesTouchMode() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = false,
        hasLeanback = false,
        hasTouchscreen = true,
        smallestScreenWidthDp = 411,
      ),
      expectedDeviceClass = DeviceClass.Phone,
      expectedInputMode = InputMode.Touch,
    )
  }

  @Test
  fun tabletBoundaryStartsAt600Dp() {
    assertProfile(
      signals = InteractionDeviceSignals(
        isTelevisionUiMode = false,
        hasLeanback = false,
        hasTouchscreen = true,
        smallestScreenWidthDp = 600,
      ),
      expectedDeviceClass = DeviceClass.Tablet,
      expectedInputMode = InputMode.Touch,
    )
  }

  private fun assertProfile(
    signals: InteractionDeviceSignals,
    expectedDeviceClass: DeviceClass,
    expectedInputMode: InputMode,
  ) {
    val profile = resolveInteractionProfile(signals)

    assertEquals(expectedDeviceClass, profile.deviceClass)
    assertEquals(expectedInputMode, profile.inputMode)
  }
}
