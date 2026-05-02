package com.kirin.bilitv.core.network

object BiliHeaders {
  const val UserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
  const val Referer = "https://www.bilibili.com/"
  const val Origin = "https://www.bilibili.com"

  fun cookie(sessData: String?, biliJct: String? = null): String? {
    val parts = buildList {
      sessData?.takeIf { it.isNotBlank() }?.let { add("SESSDATA=$it") }
      biliJct?.takeIf { it.isNotBlank() }?.let { add("bili_jct=$it") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
  }
}
