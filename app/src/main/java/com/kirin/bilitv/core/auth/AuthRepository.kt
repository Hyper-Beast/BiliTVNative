package com.kirin.bilitv.core.auth

import com.kirin.bilitv.core.network.BiliApiClient
import com.kirin.bilitv.core.network.BiliApiCodeException
import com.kirin.bilitv.core.network.BiliApiEndpoints
import com.kirin.bilitv.core.network.asObjectOrNull
import com.kirin.bilitv.core.network.int
import com.kirin.bilitv.core.network.long
import com.kirin.bilitv.core.network.obj
import com.kirin.bilitv.core.network.rootObject
import com.kirin.bilitv.core.network.string
import com.kirin.bilitv.core.storage.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray

class AuthRepository(
  private val apiClient: BiliApiClient,
  private val tvLoginSigner: TvLoginSigner,
  private val sessionStore: SessionStore,
) {
  suspend fun generateTvQrCode(): TvQrCode {
    val params = tvLoginSigner.sign(mapOf(LocalIdParam to LocalId))
    val root = apiClient.postJson(
      url = BiliApiEndpoints.TvQrCode,
      params = params,
    ).rootObject()
    root.requireCodeOrThrow("TV QR code")
    val data = root.obj("data") ?: throw BiliApiCodeException("TV QR code", -1, "Missing data")
    return TvQrCode(
      url = data.string("url"),
      authCode = data.string("auth_code"),
    )
  }

  suspend fun pollTvLogin(authCode: String): TvLoginPollResult {
    val params = tvLoginSigner.sign(
      mapOf(
        "auth_code" to authCode,
        LocalIdParam to LocalId,
      ),
    )
    val root = apiClient.postJson(
      url = BiliApiEndpoints.TvQrPoll,
      params = params,
    ).rootObject()

    return when (val code = root.int("code")) {
      0 -> {
        val data = root.obj("data") ?: throw BiliApiCodeException("TV QR poll", -1, "Missing data")
        val credentials = parseLoginCredentials(data)
        sessionStore.saveSession(
          sessData = credentials.sessData,
          biliJct = credentials.biliJct,
        )
        sessionStore.saveUserProfile(
          mid = credentials.mid,
          face = null,
          uname = null,
          isVip = false,
        )
        runCatching {
          refreshUserProfile()
        }
        TvLoginPollResult.Success
      }
      86039 -> TvLoginPollResult.Waiting
      86090 -> TvLoginPollResult.Scanned
      86038 -> TvLoginPollResult.Expired
      else -> TvLoginPollResult.Error(root.string("message").ifBlank { "code=$code" })
    }
  }

  suspend fun refreshUserProfile() {
    val session = sessionStore.session.first()
    if (!session.isLoggedIn) {
      return
    }

    val root = apiClient.getJson(
      url = BiliApiEndpoints.WbiNav,
      sessData = session.sessData,
      biliJct = session.biliJct,
    ).rootObject()
    root.requireCodeOrThrow("user nav")
    val data = root.obj("data") ?: return
    val vip = data.obj("vip")
    sessionStore.saveUserProfile(
      mid = data.long("mid").takeIf { it > 0L } ?: session.mid,
      face = data.string("face").ifBlank { session.face.orEmpty() },
      uname = data.string("uname").ifBlank { session.uname.orEmpty() },
      isVip = vip?.int("status") == 1,
    )
  }

  suspend fun clearSession() {
    sessionStore.clearSession()
  }

  private fun parseLoginCredentials(data: kotlinx.serialization.json.JsonObject): LoginCredentials {
    var sessData: String? = null
    var biliJct: String? = null
    val cookieInfo = data.obj("cookie_info")
    val cookies = cookieInfo?.get("cookies") as? JsonArray
    cookies?.forEach { element ->
      val cookie = element.asObjectOrNull() ?: return@forEach
      when (cookie.string("name")) {
        "SESSDATA" -> sessData = cookie.string("value")
        "bili_jct" -> biliJct = cookie.string("value")
      }
    }
    return LoginCredentials(
      mid = data.long("mid"),
      sessData = sessData,
      biliJct = biliJct,
    )
  }

  private fun kotlinx.serialization.json.JsonObject.requireCodeOrThrow(context: String) {
    val code = int("code")
    if (code != 0) {
      throw BiliApiCodeException(
        context = context,
        code = code,
        biliMessage = string("message"),
      )
    }
  }

  private data class LoginCredentials(
    val mid: Long,
    val sessData: String?,
    val biliJct: String?,
  )

  private companion object {
    const val LocalIdParam = "local_id"
    const val LocalId = "0"
  }
}

data class TvQrCode(
  val url: String,
  val authCode: String,
)

sealed interface TvLoginPollResult {
  data object Waiting : TvLoginPollResult
  data object Scanned : TvLoginPollResult
  data object Expired : TvLoginPollResult
  data object Success : TvLoginPollResult
  data class Error(val message: String) : TvLoginPollResult
}
