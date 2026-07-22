package com.kirin.bilitv.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.input.DeviceClass
import com.kirin.bilitv.ui.input.InputMode
import com.kirin.bilitv.ui.input.InteractionProfile
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionProfile
import com.kirin.bilitv.ui.theme.BiliTvTheme
import org.junit.Rule
import org.junit.Test

class AdaptiveAppScaffoldSmokeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun remoteProfileRendersTvScaffoldAndContent() {
    renderScaffold(
      InteractionProfile(
        deviceClass = DeviceClass.Tv,
        inputMode = InputMode.Remote,
      ),
    )

    composeRule.onNodeWithTag(AdaptiveAppScaffoldSideNavigationTestTag).assertExists()
    composeRule.onNodeWithTag(ScaffoldContentTestTag).assertExists()
  }

  @Test
  fun touchProfileRendersSideNavigationScaffoldAndContent() {
    renderScaffold(
      InteractionProfile(
        deviceClass = DeviceClass.Tablet,
        inputMode = InputMode.Touch,
      ),
    )

    composeRule.onNodeWithTag(AdaptiveAppScaffoldSideNavigationTestTag).assertExists()
    composeRule.onNodeWithTag(ScaffoldContentTestTag).assertExists()
  }

  private fun renderScaffold(profile: InteractionProfile) {
    composeRule.setContent {
      val accountFocusRequester = remember { FocusRequester() }
      val navFocusRequesters = remember {
        AppDestination.entries.associateWith { FocusRequester() }
      }
      BiliTvTheme {
        CompositionLocalProvider(
          LocalInteractionProfile provides profile,
          LocalInteractionMode provides profile.legacyMode,
        ) {
          AdaptiveAppScaffold(
            selectedDestination = AppDestination.Recommend,
            accountSelected = false,
            userSession = UserSession(),
            autoConfirmOnFocus = false,
            accountFocusRequester = accountFocusRequester,
            navFocusRequesters = navFocusRequesters,
            contentPaddingEnabled = false,
            onAccountSelected = {},
            onDestinationSelected = {},
            shouldAutoConfirmDestination = { false },
            onMoveRight = { false },
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .testTag(ScaffoldContentTestTag),
            )
          }
        }
      }
    }
  }

  private companion object {
    const val ScaffoldContentTestTag = "adaptive_app_scaffold_content"
  }
}
