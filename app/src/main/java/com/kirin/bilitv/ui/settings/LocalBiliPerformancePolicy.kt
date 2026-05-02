package com.kirin.bilitv.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf
import com.kirin.bilitv.core.settings.AppPerformancePolicy

val LocalBiliPerformancePolicy = staticCompositionLocalOf {
  AppPerformancePolicy.Standard
}
