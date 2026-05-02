package com.kirin.bilitv.core.cache

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppCacheManager(context: Context) {
  private val appContext = context.applicationContext

  suspend fun cacheSizeBytes(): Long {
    return withContext(Dispatchers.IO) {
      appContext.cacheDir.sizeBytes()
    }
  }

  @OptIn(ExperimentalCoilApi::class)
  suspend fun clearCache(): CacheClearResult {
    return withContext(Dispatchers.IO) {
      val cacheDir = appContext.cacheDir
      val beforeBytes = cacheDir.sizeBytes()

      appContext.imageLoader.memoryCache?.clear()
      appContext.imageLoader.diskCache?.clear()
      cacheDir.listFiles()?.forEach { child -> child.deleteRecursively() }

      CacheClearResult(clearedBytes = beforeBytes)
    }
  }
}

data class CacheClearResult(
  val clearedBytes: Long,
)

private fun File.sizeBytes(): Long {
  if (!exists()) return 0L
  if (isFile) return length()
  return listFiles()?.sumOf { child -> child.sizeBytes() } ?: 0L
}
