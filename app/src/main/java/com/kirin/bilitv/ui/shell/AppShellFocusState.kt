package com.kirin.bilitv.ui.shell

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester

@Stable
internal class AppShellFocusState {
  val accountFocusRequester = FocusRequester()
  val navFocusRequesters = AppDestination.entries.associateWith { FocusRequester() }
  val contentFocusRequester = FocusRequester()
  val searchFocusRequester = FocusRequester()
  val dynamicFocusRequester = FocusRequester()
  val historyFocusRequester = FocusRequester()
  val settingsFocusRequester = FocusRequester()

  var playbackFocusRestoreDestination by mutableStateOf<AppDestination?>(null)
    private set
  var playbackFocusRestoreRequestKey by mutableIntStateOf(0)
    private set
  var contentFocusRestoreDestination by mutableStateOf<AppDestination?>(null)
    private set
  var contentFocusRestoreRequestKey by mutableIntStateOf(0)
    private set
  var pendingContentFocusDestination by mutableStateOf<AppDestination?>(null)
    private set

  fun requestDestinationFocus(destination: AppDestination): Boolean {
    return runCatching {
      when (destination) {
        AppDestination.Recommend -> contentFocusRequester.requestFocus()
        AppDestination.Search -> searchFocusRequester.requestFocus()
        AppDestination.Dynamic -> dynamicFocusRequester.requestFocus()
        AppDestination.History -> historyFocusRequester.requestFocus()
        AppDestination.Settings -> settingsFocusRequester.requestFocus()
      }
    }.getOrDefault(false)
  }

  fun restoreFocusRequestKeyFor(destination: AppDestination): Int {
    return when {
      playbackFocusRestoreDestination == destination -> playbackFocusRestoreRequestKey
      contentFocusRestoreDestination == destination -> contentFocusRestoreRequestKey
      else -> 0
    }
  }

  fun clearFocusRestoreRequest(destination: AppDestination, key: Int) {
    if (playbackFocusRestoreDestination == destination && key == playbackFocusRestoreRequestKey) {
      playbackFocusRestoreDestination = null
    }
    if (contentFocusRestoreDestination == destination && key == contentFocusRestoreRequestKey) {
      contentFocusRestoreDestination = null
      pendingContentFocusDestination = null
    }
  }

  fun requestContentFocusRestore(destination: AppDestination) {
    if (destination.usesGridFocusRestore()) {
      contentFocusRestoreDestination = destination
      contentFocusRestoreRequestKey += 1
      pendingContentFocusDestination = null
    } else {
      pendingContentFocusDestination = destination
    }
  }

  fun requestPlaybackFocusRestore(destination: AppDestination) {
    playbackFocusRestoreDestination = destination
    playbackFocusRestoreRequestKey += 1
  }

  fun clearPendingContentFocusDestination() {
    pendingContentFocusDestination = null
  }

  private fun AppDestination.usesGridFocusRestore(): Boolean {
    return this == AppDestination.Recommend || this == AppDestination.Dynamic || this == AppDestination.History
  }
}
