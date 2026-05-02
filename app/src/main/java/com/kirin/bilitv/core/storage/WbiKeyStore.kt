package com.kirin.bilitv.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class WbiKeys(
  val imgKey: String,
  val subKey: String,
  val updatedAtMillis: Long,
) {
  fun isFresh(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return nowMillis - updatedAtMillis < FreshWindowMillis
  }

  private companion object {
    const val FreshWindowMillis = 120L * 60L * 1000L
  }
}

class WbiKeyStore(private val context: Context) {
  suspend fun load(): WbiKeys? {
    return context.biliDataStore.data.map { preferences ->
      val imgKey = preferences[Keys.ImgKey]
      val subKey = preferences[Keys.SubKey]
      val updatedAtMillis = preferences[Keys.UpdatedAtMillis]
      if (imgKey.isNullOrBlank() || subKey.isNullOrBlank() || updatedAtMillis == null) {
        null
      } else {
        WbiKeys(imgKey, subKey, updatedAtMillis)
      }
    }.first()
  }

  suspend fun save(keys: WbiKeys) {
    context.biliDataStore.edit { preferences ->
      preferences[Keys.ImgKey] = keys.imgKey
      preferences[Keys.SubKey] = keys.subKey
      preferences[Keys.UpdatedAtMillis] = keys.updatedAtMillis
    }
  }

  private object Keys {
    val ImgKey = stringPreferencesKey("wbi_img_key")
    val SubKey = stringPreferencesKey("wbi_sub_key")
    val UpdatedAtMillis = longPreferencesKey("wbi_keys_time")
  }
}

