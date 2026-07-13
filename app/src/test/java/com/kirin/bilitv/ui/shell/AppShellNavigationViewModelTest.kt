package com.kirin.bilitv.ui.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellNavigationViewModelTest {
  @Test
  fun freshInstanceStartsOnRecommend() {
    val state = AppShellNavigationViewModel().state.value

    assertEquals(AppDestination.Recommend, state.selectedDestination)
    assertEquals(setOf(AppDestination.Recommend), state.visitedDestinations)
    assertFalse(state.accountSelected)
  }

  @Test
  fun selectingDestinationClosesAccountAndTracksVisit() {
    val viewModel = AppShellNavigationViewModel()
    viewModel.selectAccount()

    viewModel.selectDestination(AppDestination.Search)

    val state = viewModel.state.value
    assertEquals(AppDestination.Search, state.selectedDestination)
    assertTrue(AppDestination.Search in state.visitedDestinations)
    assertFalse(state.accountSelected)
  }

  @Test
  fun newInstanceResetsPreviousRouteToRecommend() {
    AppShellNavigationViewModel().selectDestination(AppDestination.Search)

    val restartedState = AppShellNavigationViewModel().state.value

    assertEquals(AppDestination.Recommend, restartedState.selectedDestination)
  }

  @Test
  fun playbackOriginSurvivesInViewModelUntilConsumed() {
    val viewModel = AppShellNavigationViewModel()
    val origin = PlaybackFocusOrigin(
      destination = AppDestination.Recommend,
      index = 7,
      key = "BV1SOURCE",
    )
    viewModel.rememberPlaybackFocusOrigin(origin)

    assertEquals(origin, viewModel.consumePlaybackFocusOrigin())
    assertEquals(null, viewModel.consumePlaybackFocusOrigin())
  }

  @Test
  fun startupFocusGuardBlocksFirstFocusableDestinationFromOpening() {
    assertFalse(
      shouldAutoConfirmDestinationOnFocus(
        autoConfirmOnFocus = false,
        destinationVisited = false,
        startupFocusPending = true,
      ),
    )
    assertTrue(
      shouldAutoConfirmDestinationOnFocus(
        autoConfirmOnFocus = false,
        destinationVisited = false,
        startupFocusPending = false,
      ),
    )
  }
}
