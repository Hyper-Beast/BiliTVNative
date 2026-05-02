package com.kirin.bilitv.core.app

import android.content.Context
import com.kirin.bilitv.core.auth.AuthRepository
import com.kirin.bilitv.core.auth.TvLoginSigner
import com.kirin.bilitv.core.auth.WbiKeyRepository
import com.kirin.bilitv.core.auth.WbiSigner
import com.kirin.bilitv.core.cache.AppCacheManager
import com.kirin.bilitv.core.network.BiliApiClient
import com.kirin.bilitv.core.network.BiliHttpClientFactory
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.core.player.CodecCapabilityProbe
import com.kirin.bilitv.core.player.DanmakuSettingsStore
import com.kirin.bilitv.core.player.PlaybackProgressStore
import com.kirin.bilitv.core.player.PlaybackRepository
import com.kirin.bilitv.core.settings.AppSettingsStore
import com.kirin.bilitv.core.storage.SearchHistoryStore
import com.kirin.bilitv.core.storage.SessionStore
import com.kirin.bilitv.core.storage.WbiKeyStore
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
  }

  val appSettingsStore: AppSettingsStore = AppSettingsStore(appContext)
  val appCacheManager: AppCacheManager = AppCacheManager(appContext)
  val searchHistoryStore: SearchHistoryStore = SearchHistoryStore(appContext)
  val sessionStore: SessionStore = SessionStore(appContext)
  val wbiKeyStore: WbiKeyStore = WbiKeyStore(appContext)
  val httpClientFactory: BiliHttpClientFactory = BiliHttpClientFactory()
  val codecCapabilityProbe: CodecCapabilityProbe = CodecCapabilityProbe()
  val playbackHttpClient = httpClientFactory.createPlaybackClient()
  val apiClient: BiliApiClient = BiliApiClient(
    client = httpClientFactory.createApiClient(),
    json = json,
  )
  val wbiSigner: WbiSigner = WbiSigner()
  val wbiKeyRepository: WbiKeyRepository = WbiKeyRepository(
    apiClient = apiClient,
    keyStore = wbiKeyStore,
  )
  val videoRepository: VideoRepository = VideoRepository(
    apiClient = apiClient,
    wbiKeyRepository = wbiKeyRepository,
    wbiSigner = wbiSigner,
    sessionStore = sessionStore,
  )
  val playbackRepository: PlaybackRepository = PlaybackRepository(
    apiClient = apiClient,
    wbiKeyRepository = wbiKeyRepository,
    wbiSigner = wbiSigner,
    sessionStore = sessionStore,
    codecCapabilityProbe = codecCapabilityProbe,
    progressStore = PlaybackProgressStore(appContext),
  )
  val danmakuSettingsStore: DanmakuSettingsStore = DanmakuSettingsStore(appContext)
  val tvLoginSigner: TvLoginSigner = TvLoginSigner()
  val authRepository: AuthRepository = AuthRepository(
    apiClient = apiClient,
    tvLoginSigner = tvLoginSigner,
    sessionStore = sessionStore,
  )
}
