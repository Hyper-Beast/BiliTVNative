package com.kirin.bilitv.ui.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.kirin.bilitv.ui.glass.biliLiquidGlassSurface
import com.kirin.bilitv.ui.input.InteractionMode
import com.kirin.bilitv.ui.input.LocalInteractionMode
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.LocalHomeColors

@Composable
fun BiliFocusableSurface(
  modifier: Modifier = Modifier,
  scaleOnFocus: Boolean = true,
  shadowOnFocus: Boolean = true,
  shape: Shape = RoundedCornerShape(BiliRadius.Card),
  focusedScale: Float = BiliFocus.CardScale,
  focusedBorderColor: Color? = null,
  restingBorderColor: Color? = null,
  focusedBorderWidth: Dp = BiliFocus.BorderWidth,
  restingBorderWidth: Dp = BiliFocus.RestingBorderWidth,
  focusedShadowColor: Color? = null,
  focusedShadowElevation: Dp = BiliFocus.ShadowElevation,
  focusedLift: Dp = BiliFocus.RestingLift,
  focusedBackgroundColor: Color? = null,
  restingBackgroundColor: Color? = null,
  focusedForeground: (DrawScope.() -> Unit)? = null,
  onClick: () -> Unit = {},
  onFocused: () -> Unit = {},
  onFocusChanged: (Boolean) -> Unit = {},
  content: @Composable () -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val performancePolicy = LocalBiliPerformancePolicy.current
  val interactionMode = LocalInteractionMode.current
  val homeColors = LocalHomeColors.current
  val focusVisualsEnabled = interactionMode == InteractionMode.Tv
  val visuallyFocused = focused && focusVisualsEnabled
  val focusShadowEnabled = focusVisualsEnabled && performancePolicy.focusShadowEnabled && shadowOnFocus
  val liquidGlassEnabled = performancePolicy.cinematicVisualEffectsEnabled && performancePolicy.liquidGlassCardsEnabled
  val targetFocusedBorderColor = focusedBorderColor ?: homeColors.accent
  val targetRestingBorderColor = restingBorderColor ?: homeColors.glassBorder
  val targetFocusedBackgroundColor = focusedBackgroundColor ?: homeColors.cardFocusedSurface
  val targetRestingBackgroundColor = restingBackgroundColor ?: homeColors.cardSurface
  val scale = if (performancePolicy.motionEnabled) {
    animateFloatAsState(
      targetValue = if (visuallyFocused && scaleOnFocus) focusedScale else 1f,
      animationSpec = if (visuallyFocused) {
        spring(
          dampingRatio = BiliMotion.FocusSpringDampingRatio,
          stiffness = BiliMotion.FocusSpringStiffness,
        )
      } else {
        tween(BiliMotion.FocusOutMs, easing = BiliMotion.FocusEasing)
      },
      label = "focusScale",
    ).value
  } else {
    1f
  }
  val borderWidth = if (performancePolicy.motionEnabled) {
    animateDpAsState(
      targetValue = if (visuallyFocused) focusedBorderWidth else restingBorderWidth,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusBorderWidth",
    ).value
  } else {
    if (visuallyFocused) focusedBorderWidth else restingBorderWidth
  }
  val shadowElevation = if (performancePolicy.motionEnabled) {
    animateDpAsState(
      targetValue = if (visuallyFocused && focusShadowEnabled) {
        focusedShadowElevation
      } else {
        BiliFocus.RestingShadowElevation
      },
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusShadowElevation",
    ).value
  } else {
    BiliFocus.RestingShadowElevation
  }
  val lift = if (performancePolicy.motionEnabled) {
    animateDpAsState(
      targetValue = if (visuallyFocused && scaleOnFocus) {
        focusedLift
      } else {
        BiliFocus.RestingLift
      },
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusLift",
    ).value
  } else {
    BiliFocus.RestingLift
  }
  val borderColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = if (visuallyFocused) targetFocusedBorderColor else targetRestingBorderColor,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusBorder",
    ).value
  } else {
    if (visuallyFocused) targetFocusedBorderColor else targetRestingBorderColor
  }
  val backgroundColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = if (visuallyFocused) targetFocusedBackgroundColor else targetRestingBackgroundColor,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusBackground",
    ).value
  } else {
    if (visuallyFocused) {
      targetFocusedBackgroundColor
    } else {
      targetRestingBackgroundColor
    }
  }
  val interactionSource = remember { MutableInteractionSource() }
  val shadowColor = focusedShadowColor ?: homeColors.accent.copy(alpha = BiliFocus.ShadowAlpha)

  Box(
    modifier = modifier
      .zIndex(if (visuallyFocused) BiliFocus.FocusedZIndex else 0f)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = -lift.toPx()
        this.shadowElevation = if (focusShadowEnabled) shadowElevation.toPx() else 0f
        this.shape = shape
        clip = false
        ambientShadowColor = shadowColor
        spotShadowColor = shadowColor
      }
      .biliLiquidGlassSurface(
        enabled = liquidGlassEnabled,
        shape = shape,
        surfaceColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
      )
      .then(
        if (visuallyFocused && focusedForeground != null) {
          Modifier.drawWithContent {
            drawContent()
            focusedForeground()
          }
        } else {
          Modifier
        },
      )
      .onFocusChanged { focusState ->
        val nextFocused = focusState.isFocused || focusState.hasFocus
        if (focused != nextFocused) {
          focused = nextFocused
          onFocusChanged(nextFocused)
        }
        if (focusState.isFocused) {
          onFocused()
        }
      }
      .onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && event.key.isConfirmKey) {
          onClick()
          true
        } else {
          false
        }
      }
      .focusable(interactionSource = interactionSource)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      ),
  ) {
    Box(modifier = Modifier.clip(shape)) {
      content()
    }
  }
}

private val Key.isConfirmKey: Boolean
  get() = this == Key.Enter || this == Key.NumPadEnter || this == Key.DirectionCenter
