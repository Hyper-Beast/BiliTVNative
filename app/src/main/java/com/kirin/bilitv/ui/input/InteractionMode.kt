package com.kirin.bilitv.ui.input

import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

enum class InteractionMode {
  Tv,
  Touch,
}

enum class DeviceClass {
  Tv,
  Tablet,
  Phone,
}

enum class InputMode {
  Remote,
  Touch,
}

data class InteractionProfile(
  val deviceClass: DeviceClass,
  val inputMode: InputMode,
) {
  val legacyMode: InteractionMode
    get() = when (inputMode) {
      InputMode.Remote -> InteractionMode.Tv
      InputMode.Touch -> InteractionMode.Touch
    }
}

val LocalInteractionProfile = staticCompositionLocalOf {
  InteractionProfile(
    deviceClass = DeviceClass.Tv,
    inputMode = InputMode.Remote,
  )
}

val LocalInteractionMode = staticCompositionLocalOf { InteractionMode.Tv }

@Composable
fun rememberInteractionProfile(): InteractionProfile {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
  val smallestScreenWidthDp = configuration.smallestScreenWidthDp
  return remember(context, uiModeType, smallestScreenWidthDp) {
    val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val hasTouchscreen = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    resolveInteractionProfile(
      InteractionDeviceSignals(
        isTelevisionUiMode = uiModeType == Configuration.UI_MODE_TYPE_TELEVISION,
        hasLeanback = hasLeanback,
        hasTouchscreen = hasTouchscreen,
        smallestScreenWidthDp = smallestScreenWidthDp,
      ),
    )
  }
}

internal data class InteractionDeviceSignals(
  val isTelevisionUiMode: Boolean,
  val hasLeanback: Boolean,
  val hasTouchscreen: Boolean,
  val smallestScreenWidthDp: Int,
)

internal fun resolveInteractionProfile(signals: InteractionDeviceSignals): InteractionProfile {
  // Explicit TV UI mode is authoritative because some television firmware falsely reports touch support.
  val isRemoteTelevision = signals.isTelevisionUiMode ||
    (signals.hasLeanback && !signals.hasTouchscreen)
  val deviceClass = when {
    isRemoteTelevision -> DeviceClass.Tv
    signals.smallestScreenWidthDp >= TabletSmallestWidthDp -> DeviceClass.Tablet
    else -> DeviceClass.Phone
  }
  return InteractionProfile(
    deviceClass = deviceClass,
    inputMode = if (deviceClass == DeviceClass.Tv) InputMode.Remote else InputMode.Touch,
  )
}

@Composable
fun rememberInteractionMode(): InteractionMode {
  return rememberInteractionProfile().legacyMode
}

private const val TabletSmallestWidthDp = 600
