package com.kirin.bilitv.core.player

import com.kirin.bilitv.core.network.BiliHeaders

data class BiliPlaybackHeaders(
  val sessData: String?,
  val biliJct: String?,
) {
  val cookie: String?
    get() = BiliHeaders.cookie(sessData, biliJct)

  fun asMap(includeCookie: Boolean = true): Map<String, String> {
    return buildMap {
      put("User-Agent", BiliHeaders.UserAgent)
      put("Referer", BiliHeaders.Referer)
      put("Origin", BiliHeaders.Origin)
      if (includeCookie) {
        cookie?.let { value -> put("Cookie", value) }
      }
    }
  }
}
