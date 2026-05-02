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
      preferences.remove(Keys.Mid)
      preferences.remove(Keys.Face)
      preferences.remove(Keys.Uname)
      preferences.remove(Keys.IsVip)
    }
  }

  private object Keys {
    val SessData = stringPreferencesKey("sessdata")
    val BiliJct = stringPreferencesKey("bili_jct")
    val Mid = longPreferencesKey("mid")
    val Face = stringPreferencesKey("face")
    val Uname = stringPreferencesKey("uname")
    val IsVip = booleanPreferencesKey("is_vip")
  }
}
