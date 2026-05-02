package com.kirin.bilitv.core.auth

import java.net.URLEncoder

class WbiSigner {
  fun sign(
    params: Map<String, String>,
    imgKey: String,
    subKey: String,
    timestampSeconds: Long = currentTimestampSeconds(),
  ): Map<String, String> {
    val mixinKey = getMixinKey(imgKey, subKey)
    val signed = params.toMutableMap()
    signed["wts"] = timestampSeconds.toString()

    val filtered = signed.mapValues { (_, value) ->
      value.replace(ForbiddenCharacters, "")
    }.toSortedMap()

    val query = filtered.entries.joinToString("&") { (key, value) ->
      "$key=${encodeComponent(value)}"
    }

    filtered["w_rid"] = md5Hex(query + mixinKey)
    return filtered
  }

  private fun getMixinKey(imgKey: String, subKey: String): String {
    val raw = imgKey + subKey
    return MixinKeyEncTab
      .take(MixinKeyLength)
      .mapNotNull { index -> raw.getOrNull(index) }
      .joinToString(separator = "")
  }

  private fun encodeComponent(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
      .replace("+", "%20")
      .replace("%7E", "~")
  }

  private fun currentTimestampSeconds(): Long {
    return System.currentTimeMillis() / 1000L
  }

  private companion object {
    const val MixinKeyLength = 32
    val ForbiddenCharacters = Regex("[!'()*]")
    val MixinKeyEncTab = intArrayOf(
      46, 47, 18, 2, 53, 8, 23, 32,
      15, 50, 10, 31, 58, 3, 45, 35,
      27, 43, 5, 49, 33, 9, 42, 19,
      29, 28, 14, 39, 12, 38, 41, 13,
      37, 48, 7, 16, 24, 55, 40, 61,
      26, 17, 0, 1, 60, 51, 30, 4,
      22, 25, 54, 21, 56, 59, 6, 63,
      57, 62, 11, 36, 20, 34, 44, 52,
    )
  }
}

