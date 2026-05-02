package com.kirin.bilitv.ui.player

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bytedance.danmaku.render.engine.DanmakuView
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER
import com.kirin.bilitv.core.i18n.ChineseTextConverter
import com.kirin.bilitv.core.player.DanmakuEntry
import com.kirin.bilitv.core.player.DanmakuSettings
import com.kirin.bilitv.core.player.DanmakuMode
import com.kirin.bilitv.ui.i18n.LocalChineseTextConverter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun PlayerDanmakuLayer(
  entries: List<DanmakuEntry>,
  settings: DanmakuSettings,
  positionMs: Long,
  isPlaying: Boolean,
  playbackSpeed: Float,
  lowSpecMode: Boolean,
  modifier: Modifier = Modifier,
) {
  if (!settings.enabled || entries.isEmpty()) {
    return
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val textConverter = LocalChineseTextConverter.current
    val fontSizePx = with(density) { settings.fontSize.sp.toPx() }
    val viewportHeightPx = with(density) { maxHeight.toPx() }
    val danmakuData = remember(entries, settings, fontSizePx, lowSpecMode, textConverter) {
      entries.toTextData(
        settings = settings,
        fontSizePx = fontSizePx,
        maxEntries = if (lowSpecMode) LowSpecMaxDanmakuEntries else StandardMaxDanmakuEntries,
        textConverter = textConverter,
      )
    }
    var danmakuView by remember { mutableStateOf<DanmakuView?>(null) }
    var lastTimelinePositionMs by remember(entries) { mutableLongStateOf(positionMs) }

    AndroidView(
      factory = { context ->
        DanmakuView(context).apply {
          isFocusable = false
          isFocusableInTouchMode = false
          importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
          setBackgroundColor(Color.TRANSPARENT)
          setLayerType(View.LAYER_TYPE_HARDWARE, null)
          controller.config.debug.logLevel = Log.WARN
          danmakuView = this
        }
      },
      update = { view ->
        danmakuView = view
        view.applyDanmakuConfig(
          settings = settings,
          fontSizePx = fontSizePx,
          viewportHeightPx = viewportHeightPx,
          playbackSpeed = playbackSpeed,
          lowSpecMode = lowSpecMode,
        )
      },
      modifier = Modifier.fillMaxSize(),
    )

    LaunchedEffect(danmakuView, danmakuData) {
      val view = danmakuView ?: return@LaunchedEffect
      view.controller.stop()
      view.controller.setData(danmakuData)
      lastTimelinePositionMs = positionMs
      if (isPlaying && danmakuData.isNotEmpty()) {
        view.controller.start(positionMs)
      }
    }

    LaunchedEffect(danmakuView, isPlaying, danmakuData) {
      val view = danmakuView ?: return@LaunchedEffect
      if (isPlaying && danmakuData.isNotEmpty()) {
        view.controller.start(positionMs)
      } else {
        view.controller.pause()
      }
    }

    LaunchedEffect(danmakuView, positionMs, isPlaying) {
      val view = danmakuView ?: return@LaunchedEffect
      val positionJumped = positionMs < lastTimelinePositionMs ||
        abs(positionMs - lastTimelinePositionMs) > DanmakuSeekJumpThresholdMs
      lastTimelinePositionMs = positionMs
      if (positionJumped) {
        view.controller.clear()
        if (isPlaying && danmakuData.isNotEmpty()) {
          view.controller.pause()
          view.controller.start(positionMs)
        }
      }
    }

    DisposableEffect(danmakuView) {
      onDispose {
        danmakuView?.controller?.stop()
      }
    }
  }
}

private fun List<DanmakuEntry>.toTextData(
  settings: DanmakuSettings,
  fontSizePx: Float,
  maxEntries: Int,
  textConverter: ChineseTextConverter,
): List<TextData> {
  return asSequence()
    .filter { entry ->
      when (entry.mode) {
        DanmakuMode.Scroll -> true
        DanmakuMode.Top -> settings.allowTop
        DanmakuMode.Bottom -> settings.allowBottom
      }
    }
    .take(maxEntries)
    .map { entry ->
      TextData().apply {
        text = textConverter.convert(entry.text)
        showAtTime = entry.showAtMs
        layerType = entry.mode.layerType
        textSize = fontSizePx
        textColor = entry.color
        textStrokeWidth = (fontSizePx * 0.08f).coerceAtLeast(1.5f)
        textStrokeColor = Color.argb(190, 0, 0, 0)
        includeFontPadding = false
      }
    }
    .toList()
}

private fun DanmakuView.applyDanmakuConfig(
  settings: DanmakuSettings,
  fontSizePx: Float,
  viewportHeightPx: Float,
  playbackSpeed: Float,
  lowSpecMode: Boolean,
) {
  val lineHeightPx = (fontSizePx * 1.35f).coerceAtLeast(32f)
  val lineMarginPx = (fontSizePx * 0.32f).coerceAtLeast(6f)
  val availableHeightPx = (viewportHeightPx * settings.area.coerceIn(0.25f, 1f)).coerceAtLeast(lineHeightPx)
  val lineCount = (availableHeightPx / (lineHeightPx + lineMarginPx))
    .roundToInt()
    .coerceIn(1, if (lowSpecMode) 5 else 10)
  val fixedLineCount = (lineCount / 3).coerceIn(1, if (lowSpecMode) 1 else 3)
  val baseMoveTimeMs = ((13 - settings.speed.coerceIn(3, 7)) * 1000L).coerceIn(6000L, 10000L)
  val effectivePlaybackSpeed = playbackSpeed.coerceIn(0.5f, 2.0f)

  controller.config.common.alpha = (settings.opacity.coerceIn(0.1f, 1f) * 255f).roundToInt()
  controller.config.common.playSpeed = (effectivePlaybackSpeed * 100f).roundToInt().coerceAtLeast(50)
  controller.config.common.topVisible = settings.allowTop
  controller.config.common.bottomVisible = settings.allowBottom
  controller.config.common.pauseInvalidateWhenBlank = true

  controller.config.text.size = fontSizePx
  controller.config.text.color = Color.WHITE
  controller.config.text.strokeWidth = (fontSizePx * 0.08f).coerceAtLeast(1.5f)
  controller.config.text.strokeColor = Color.argb(190, 0, 0, 0)
  controller.config.text.includeFontPadding = false

  controller.config.scroll.moveTime = (baseMoveTimeMs / effectivePlaybackSpeed).toLong().coerceAtLeast(3500L)
  controller.config.scroll.lineHeight = lineHeightPx
  controller.config.scroll.lineMargin = lineMarginPx
  controller.config.scroll.lineCount = lineCount
  controller.config.scroll.marginTop = 0f
  controller.config.scroll.itemMargin = fontSizePx
  controller.config.scroll.bufferSize = if (lowSpecMode) 3 else 8

  controller.config.top.lineHeight = lineHeightPx
  controller.config.top.lineMargin = lineMarginPx
  controller.config.top.lineCount = fixedLineCount
  controller.config.top.marginTop = 0f
  controller.config.top.bufferSize = if (lowSpecMode) 2 else 4

  controller.config.bottom.lineHeight = lineHeightPx
  controller.config.bottom.lineMargin = lineMarginPx
  controller.config.bottom.lineCount = fixedLineCount
  controller.config.bottom.marginBottom = lineMarginPx
  controller.config.bottom.bufferSize = if (lowSpecMode) 2 else 4
}

private val DanmakuMode.layerType: Int
  get() = when (this) {
    DanmakuMode.Scroll -> LAYER_TYPE_SCROLL
    DanmakuMode.Top -> LAYER_TYPE_TOP_CENTER
    DanmakuMode.Bottom -> LAYER_TYPE_BOTTOM_CENTER
  }

private const val DanmakuSeekJumpThresholdMs = 3_000L
private const val StandardMaxDanmakuEntries = 5000
private const val LowSpecMaxDanmakuEntries = 2500
