package com.kirin.bilitv.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.FormBody
import okhttp3.Request

class BiliApiClient(
  private val client: OkHttpClient,
  private val json: Json,
) {
  suspend fun getJson(
    url: String,
    params: Map<String, String> = emptyMap(),
    sessData: String? = null,
    biliJct: String? = null,
  ): JsonElement = withContext(Dispatchers.IO) {
    val httpUrlBuilder = url.toHttpUrl().newBuilder()
    params.forEach { (key, value) ->
      httpUrlBuilder.addQueryParameter(key, value)
    }

    val requestBuilder = Request.Builder()
      .url(httpUrlBuilder.build())
      .get()

    BiliHeaders.cookie(sessData, biliJct)?.let { cookie ->
      requestBuilder.header("Cookie", cookie)
    }

    client.newCall(requestBuilder.build()).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw BiliNetworkException(response.code, body)
      }
      json.parseToJsonElement(body)
    }
  }

  suspend fun getJsonWithHeaders(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): JsonElement = withContext(Dispatchers.IO) {
    val httpUrlBuilder = url.toHttpUrl().newBuilder()
    params.forEach { (key, value) ->
      httpUrlBuilder.addQueryParameter(key, value)
    }

    val requestBuilder = Request.Builder()
      .url(httpUrlBuilder.build())
      .get()

    headers.forEach { (key, value) ->
      requestBuilder.header(key, value)
    }

    client.newCall(requestBuilder.build()).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw BiliNetworkException(response.code, body)
      }
      json.parseToJsonElement(body)
    }
  }

  suspend fun getBytes(
    url: String,
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
  ): ByteArray = withContext(Dispatchers.IO) {
    val httpUrlBuilder = url.toHttpUrl().newBuilder()
    params.forEach { (key, value) ->
      httpUrlBuilder.addQueryParameter(key, value)
    }

    val requestBuilder = Request.Builder()
      .url(httpUrlBuilder.build())
      .get()

    headers.forEach { (key, value) ->
      requestBuilder.header(key, value)
    }

    client.newCall(requestBuilder.build()).execute().use { response ->
      val body = response.body?.bytes() ?: ByteArray(0)
      if (!response.isSuccessful) {
        throw BiliNetworkException(response.code, body.decodeToString())
      }
      body
    }
  }

  suspend fun postJson(
    url: String,
    params: Map<String, String> = emptyMap(),
    sessData: String? = null,
    biliJct: String? = null,
  ): JsonElement = withContext(Dispatchers.IO) {
    val httpUrlBuilder = url.toHttpUrl().newBuilder()
    params.forEach { (key, value) ->
      httpUrlBuilder.addQueryParameter(key, value)
    }

    val requestBuilder = Request.Builder()
      .url(httpUrlBuilder.build())
      .post(FormBody.Builder().build())

    BiliHeaders.cookie(sessData, biliJct)?.let { cookie ->
      requestBuilder.header("Cookie", cookie)
    }

    client.newCall(requestBuilder.build()).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw BiliNetworkException(response.code, body)
      }
      json.parseToJsonElement(body)
    }
  }

  suspend fun postFormJson(
    url: String,
    params: Map<String, String> = emptyMap(),
    sessData: String? = null,
    biliJct: String? = null,
  ): JsonElement = withContext(Dispatchers.IO) {
    val formBuilder = FormBody.Builder()
    params.forEach { (key, value) ->
      formBuilder.add(key, value)
    }

    val requestBuilder = Request.Builder()
      .url(url)
      .post(formBuilder.build())

    BiliHeaders.cookie(sessData, biliJct)?.let { cookie ->
      requestBuilder.header("Cookie", cookie)
    }

    client.newCall(requestBuilder.build()).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw BiliNetworkException(response.code, body)
      }
      json.parseToJsonElement(body)
    }
  }
}

class BiliNetworkException(
  val statusCode: Int,
  val responseBody: String,
) : Exception("Bilibili request failed with status $statusCode")

class BiliApiCodeException(
  val context: String,
  val code: Int,
  val biliMessage: String,
) : Exception("Bilibili $context failed with code $code: $biliMessage")

internal fun kotlinx.serialization.json.JsonObject.requireBiliCodeOk(context: String) {
  val code = int("code")
  if (code != 0) {
    throw BiliApiCodeException(
      context = context,
      code = code,
      biliMessage = string("message"),
    )
  }
}
