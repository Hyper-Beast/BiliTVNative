package com.kirin.bilitv.core.player

import kotlin.math.roundToInt

data class VideoshotData(
  val images: List<String>,
  val imgXLen: Int,
  val imgYLen: Int,
  val imgXSize: Int,
  val imgYSize: Int,
  val pvdataUrl: String?,
  val frameTimestamps: List<Int> = emptyList(),
) {
  val framesPerImage: Int
    get() = (imgXLen * imgYLen).coerceAtLeast(1)

  fun frameAt(positionMs: Long, durationMs: Long): VideoshotFrame? {
    if (images.isEmpty()) return null
    val frameIndex = frameIndexAt(positionMs, durationMs)
    val imageIndex = (frameIndex / framesPerImage).coerceIn(0, images.lastIndex)
    val frameInImage = frameIndex % framesPerImage
    return VideoshotFrame(
      imageUrl = images[imageIndex],
      x = (frameInImage % imgXLen.coerceAtLeast(1)) * imgXSize,
      y = (frameInImage / imgXLen.coerceAtLeast(1)) * imgYSize,
      width = imgXSize,
      height = imgYSize,
    )
  }

  fun closestTimestampMs(positionMs: Long): Long {
    if (frameTimestamps.isEmpty()) return positionMs
    val seconds = positionMs / 1000.0
    var closest = frameTimestamps.first()
    var minDiff = kotlin.math.abs(closest - seconds)
    for (index in 1 until frameTimestamps.size) {
      val timestamp = frameTimestamps[index]
      val diff = kotlin.math.abs(timestamp - seconds)
      if (diff < minDiff) {
        minDiff = diff
        closest = timestamp
      }
    }
    return closest.coerceAtLeast(0) * 1000L
  }

  private fun frameIndexAt(positionMs: Long, durationMs: Long): Int {
    val maxFrameIndex = (framesPerImage * images.size - 1).coerceAtLeast(0)
    if (frameTimestamps.size > 1) {
      val seconds = positionMs / 1000.0
      var result = frameTimestamps.size - 2
      for (index in 0 until frameTimestamps.lastIndex) {
        if (seconds >= frameTimestamps[index] && seconds < frameTimestamps[index + 1]) {
          result = index - 1
          break
        }
      }
      return result.coerceAtLeast(0).coerceAtMost(maxFrameIndex)
    }

    if (durationMs <= 0L) return 0
    val fraction = positionMs.toDouble().coerceIn(0.0, durationMs.toDouble()) / durationMs.toDouble()
    return (fraction * maxFrameIndex).roundToInt().coerceIn(0, maxFrameIndex)
  }

  companion object {
    fun parsePvdata(bytes: ByteArray): List<Int> {
      val timestamps = mutableListOf<Int>()
      var index = 0
      while (index < bytes.size - 1) {
        timestamps += ((bytes[index].toInt() and 0xff) shl 8) or (bytes[index + 1].toInt() and 0xff)
        index += 2
      }
      return timestamps
    }
  }
}

data class VideoshotFrame(
  val imageUrl: String,
  val x: Int,
  val y: Int,
  val width: Int,
  val height: Int,
)
