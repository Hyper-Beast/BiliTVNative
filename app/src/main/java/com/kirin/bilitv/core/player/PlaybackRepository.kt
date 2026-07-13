package com.kirin.bilitv.core.player

import android.util.Log
import com.kirin.bilitv.core.auth.WbiKeyRepository
import com.kirin.bilitv.core.auth.WbiSigner
import com.kirin.bilitv.core.network.BiliApiClient
import com.kirin.bilitv.core.network.BiliApiEndpoints
import com.kirin.bilitv.core.network.BiliNumberParser
import com.kirin.bilitv.core.network.asObjectOrNull
import com.kirin.bilitv.core.network.int
import com.kirin.bilitv.core.network.long
import com.kirin.bilitv.core.network.obj
import com.kirin.bilitv.core.network.requireBiliCodeOk
import com.kirin.bilitv.core.network.rootObject
import com.kirin.bilitv.core.network.string
import com.kirin.bilitv.core.storage.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class PlaybackRepository(
  private val apiClient: BiliApiClient,
  private val wbiKeyRepository: WbiKeyRepository,
  private val wbiSigner: WbiSigner,
  private val sessionStore: SessionStore,
  private val codecCapabilityProbe: CodecCapabilityProbe,
  private val progressStore: PlaybackProgressStore,
) {
  private val videoshotRepository = VideoshotRepository(
    apiClient = apiClient,
    sessionStore = sessionStore,
  )
  private val danmakuRepository = DanmakuRepository(
    apiClient = apiClient,
    sessionStore = sessionStore,
  )
  private val commentRepository = CommentRepository(
    apiClient = apiClient,
    sessionStore = sessionStore,
  )
  private val airJumpRepository = AirJumpRepository(apiClient)

  suspend fun getPlaybackInfo(
    request: PlaybackRequest,
    codecPreference: PlaybackCodecPreference,
    qualityPreference: PlaybackQualityPreference,
  ): PlaybackInfo {
    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    val codecCapability = codecCapabilityProbe.probe()
    val effectiveCodecPreference = when {
      codecPreference == PlaybackCodecPreference.H264 -> PlaybackCodecPreference.H264
      codecCapability.supports(codecPreference) -> codecPreference
      else -> PlaybackCodecPreference.Auto
    }
    val fnval = buildFnval(
      codecPreference = effectiveCodecPreference,
      codecCapability = codecCapability,
    )
    val requestedQualityId = request.preferredQualityId ?: qualityPreference.requestedQualityId
    Log.i(
      PlaybackLogTag,
      "playurl codec requested=${codecPreference.key} effective=${effectiveCodecPreference.key} fnval=$fnval qn=$requestedQualityId",
    )
    val params = mutableMapOf(
      "bvid" to request.bvid,
      "cid" to request.cid.toString(),
      "qn" to requestedQualityId.toString(),
      "fnval" to fnval.toString(),
      "fourk" to "1",
    )
    val keys = wbiKeyRepository.ensureKeys(sessData)
    val signedParams = if (keys != null) {
      wbiSigner.sign(params, keys.imgKey, keys.subKey)
    } else {
      params
    }

    val headers = BiliPlaybackHeaders(sessData = sessData, biliJct = biliJct)
    val root = apiClient.getJsonWithHeaders(
      url = BiliApiEndpoints.PlayUrl,
      params = signedParams,
      headers = headers.asMap(),
    ).rootObject()
    root.requireBiliCodeOk("playurl")
    return parsePlaybackInfo(
      request = request,
      headers = headers,
      data = root.obj("data") ?: JsonObject(emptyMap()),
      requestedQualityId = requestedQualityId,
      codecPreference = effectiveCodecPreference,
      codecCapability = codecCapability,
    )
  }

  suspend fun resolveCid(bvid: String): Long {
    if (bvid.isBlank()) {
      return 0L
    }
    val root = apiClient.getJson(
      url = BiliApiEndpoints.View,
      params = mapOf("bvid" to bvid),
    ).rootObject()
    root.requireBiliCodeOk("view")
    val data = root.obj("data") ?: return 0L
    return data.long("cid").takeIf { it > 0L }
      ?: ((data["pages"] as? JsonArray)
        ?.firstOrNull()
        ?.asObjectOrNull()
        ?.long("cid") ?: 0L)
  }

  suspend fun getVideoMetadata(request: PlaybackRequest): PlaybackVideoMetadata {
    val root = apiClient.getJson(
      url = BiliApiEndpoints.View,
      params = mapOf("bvid" to request.bvid),
    ).rootObject()
    root.requireBiliCodeOk("view metadata")

    val data = root.obj("data") ?: JsonObject(emptyMap())
    val owner = data.obj("owner")
    val stat = data.obj("stat")
    val pages = (data["pages"] as? JsonArray)
      ?.mapNotNull { element ->
        val page = element.asObjectOrNull() ?: return@mapNotNull null
        PlaybackEpisode(
          cid = page.long("cid"),
          page = page.int("page"),
          title = page.string("part").ifBlank { page.string("page_part") },
          durationSeconds = BiliNumberParser.parseDuration(page["duration"]),
        )
      }
      ?.filter { episode -> episode.cid > 0L }
      .orEmpty()

    return PlaybackVideoMetadata(
      aid = data.long("aid"),
      bvid = data.string("bvid").ifBlank { request.bvid },
      cid = request.cid.takeIf { it > 0L }
        ?: data.long("cid").takeIf { it > 0L }
        ?: pages.firstOrNull()?.cid
        ?: 0L,
      title = data.string("title").ifBlank { request.title },
      ownerName = owner?.string("name").orEmpty(),
      ownerFace = owner?.string("face").orEmpty(),
      ownerMid = owner?.long("mid") ?: 0L,
      viewCount = BiliNumberParser.toInt(stat?.get("view")),
      danmakuCount = BiliNumberParser.toInt(stat?.get("danmaku")),
      pubdate = data.long("pubdate"),
      pages = pages,
    )
  }

  suspend fun getOnlineCount(aid: Long, cid: Long): String? {
    if (aid <= 0L || cid <= 0L) {
      return null
    }
    val sessData = sessionStore.sessData.first()
    val root = apiClient.getJson(
      url = BiliApiEndpoints.PlayerOnlineTotal,
      params = mapOf(
        "aid" to aid.toString(),
        "cid" to cid.toString(),
      ),
      sessData = sessData,
    ).rootObject()
    root.requireBiliCodeOk("player online total")

    val data = root.obj("data") ?: return null
    return data.string("total")
      .ifBlank { data.string("count") }
      .takeIf { count -> count.isNotBlank() }
  }

  suspend fun getVideoshot(bvid: String, cid: Long): VideoshotData? {
    return videoshotRepository.getVideoshot(bvid = bvid, cid = cid)
  }

  suspend fun getVideoshotImageBytes(url: String): ByteArray? {
    return videoshotRepository.getVideoshotImageBytes(url)
  }

  suspend fun getDanmaku(cid: Long): List<DanmakuEntry> {
    return danmakuRepository.getDanmaku(cid)
  }

  suspend fun getComments(aid: Long): List<PlayerComment> {
    return commentRepository.getComments(aid)
  }

  suspend fun getAirJumpSegments(bvid: String): List<AirJumpSegment> {
    return airJumpRepository.getAirJumpSegments(bvid)
  }

  suspend fun getSavedProgress(bvid: String, cid: Long): PlaybackProgress? {
    return progressStore.getProgress(bvid = bvid, cid = cid)
  }

  suspend fun getLatestSavedProgress(bvid: String): PlaybackProgress? {
    return progressStore.getLatestProgress(bvid = bvid)
  }

  suspend fun saveProgress(
    bvid: String,
    cid: Long,
    positionMs: Long,
    durationMs: Long,
  ) {
    progressStore.saveProgress(
      bvid = bvid,
      cid = cid,
      positionMs = positionMs,
      durationMs = durationMs,
    )
  }

  suspend fun reportProgress(
    bvid: String,
    cid: Long,
    progressSeconds: Int,
  ): Boolean {
    if (bvid.isBlank() || cid <= 0L) return false
    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    if (sessData.isNullOrBlank() || biliJct.isNullOrBlank()) return false

    return runCatching {
      val root = apiClient.postJson(
        url = BiliApiEndpoints.PlayerHeartbeat,
        params = mapOf(
          "bvid" to bvid,
          "cid" to cid.toString(),
          "played_time" to progressSeconds.toString(),
          "real_played_time" to progressSeconds.toString(),
          "start_ts" to (System.currentTimeMillis() / 1000L).toString(),
          "csrf" to biliJct,
        ),
        sessData = sessData,
        biliJct = biliJct,
      ).rootObject()
      root.requireBiliCodeOk("player heartbeat")
      true
    }.getOrDefault(false)
  }

  private fun parsePlaybackInfo(
    request: PlaybackRequest,
    headers: BiliPlaybackHeaders,
    data: JsonObject,
    requestedQualityId: Int,
    codecPreference: PlaybackCodecPreference,
    codecCapability: CodecCapability,
  ): PlaybackInfo {
    val dash = data.obj("dash")
    val videoTracks = (dash?.get("video") as? JsonArray)
      ?.mapNotNull { it.asObjectOrNull()?.toPlaybackTrack() }
      .orEmpty()
      .filter { track -> track.baseUrl.isNotBlank() }
    val audioTracks = (dash?.get("audio") as? JsonArray)
      ?.mapNotNull { it.asObjectOrNull()?.toPlaybackTrack() }
      .orEmpty()
      .filter { track -> track.baseUrl.isNotBlank() }
    val qualities = parseQualities(data)
    val selectedQuality = qualities.firstOrNull { quality -> quality.id == data.int("quality") }
      ?: qualities.firstOrNull()
      ?: PlaybackQuality(id = data.int("quality"), description = data.int("quality").toString())
    val acceptedVideoTracks = videoTracks.filter { track ->
      track.isPlayable(codecPreference = codecPreference, capability = codecCapability)
    }.ifEmpty {
      videoTracks.filter { track -> track.isPlayable(codecPreference = PlaybackCodecPreference.Auto, capability = codecCapability) }
    }.ifEmpty {
      videoTracks
    }
    val selectedQualityTracks = acceptedVideoTracks.filter { track -> track.id == selectedQuality.id }
      .ifEmpty { acceptedVideoTracks }
      .sortedWith(videoTrackComparator(codecPreference))
    Log.i(
      PlaybackLogTag,
      "playurl qn requested=$requestedQualityId returned=${selectedQuality.id} tracks=" +
        selectedQualityTracks.joinToString { track -> "${track.id}:${track.width}x${track.height}:${track.codecLabel()}" },
    )

    return PlaybackInfo(
      bvid = request.bvid,
      cid = request.cid,
      title = request.title,
      durationMs = (dash?.long("duration") ?: 0L) * 1000L,
      qualities = qualities,
      selectedQuality = selectedQuality,
      videoTracks = selectedQualityTracks,
      audioTracks = audioTracks.sortedByDescending(PlaybackTrack::bandwidth),
      headers = headers,
    )
  }

  private fun JsonObject.toPlaybackTrack(): PlaybackTrack {
    return PlaybackTrack(
      id = int("id"),
      baseUrl = string("baseUrl").ifBlank { string("base_url") },
      backupUrls = ((this["backupUrl"] ?: this["backup_url"]) as? JsonArray)
        ?.mapNotNull { element -> element.toString().trim('"').takeIf(String::isNotBlank) }
        .orEmpty(),
      bandwidth = int("bandwidth"),
      codecs = string("codecs"),
      width = int("width"),
      height = int("height"),
      mimeType = string("mimeType").ifBlank { string("mime_type") },
      segmentBase = PlaybackSegmentBase(
        initializationRange = obj("SegmentBase")?.rangeString("Initialization")
          ?: obj("segment_base")?.rangeString("initialization")
          ?: "0-0",
        indexRange = obj("SegmentBase")?.string("indexRange")
          ?: obj("segment_base")?.string("index_range")
          ?: "0-0",
      ),
    )
  }

  private fun JsonObject.rangeString(name: String): String? {
    return string(name).ifBlank {
      obj(name)?.string("range").orEmpty()
    }.takeIf { value -> value.isNotBlank() }
  }

  private fun parseQualities(data: JsonObject): List<PlaybackQuality> {
    val ids = (data["accept_quality"] as? JsonArray)
      ?.mapNotNull { element -> element.toString().toIntOrNull() }
      .orEmpty()
    val descriptions = (data["accept_description"] as? JsonArray)
      ?.map { element -> element.asString() }
      .orEmpty()

    return ids.mapIndexed { index, id ->
      PlaybackQuality(
        id = id,
        description = descriptions.getOrNull(index).orEmpty().ifBlank { id.toString() },
      )
    }
  }

  private fun JsonElement.asString(): String {
    return (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
  }

  private fun PlaybackTrack.isPlayable(
    codecPreference: PlaybackCodecPreference,
    capability: CodecCapability,
  ): Boolean {
    when (codecPreference) {
      PlaybackCodecPreference.H264 -> return isH264
      PlaybackCodecPreference.H265 -> return isH265
      PlaybackCodecPreference.Av1 -> return isAv1
      PlaybackCodecPreference.Auto -> Unit
    }
    return when {
      isAv1 -> capability.supportsAv1
      isH265 -> capability.supportsH265
      isH264 -> capability.supportsH264
      else -> true
    }
  }

  private fun videoTrackComparator(codecPreference: PlaybackCodecPreference): Comparator<PlaybackTrack> {
    return compareByDescending<PlaybackTrack> { track ->
      track.codecPriority(codecPreference)
    }.thenByDescending { track ->
      track.height
    }.thenByDescending { track ->
      track.bandwidth
    }
  }

  private fun PlaybackTrack.codecPriority(codecPreference: PlaybackCodecPreference): Int {
    return when (codecPreference) {
      PlaybackCodecPreference.H264 -> if (isH264) 1 else 0
      PlaybackCodecPreference.H265 -> if (isH265) 1 else 0
      PlaybackCodecPreference.Av1 -> if (isAv1) 1 else 0
      PlaybackCodecPreference.Auto -> when {
        isAv1 -> 3
        isH265 -> 2
        isH264 -> 1
        else -> 0
      }
    }
  }

  private fun PlaybackTrack.codecLabel(): String {
    return when {
      isAv1 -> "AV1"
      isH265 -> "H.265"
      isH264 -> "H.264"
      else -> codecs
    }
  }

  private fun buildFnval(codecPreference: PlaybackCodecPreference, codecCapability: CodecCapability): Int {
    var fnval = FnvalDash
    if (codecPreference != PlaybackCodecPreference.H264 && codecCapability.supportsH265) {
      fnval = fnval or FnvalH265
    }
    if (codecPreference == PlaybackCodecPreference.Auto && codecCapability.supportsAv1 ||
      codecPreference == PlaybackCodecPreference.Av1 && codecCapability.supportsAv1
    ) {
      fnval = fnval or FnvalAv1
    }
    return fnval
  }

  private companion object {
    const val FnvalDash = 16
    const val FnvalH265 = 64
    const val FnvalAv1 = 1024
    const val PlaybackLogTag = "BiliTVNative:Playback"
  }
}
