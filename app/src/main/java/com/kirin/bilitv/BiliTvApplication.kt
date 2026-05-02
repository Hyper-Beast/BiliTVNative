package com.kirin.bilitv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kirin.bilitv.core.app.AppContainer

class BiliTvApplication : Application(), ImageLoaderFactory {
  lateinit var appContainer: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    appContainer = AppContainer(this)
  }

  override fun newImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(ImageMemoryCachePercent)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve(ImageDiskCacheDirectory))
          .maxSizeBytes(ImageDiskCacheMaxBytes)
          .build()
      }
      .crossfade(false)
      .build()
  }

  private companion object {
    const val ImageMemoryCachePercent = 0.20
    const val ImageDiskCacheDirectory = "image_cache"
    const val ImageDiskCacheMaxBytes = 128L * 1024L * 1024L
  }
}
