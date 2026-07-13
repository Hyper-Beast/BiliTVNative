package com.kirin.bilitv

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kirin.bilitv.ui.shell.BiliTvApp
import com.kirin.bilitv.ui.theme.BiliTvTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureImmersiveWindow()
    val appContainer = (application as BiliTvApplication).appContainer
    setContent {
      BiliTvTheme {
        BiliTvApp(
          videoRepository = appContainer.videoRepository,
          playbackRepository = appContainer.playbackRepository,
          danmakuSettingsStore = appContainer.danmakuSettingsStore,
          playbackHttpClient = appContainer.playbackHttpClient,
          codecCapabilityProbe = appContainer.codecCapabilityProbe,
          authRepository = appContainer.authRepository,
          appSettingsStore = appContainer.appSettingsStore,
          appCacheManager = appContainer.appCacheManager,
          searchHistoryStore = appContainer.searchHistoryStore,
          sessionStore = appContainer.sessionStore,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    configureImmersiveWindow()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      configureImmersiveWindow()
    }
  }

  @Suppress("DEPRECATION")
  private fun configureImmersiveWindow() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    window.decorView.systemUiVisibility =
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isStatusBarContrastEnforced = false
      window.isNavigationBarContrastEnforced = false
    }
    WindowCompat.getInsetsController(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      hide(WindowInsetsCompat.Type.systemBars())
    }
  }
}
