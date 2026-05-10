package com.kirin.bilitv.core.network

object BiliHeaders {
  const val UserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
  const val Referer = "https://www.bilibili.com/"
  const val Origin = "https://www.bilibili.com"
  const val SpaceOrigin = "https://space.bilibili.com"
  const val OmitRefererHeader = "X-BiliTV-Omit-Referer"
  const val OmitRefererValue = "1"

  fun cookie(
    sessData: String?,
    biliJct: String? = null,
    buvid3: String? = null,
    buvid4: String? = null,
    dedeUserId: Long? = null,
  ): String? {
    val parts = buildList {
      sessData?.takeIf { it.isNotBlank() }?.let { add("SESSDATA=$it") }
      biliJct?.takeIf { it.isNotBlank() }?.let { add("bili_jct=$it") }
      buvid3?.takeIf { it.isNotBlank() }?.let { add("buvid3=$it") }
      buvid4?.takeIf { it.isNotBlank() }?.let { add("buvid4=$it") }
      dedeUserId?.takeIf { it > 0L }?.let { add("DedeUserID=$it") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
  }
}
