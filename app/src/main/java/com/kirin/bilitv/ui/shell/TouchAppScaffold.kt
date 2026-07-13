package com.kirin.bilitv.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.kirin.bilitv.R
import com.kirin.bilitv.core.storage.UserSession
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Composable
internal fun TouchAppScaffold(
  selectedDestination: AppDestination,
  accountSelected: Boolean,
  userSession: UserSession,
  contentPaddingEnabled: Boolean,
  onAccountSelected: () -> Unit,
  onDestinationSelected: (AppDestination) -> Unit,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .testTag(AdaptiveAppScaffoldTouchTestTag),
  ) {
    TouchAppNavigationBar(
      selectedDestination = selectedDestination,
      accountSelected = accountSelected,
      userSession = userSession,
      onAccountSelected = onAccountSelected,
      onDestinationSelected = onDestinationSelected,
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

@Composable
private fun TouchAppNavigationBar(
  selectedDestination: AppDestination,
  accountSelected: Boolean,
  userSession: UserSession,
  onAccountSelected: () -> Unit,
  onDestinationSelected: (AppDestination) -> Unit,
) {
  val homeColors = LocalHomeColors.current
  val performancePolicy = LocalBiliPerformancePolicy.current
  val refined = performancePolicy.cinematicVisualEffectsEnabled
  val shape = RoundedCornerShape(BiliRadius.Panel)
  val surface = if (refined) {
    Brush.horizontalGradient(
      colors = listOf(
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarCinematicStartAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarCinematicMidAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarCinematicEndAlpha),
      ),
    )
  } else {
    Brush.horizontalGradient(
      colors = listOf(
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarRefinedStartAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarRefinedMidAlpha),
        homeColors.sidebarSurface.copy(alpha = BiliFocus.HomeSidebarRefinedEndAlpha),
      ),
    )
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        start = BiliSizing.ContentPadding,
        top = BiliSpacing.Md,
        end = BiliSizing.ContentPadding,
        bottom = BiliSpacing.Sm,
      )
      .height(BiliSizing.NavItemHeight)
      .clip(shape)
      .background(surface)
      .border(BorderStroke(BiliFocus.RestingBorderWidth, homeColors.glassBorder), shape)
      .padding(horizontal = BiliSpacing.Sm),
    horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Sm),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TouchNavIcon(
      iconRes = R.drawable.ic_nav_account,
      contentDescription = if (userSession.isLoggedIn) {
        userSession.uname.orEmpty().ifBlank { stringResource(R.string.account_logged_in_default) }
      } else {
        stringResource(R.string.nav_login)
      },
      selected = accountSelected,
      onClick = onAccountSelected,
    )
    Spacer(modifier = Modifier.weight(1f))
    AppDestination.entries.forEach { destination ->
      TouchNavIcon(
        iconRes = destination.iconRes,
        contentDescription = stringResource(destination.titleRes),
        selected = !accountSelected && selectedDestination == destination,
        onClick = {
          onDestinationSelected(destination)
        },
      )
    }
  }
}

@Composable
private fun TouchNavIcon(
  iconRes: Int,
  contentDescription: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val homeColors = LocalHomeColors.current
  val shape = RoundedCornerShape(BiliRadius.Pill)
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier
      .size(BiliSizing.NavItemHeight)
      .clip(shape)
      .background(
        color = if (selected) {
          homeColors.textPrimary.copy(alpha = BiliFocus.CinematicSelectedBackgroundAlpha)
        } else {
          BiliColors.Transparent
        },
        shape = shape,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = if (selected) homeColors.accent else homeColors.textSecondary,
      modifier = Modifier.size(BiliSizing.NavIconSize),
    )
  }
}
