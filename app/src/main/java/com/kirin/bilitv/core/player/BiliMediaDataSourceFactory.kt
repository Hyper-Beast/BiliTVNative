package com.kirin.bilitv.core.player

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.kirin.bilitv.core.network.BiliHeaders
import okhttp3.OkHttpClient

class BiliMediaDataSourceFactory(
  client: OkHttpClient,
  private val headers: BiliPlaybackHeaders,
) {
  private val factory = OkHttpDataSource.Factory(client)
    .setUserAgent(BiliHeaders.UserAgent)

  fun create(): DataSource.Factory {
    return factory.setDefaultRequestProperties(headers.asMap())
  }
}
