package com.kirin.bilitv.core.auth

import com.kirin.bilitv.core.network.BiliApiClient
import com.kirin.bilitv.core.network.BiliApiEndpoints
import com.kirin.bilitv.core.network.obj
import com.kirin.bilitv.core.network.rootObject
import com.kirin.bilitv.core.network.string
import com.kirin.bilitv.core.storage.WbiKeyStore
import com.kirin.bilitv.core.storage.WbiKeys

class WbiKeyRepository(
  private val apiClient: BiliApiClient,
  private val keyStore: WbiKeyStore,
) {
  suspend fun ensureKeys(sessData: String? = null): WbiKeys? {
    val cached = keyStore.load()
    if (cached?.isFresh() == true) return cached

    return try {
      val refreshed = fetchKeys(sessData)
      if (refreshed != null) {
        keyStore.save(refreshed)
        refreshed
      } else {
        cached
      }
    } catch (_: Exception) {
      cached
    }
  }

  suspend fun refreshKeys(sessData: String? = null): WbiKeys? {
    return try {
      fetchKeys(sessData)?.also { refreshed ->
        keyStore.save(refreshed)
      }
    } catch (_: Exception) {
      null
    }
  }

  private suspend fun fetchKeys(sessData: String?): WbiKeys? {
    val root = apiClient.getJson(
      url = BiliApiEndpoints.WbiNav,
      sessData = sessData,
    ).rootObject()

    val data = root.obj("data") ?: return null
    val wbiImg = data.obj("wbi_img") ?: return null
    val imgKey = extractKey(wbiImg.string("img_url"))
    val subKey = extractKey(wbiImg.string("sub_url"))

    if (imgKey.isBlank() || subKey.isBlank()) return null

    return WbiKeys(
      imgKey = imgKey,
      subKey = subKey,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  private fun extractKey(url: String): String {
    return url.substringAfterLast('/')
      .substringBefore('.')
      .trim()
  }
}
