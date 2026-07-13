package com.kirin.bilitv.ui.shell

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class AppShellNavigationState(
  val selectedDestination: AppDestination = AppDestination.Recommend,
  val visitedDestinations: Set<AppDestination> = setOf(AppDestination.Recommend),
  val accountSelected: Boolean = false,
)

internal data class PlaybackFocusOrigin(
  val destination: AppDestination,
  val index: Int,
  val key: String,
)

internal class AppShellNavigationViewModel : ViewModel() {
  private val _state = MutableStateFlow(AppShellNavigationState())
  val state: StateFlow<AppShellNavigationState> = _state.asStateFlow()
  private var playbackFocusOrigin: PlaybackFocusOrigin? = null

  fun selectDestination(destination: AppDestination) {
    val current = _state.value
    _state.value = current.copy(
      selectedDestination = destination,
      visitedDestinations = current.visitedDestinations + destination,
      accountSelected = false,
    )
  }

  fun selectAccount() {
    _state.value = _state.value.copy(accountSelected = true)
  }

  fun rememberPlaybackFocusOrigin(origin: PlaybackFocusOrigin) {
    playbackFocusOrigin = origin
  }

  fun consumePlaybackFocusOrigin(): PlaybackFocusOrigin? {
    return playbackFocusOrigin.also {
      playbackFocusOrigin = null
    }
  }
}

internal fun shouldAutoConfirmDestinationOnFocus(
  autoConfirmOnFocus: Boolean,
  destinationVisited: Boolean,
  startupFocusPending: Boolean,
): Boolean {
  return !startupFocusPending && (autoConfirmOnFocus || !destinationVisited)
}
