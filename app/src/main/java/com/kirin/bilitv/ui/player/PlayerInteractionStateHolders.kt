package com.kirin.bilitv.ui.player

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

internal class PlayerOverlayFocusStateHolder {
  val progressFocusedState = mutableStateOf(false)
  val focusedControlState = mutableStateOf(PlayerControl.Episodes)
  val focusedPanelIndexState = mutableIntStateOf(0)

  fun resetPrimaryControlFocus() {
    focusedControlState.value = PlayerControl.Episodes
    progressFocusedState.value = false
  }

  fun clearProgressFocus() {
    progressFocusedState.value = false
  }

  fun focusProgress() {
    progressFocusedState.value = true
  }

  fun moveFocusedControl(delta: Int) {
    val controls = PlayerControl.entries
    val next = (controls.indexOf(focusedControlState.value) + delta).coerceIn(0, controls.lastIndex)
    focusedControlState.value = controls[next]
    clearProgressFocus()
  }
}

internal class PlayerTouchGestureStateHolder {
  val seekActiveState = mutableStateOf(false)
  val gestureSeekActiveState = mutableStateOf(false)
  val gestureSeekDeltaMsState = mutableLongStateOf(0L)
  val feedbackState = mutableStateOf<PlayerTouchFeedback?>(null)

  fun beginSeek() {
    seekActiveState.value = true
  }

  fun endSeek() {
    seekActiveState.value = false
  }

  fun clearSeek() {
    seekActiveState.value = false
    gestureSeekActiveState.value = false
    gestureSeekDeltaMsState.longValue = 0L
  }

  fun beginGestureSeek() {
    seekActiveState.value = true
    gestureSeekActiveState.value = true
    gestureSeekDeltaMsState.longValue = 0L
  }

  fun updateGestureSeekDelta(deltaMs: Long) {
    gestureSeekDeltaMsState.longValue = deltaMs
  }

  fun showFeedback(type: PlayerTouchFeedbackType, percent: Int = 0) {
    feedbackState.value = PlayerTouchFeedback(type = type, percent = percent)
  }

  fun clearFeedback() {
    feedbackState.value = null
  }

  fun clearFeedbackUnlessSpeedBoost() {
    if (feedbackState.value?.type != PlayerTouchFeedbackType.SpeedBoost) {
      feedbackState.value = null
    }
  }
}

internal data class PlayerTouchFeedback(
  val type: PlayerTouchFeedbackType,
  val percent: Int = 0,
)

internal enum class PlayerTouchFeedbackType {
  Brightness,
  Volume,
  SpeedBoost,
}
