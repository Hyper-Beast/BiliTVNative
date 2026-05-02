package com.kirin.bilitv.core.auth

class TvLoginSigner {
  fun sign(params: Map<String, String>, timestampSeconds: Long = currentTimestampSeconds()): Map<String, String> {
    val signParams = params.toMutableMap()
    signParams["appkey"] = TvAppKey
    signParams["ts"] = timestampSeconds.toString()

    val query = signParams.toSortedMap()
      .entries
      .joinToString("&") { (key, value) -> "$key=$value" }

    signParams["sign"] = md5Hex(query + TvAppSecret)
    return signParams
  }

  private fun currentTimestampSeconds(): Long {
    return System.currentTimeMillis() / 1000L
  }

  private companion object {
    const val TvAppKey = "4409e2ce8ffd12b8"
    const val TvAppSecret = "59b43e04ad6965f34319062b478f83dd"
  }
}

