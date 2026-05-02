package com.kirin.bilitv.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import com.kirin.bilitv.core.image.buildVideoThumbnailRequest
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.settings.LocalBiliPerformancePolicy

private const val StandardVideoThumbnailPrefetchCount = 16

@Composable
fun VideoThumbnailPrefetcher(
  videos: List<VideoSummary>,
  focusedIndex: Int,
  prefetchCount: Int = StandardVideoThumbnailPrefetchCount,
) {
  val performancePolicy = LocalBiliPerformancePolicy.current
  val effectivePrefetchCount = prefetchCount.coerceAtMost(performancePolicy.videoThumbnailPrefetchCount)
  if (videos.isEmpty() || effectivePrefetchCount <= 0) {
    return
  }

  val context = LocalContext.current
  val imageLoader = context.imageLoader
  val thumbnailWidthPx = performancePolicy.videoThumbnailWidthPx
  val thumbnailHeightPx = performancePolicy.videoThumbnailHeightPx
  val thumbnailRgb565Enabled = performancePolicy.videoThumbnailRgb565Enabled
  val imageMemoryCacheEnabled = performancePolicy.imageMemoryCacheEnabled
  val prefetchedUrls = remember(
    videos,
    thumbnailWidthPx,
    thumbnailHeightPx,
    thumbnailRgb565Enabled,
    imageMemoryCacheEnabled,
  ) {
    mutableStateSetOf<String>()
  }
  val startIndex = focusedIndex.coerceAtLeast(0)
  val endIndex = (startIndex + effectivePrefetchCount - 1).coerceAtMost(videos.size - 1)

  LaunchedEffect(
    videos,
    startIndex,
    endIndex,
    thumbnailWidthPx,
    thumbnailHeightPx,
    thumbnailRgb565Enabled,
    imageMemoryCacheEnabled,
  ) {
    for (index in startIndex..endIndex) {
      val url = videos[index].pic
      if (url.isBlank() || !prefetchedUrls.add(url)) {
        continue
      }
      imageLoader.enqueue(
        buildVideoThumbnailRequest(
          context = context,
          url = url,
          widthPx = thumbnailWidthPx,
          heightPx = thumbnailHeightPx,
          allowRgb565 = thumbnailRgb565Enabled,
          memoryCacheEnabled = imageMemoryCacheEnabled,
        ),
      )
    }
  }
}
