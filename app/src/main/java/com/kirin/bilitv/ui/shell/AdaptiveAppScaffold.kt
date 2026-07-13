package com.kirin.bilitv.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.input.InputMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile

internal const val AdaptiveAppScaffoldRemoteTestTag = "adaptive_app_scaffold_remote"
internal const val AdaptiveAppScaffoldTouchTestTag = "adaptive_app_scaffold_touch"

@Composable
internal fun AdaptiveAppScaffold(
  selectedDestination: AppDestination,
  accountSelected: Boolean,
  userSession: UserSession,
  autoConfirmOnFocus: Boolean,
  accountFocusRequester: FocusRequester,
  navFocusRequesters: Map<AppDestination, FocusRequester>,
  contentPaddingEnabled: Boolean,
  onAccountSelected: () -> Unit,
  onDestinationSelected: (AppDestination) -> Unit,
  shouldAutoConfirmDestination: (AppDestination) -> Boolean,
  onMoveRight: (AppDestination) -> Boolean,
  content: @Composable () -> Unit,
) {
  when (LocalInteractionProfile.current.inputMode) {
    InputMode.Touch -> TouchAppScaffold(
      selectedDestination = selectedDestination,
      accountSelected = accountSelected,
      userSession = userSession,
      contentPaddingEnabled = contentPaddingEnabled,
      onAccountSelected = onAccountSelected,
      onDestinationSelected = onDestinationSelected,
      content = content,
    )

    InputMode.Remote -> TvAppScaffold(
      selectedDestination = selectedDestination,
      accountSelected = accountSelected,
      userSession = userSession,
      autoConfirmOnFocus = autoConfirmOnFocus,
      accountFocusRequester = accountFocusRequester,
      navFocusRequesters = navFocusRequesters,
      contentPaddingEnabled = contentPaddingEnabled,
      onAccountSelected = onAccountSelected,
      onDestinationSelected = onDestinationSelected,
      shouldAutoConfirmDestination = shouldAutoConfirmDestination,
      onMoveRight = onMoveRight,
      content = content,
    )
  }
}
