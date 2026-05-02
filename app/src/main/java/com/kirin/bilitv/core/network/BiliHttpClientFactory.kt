package com.kirin.bilitv.core.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor

class BiliHttpClientFactory {
  fun createApiClient(): OkHttpClient {
    return baseBuilder()
      .addInterceptor(BrotliInterceptor)
      .addInterceptor { chain ->
        val request = chain.request()
        val enriched = request.newBuilder()
          .header("User-Agent", request.header("User-Agent") ?: BiliHeaders.UserAgent)
          .header("Referer", request.header("Referer") ?: BiliHeaders.Referer)
          .build()
        chain.proceed(enriched)
      }
      .build()
  }

  fun createPlaybackClient(): OkHttpClient {
    return baseBuilder()
      .addInterceptor { chain ->
        val request = chain.request()
        val enriched = request.newBuilder()
          .header("User-Agent", request.header("User-Agent") ?: BiliHeaders.UserAgent)
          .header("Referer", request.header("Referer") ?: BiliHeaders.Referer)
          .header("Origin", request.header("Origin") ?: BiliHeaders.Origin)
          .build()
        chain.proceed(enriched)
      }
      .build()
  }

  private fun baseBuilder(): OkHttpClient.Builder {
    return OkHttpClient.Builder()
      .connectTimeout(NetworkTimeoutSeconds, TimeUnit.SECONDS)
      .readTimeout(NetworkTimeoutSeconds, TimeUnit.SECONDS)
      .writeTimeout(NetworkTimeoutSeconds, TimeUnit.SECONDS)
  }

  private companion object {
    const val NetworkTimeoutSeconds = 15L
  }
}
