package com.kirin.bilitv.core.player

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import java.util.Locale

data class CodecCapability(
  val supportsH264: Boolean,
  val supportsH265: Boolean,
  val supportsAv1: Boolean,
) {
  fun supports(preference: PlaybackCodecPreference): Boolean {
    return when (preference) {
      PlaybackCodecPreference.Auto -> true
      PlaybackCodecPreference.H264 -> supportsH264
      PlaybackCodecPreference.H265 -> supportsH265
      PlaybackCodecPreference.Av1 -> supportsAv1
    }
  }
}

class CodecCapabilityProbe {
  fun probe(): CodecCapability {
    val mimeTypes = decoderMimeTypes()
    val capability = CodecCapability(
      supportsH264 = "video/avc" in mimeTypes,
      supportsH265 = "video/hevc" in mimeTypes,
      supportsAv1 = "video/av01" in mimeTypes,
    )
    Log.i(
      LogTag,
      "Hardware codecs h264=${capability.supportsH264} h265=${capability.supportsH265} av1=${capability.supportsAv1}",
    )
    return capability
  }

  private fun decoderMimeTypes(): Set<String> {
    val codecInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.asSequence()
    } else {
      @Suppress("DEPRECATION")
      (0 until MediaCodecList.getCodecCount()).asSequence().map { index ->
        @Suppress("DEPRECATION")
        MediaCodecList.getCodecInfoAt(index)
      }
    }

    return codecInfos
      .filterNot(MediaCodecInfo::isEncoder)
      .filter(::isHardwareDecoderInfo)
      .flatMap { info -> info.supportedTypes.asSequence() }
      .map(String::lowercase)
      .toSet()
  }

  private fun isHardwareDecoderInfo(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      return info.isHardwareAccelerated
    }
    val codecName = info.name.lowercase(Locale.US)
    return !codecName.startsWith("omx.google.") &&
      !codecName.startsWith("c2.android.") &&
      !codecName.startsWith("c2.google.") &&
      !codecName.contains("ffmpeg") &&
      !codecName.contains("software") &&
      !codecName.contains(".sw.")
  }

  private companion object {
    const val LogTag = "BiliTVNative:Codec"
  }
}
