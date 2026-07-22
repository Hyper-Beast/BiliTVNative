package com.kirin.bilitv.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import com.kirin.bilitv.core.storage.UserSession

internal const val AdaptiveAppScaffoldSideNavigationTestTag = "adaptive_app_scaffold_side_navigation"

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
  TvAppScaffold(
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
