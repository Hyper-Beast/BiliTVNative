package com.kirin.bilitv.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.theme.BiliSizing

@Composable
internal fun TvAppScaffold(
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
  Row(
    modifier = Modifier
      .fillMaxSize()
      .testTag(AdaptiveAppScaffoldRemoteTestTag),
  ) {
    AppSidebar(
      selectedDestination = selectedDestination,
      accountSelected = accountSelected,
      userSession = userSession,
      autoConfirmOnFocus = autoConfirmOnFocus,
      accountFocusRequester = accountFocusRequester,
      navFocusRequesters = navFocusRequesters,
      onAccountSelected = onAccountSelected,
      onDestinationSelected = onDestinationSelected,
      shouldAutoConfirmDestination = shouldAutoConfirmDestination,
      onMoveRight = onMoveRight,
    )
    Box(
      modifier = if (contentPaddingEnabled) {
        Modifier
          .fillMaxSize()
          .padding(BiliSizing.ContentPadding)
      } else {
        Modifier.fillMaxSize()
      },
    ) {
      content()
    }
  }
}
