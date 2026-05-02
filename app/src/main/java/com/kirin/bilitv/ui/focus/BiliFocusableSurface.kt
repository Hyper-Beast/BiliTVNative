package com.kirin.bilitv.ui.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.zIndex
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliFocus
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliRadius

@Composable
fun BiliFocusableSurface(
  modifier: Modifier = Modifier,
  scaleOnFocus: Boolean = true,
  shadowOnFocus: Boolean = true,
  shape: Shape = RoundedCornerShape(BiliRadius.Card),
  onClick: () -> Unit = {},
  onFocused: () -> Unit = {},
  onFocusChanged: (Boolean) -> Unit = {},
  content: @Composable () -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val performancePolicy = LocalBiliPerformancePolicy.current
  val focusShadowEnabled = performancePolicy.focusShadowEnabled && shadowOnFocus
  val scale = if (performancePolicy.motionEnabled) {
    animateFloatAsState(
      targetValue = if (focused && scaleOnFocus) BiliFocus.CardScale else 1f,
      animationSpec = if (focused) {
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
      targetValue = if (focused) BiliFocus.BorderWidth else BiliFocus.RestingBorderWidth,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusBorderWidth",
    ).value
  } else {
    if (focused) BiliFocus.BorderWidth else BiliFocus.RestingBorderWidth
  }
  val shadowElevation = if (performancePolicy.motionEnabled) {
    animateDpAsState(
      targetValue = if (focused && focusShadowEnabled) {
        BiliFocus.ShadowElevation
      } else {
        BiliFocus.RestingShadowElevation
      },
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusShadowElevation",
    ).value
  } else {
    BiliFocus.RestingShadowElevation
  }
  val borderColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = if (focused) BiliColors.BiliPink else BiliColors.Transparent,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusBorder",
    ).value
  } else {
    if (focused) BiliColors.BiliPink else BiliColors.Transparent
  }
  val backgroundColor = if (performancePolicy.motionEnabled) {
    animateColorAsState(
      targetValue = if (focused) BiliColors.SurfaceSelected else BiliColors.Surface,
      animationSpec = tween(BiliMotion.FocusMs, easing = BiliMotion.FocusEasing),
      label = "focusBackground",
    ).value
  } else {
    if (focused) {
      BiliColors.SurfaceSelected
    } else {
      BiliColors.Surface
    }
  }
  val interactionSource = remember { MutableInteractionSource() }
  val shadowColor = BiliColors.BiliPink.copy(alpha = BiliFocus.ShadowAlpha)

  Box(
    modifier = modifier
      .zIndex(if (focused) BiliFocus.FocusedZIndex else 0f)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.shadowElevation = if (focusShadowEnabled) shadowElevation.toPx() else 0f
        this.shape = shape
        clip = false
        ambientShadowColor = shadowColor
        spotShadowColor = shadowColor
      }
      .clip(shape)
      .background(backgroundColor)
      .border(BorderStroke(borderWidth, borderColor), shape)
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
    content()
  }
}

private val Key.isConfirmKey: Boolean
  get() = this == Key.Enter || this == Key.NumPadEnter || this == Key.DirectionCenter
