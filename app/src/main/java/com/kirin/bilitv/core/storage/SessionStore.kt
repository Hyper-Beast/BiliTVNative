package com.kirin.bilitv.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserSession(
  val sessData: String? = null,
  val biliJct: String? = null,
  val buvid3: String? = null,
  val buvid4: String? = null,
  val mid: Long? = null,
  val face: String? = null,
  val uname: String? = null,
  val isVip: Boolean = false,
) {
  val isLoggedIn: Boolean
    get() = !sessData.isNullOrBlank()
}

class SessionStore(private val context: Context) {
  val session: Flow<UserSession> = context.biliDataStore.data.map { preferences ->
    UserSession(
      sessData = preferences[Keys.SessData],
      biliJct = preferences[Keys.BiliJct],
      buvid3 = preferences[Keys.Buvid3],
      buvid4 = preferences[Keys.Buvid4],
      mid = preferences[Keys.Mid],
      face = preferences[Keys.Face],
      uname = preferences[Keys.Uname],
      isVip = preferences[Keys.IsVip] ?: false,
    )
  }

  val sessData: Flow<String?> = context.biliDataStore.data.map { preferences ->
    preferences[Keys.SessData]
  }

  val biliJct: Flow<String?> = context.biliDataStore.data.map { preferences ->
    preferences[Keys.BiliJct]
  }

  val buvid3: Flow<String?> = context.biliDataStore.data.map { preferences ->
    preferences[Keys.Buvid3]
  }

  val buvid4: Flow<String?> = context.biliDataStore.data.map { preferences ->
    preferences[Keys.Buvid4]
  }

  suspend fun saveSession(sessData: String?, biliJct: String?) {
    context.biliDataStore.edit { preferences ->
      if (sessData.isNullOrBlank()) {
        preferences.remove(Keys.SessData)
      } else {
        preferences[Keys.SessData] = sessData
      }

      if (biliJct.isNullOrBlank()) {
        preferences.remove(Keys.BiliJct)
      } else {
        preferences[Keys.BiliJct] = biliJct
      }
    }
  }

  suspend fun saveDeviceCookies(buvid3: String?, buvid4: String?) {
    context.biliDataStore.edit { preferences ->
      if (buvid3.isNullOrBlank()) {
        preferences.remove(Keys.Buvid3)
      } else {
        preferences[Keys.Buvid3] = buvid3
      }

      if (buvid4.isNullOrBlank()) {
        preferences.remove(Keys.Buvid4)
      } else {
        preferences[Keys.Buvid4] = buvid4
      }
    }
  }

  suspend fun saveUserProfile(mid: Long?, face: String?, uname: String?, isVip: Boolean) {
    context.biliDataStore.edit { preferences ->
      if (mid == null || mid <= 0L) {
        preferences.remove(Keys.Mid)
      } else {
        preferences[Keys.Mid] = mid
      }

      if (face.isNullOrBlank()) {
        preferences.remove(Keys.Face)
      } else {
        preferences[Keys.Face] = face
      }

      if (uname.isNullOrBlank()) {
        preferences.remove(Keys.Uname)
      } else {
        preferences[Keys.Uname] = uname
      }

      preferences[Keys.IsVip] = isVip
    }
  }

  suspend fun clearSession() {
    context.biliDataStore.edit { preferences ->
      preferences.remove(Keys.SessData)
      preferences.remove(Keys.BiliJct)
      preferences.remove(Keys.Buvid3)
      preferences.remove(Keys.Buvid4)
      preferences.remove(Keys.Mid)
      preferences.remove(Keys.Face)
      preferences.remove(Keys.Uname)
      preferences.remove(Keys.IsVip)
    }
  }

  private object Keys {
    val SessData = stringPreferencesKey("sessdata")
    val BiliJct = stringPreferencesKey("bili_jct")
    val Buvid3 = stringPreferencesKey("buvid3")
    val Buvid4 = stringPreferencesKey("buvid4")
    val Mid = longPreferencesKey("mid")
    val Face = stringPreferencesKey("face")
    val Uname = stringPreferencesKey("uname")
    val IsVip = booleanPreferencesKey("is_vip")
  }
}
