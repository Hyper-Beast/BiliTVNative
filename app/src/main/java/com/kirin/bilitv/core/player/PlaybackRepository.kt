package com.kirin.bilitv.core.player

import android.graphics.Color
import android.util.Log
import android.util.Xml
import com.kirin.bilitv.core.auth.WbiKeyRepository
import com.kirin.bilitv.core.auth.WbiSigner
import com.kirin.bilitv.core.network.BiliApiClient
import com.kirin.bilitv.core.network.BiliApiEndpoints
import com.kirin.bilitv.core.network.BiliHeaders
import com.kirin.bilitv.core.network.BiliNetworkException
import com.kirin.bilitv.core.network.BiliNumberParser
import com.kirin.bilitv.core.network.asObjectOrNull
import com.kirin.bilitv.core.network.int
import com.kirin.bilitv.core.network.long
import com.kirin.bilitv.core.network.obj
import com.kirin.bilitv.core.network.requireBiliCodeOk
import com.kirin.bilitv.core.network.rootObject
import com.kirin.bilitv.core.network.string
import com.kirin.bilitv.core.storage.SessionStore
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.xmlpull.v1.XmlPullParser

class PlaybackRepository(
  private val apiClient: BiliApiClient,
  private val wbiKeyRepository: WbiKeyRepository,
  private val wbiSigner: WbiSigner,
  private val sessionStore: SessionStore,
  private val codecCapabilityProbe: CodecCapabilityProbe,
  private val progressStore: PlaybackProgressStore,
) {
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
    if (bvid.isBlank()) return null
    val root = apiClient.getJson(
      url = BiliApiEndpoints.PlayerVideoshot,
      params = buildMap {
        put("bvid", bvid)
        if (cid > 0L) {
          put("cid", cid.toString())
        }
      },
    ).rootObject()
    root.requireBiliCodeOk("player videoshot")

    val data = root.obj("data") ?: return null
    val images = (data["image"] as? JsonArray)
      ?.map { element -> element.asString().fixResourceUrl() }
      ?.filter(String::isNotBlank)
      .orEmpty()
    if (images.isEmpty()) return null

    val pvdataUrl = data.string("pvdata").fixResourceUrl().takeIf(String::isNotBlank)
    val timestamps = pvdataUrl
      ?.let { url -> runCatching { VideoshotData.parsePvdata(apiClient.getBytes(url)) }.getOrDefault(emptyList()) }
      .orEmpty()

    return VideoshotData(
      images = images,
      imgXLen = data.int("img_x_len").takeIf { it > 0 } ?: DefaultVideoshotColumns,
      imgYLen = data.int("img_y_len").takeIf { it > 0 } ?: DefaultVideoshotRows,
      imgXSize = data.int("img_x_size").takeIf { it > 0 } ?: DefaultVideoshotWidth,
      imgYSize = data.int("img_y_size").takeIf { it > 0 } ?: DefaultVideoshotHeight,
      pvdataUrl = pvdataUrl,
      frameTimestamps = timestamps,
    )
  }

  suspend fun getVideoshotImageBytes(url: String): ByteArray? {
    if (url.isBlank()) return null
    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    return apiClient.getBytes(
      url = url,
      headers = buildMap {
        put("User-Agent", BiliHeaders.UserAgent)
        put("Referer", BiliHeaders.Referer)
        put("Origin", BiliHeaders.Origin)
        put("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
        BiliHeaders.cookie(sessData, biliJct)?.let { cookie -> put("Cookie", cookie) }
      },
    ).takeIf { bytes -> bytes.isNotEmpty() }
  }

  suspend fun getDanmaku(cid: Long): List<DanmakuEntry> {
    if (cid <= 0L) return emptyList()
    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    val headers = buildDanmakuHeaders(sessData = sessData, biliJct = biliJct)
    return withContext(Dispatchers.IO) {
      var primaryError: Throwable? = null
      val primaryEntries = runCatching {
        val bytes = apiClient.getBytes(
          url = BiliApiEndpoints.PlayerDanmaku,
          params = mapOf(
            "type" to "1",
            "oid" to cid.toString(),
          ),
          headers = headers,
        )
        parseDanmakuXml(decodeDanmakuXmlBytes(bytes))
      }.onFailure { error ->
        primaryError = error
      }.getOrDefault(emptyList())
      if (primaryEntries.isNotEmpty()) {
        return@withContext primaryEntries
      }

      runCatching {
        val bytes = apiClient.getBytes(
          url = BiliApiEndpoints.legacyDanmaku(cid),
          headers = headers,
        )
        parseDanmakuXml(decodeDanmakuXmlBytes(bytes))
      }.getOrElse { error ->
        primaryError?.let { throw it }
        throw error
      }
    }
  }

  suspend fun getAirJumpSegments(bvid: String): List<AirJumpSegment> {
    if (bvid.isBlank()) return emptyList()
    val url = BiliApiEndpoints.SponsorBlockSkipSegments.toHttpUrl().newBuilder()
      .addQueryParameter("videoID", bvid)
      .apply {
        AirJumpCategories.forEach { category ->
          addQueryParameter("category", category)
        }
      }
      .build()
      .toString()

    val root = try {
      apiClient.getJson(url = url)
    } catch (error: BiliNetworkException) {
      if (error.statusCode == 404) return emptyList()
      throw error
    }
    return (root as? JsonArray)
      ?.mapNotNull { element -> element.asObjectOrNull()?.toAirJumpSegment() }
      ?.filter { segment -> segment.durationMs > 0L }
      ?.sortedBy(AirJumpSegment::startMs)
      .orEmpty()
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

  private fun JsonObject.toAirJumpSegment(): AirJumpSegment? {
    val segmentArray = this["segment"] as? JsonArray ?: return null
    val startSeconds = segmentArray.getOrNull(0)?.asString()?.toDoubleOrNull() ?: return null
    val endSeconds = segmentArray.getOrNull(1)?.asString()?.toDoubleOrNull() ?: return null
    val category = string("category").ifBlank { "unknown" }
    val startMs = (startSeconds * 1000.0).toLong().coerceAtLeast(0L)
    val endMs = (endSeconds * 1000.0).toLong().coerceAtLeast(0L)
    if (endMs <= startMs) return null
    return AirJumpSegment(
      id = string("UUID").ifBlank { "$category:$startMs:$endMs" },
      category = category,
      startMs = startMs,
      endMs = endMs,
    )
  }

  private fun parseDanmakuXml(bytes: ByteArray): List<DanmakuEntry> {
    if (bytes.isEmpty()) return emptyList()
    val parser = Xml.newPullParser()
    val entries = mutableListOf<DanmakuEntry>()
    ByteArrayInputStream(bytes).use { stream ->
      parser.setInput(stream, null)
      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT && entries.size < MaxDanmakuEntries) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "d") {
          val params = parser.getAttributeValue(null, "p").orEmpty()
          val text = parser.nextText().trim()
          parseDanmakuEntry(params, text)?.let(entries::add)
        }
        eventType = parser.next()
      }
    }
    return entries.sortedBy(DanmakuEntry::showAtMs)
  }

  private fun buildDanmakuHeaders(sessData: String?, biliJct: String?): Map<String, String> {
    return buildMap {
      put("User-Agent", BiliHeaders.UserAgent)
      put("Referer", BiliHeaders.Referer)
      put("Origin", BiliHeaders.Origin)
      put("Accept", "text/xml,application/xml,*/*;q=0.8")
      put("Accept-Encoding", "gzip, deflate")
      BiliHeaders.cookie(sessData, biliJct)?.let { cookie -> put("Cookie", cookie) }
    }
  }

  private fun decodeDanmakuXmlBytes(bytes: ByteArray): ByteArray {
    if (bytes.isEmpty()) return bytes
    return when {
      bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() -> {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { stream -> stream.readBytes() }
      }
      bytes[0] == 0x78.toByte() -> {
        InflaterInputStream(ByteArrayInputStream(bytes)).use { stream -> stream.readBytes() }
      }
      bytes[0] == '<'.code.toByte() -> bytes
      else -> runCatching {
        val inflater = Inflater(true)
        try {
          InflaterInputStream(ByteArrayInputStream(bytes), inflater).use { stream -> stream.readBytes() }
        } finally {
          inflater.end()
        }
      }.getOrDefault(bytes)
    }
  }

  private fun parseDanmakuEntry(params: String, text: String): DanmakuEntry? {
    if (text.isBlank()) return null
    val parts = params.split(',')
    if (parts.size < 4) return null
    val showAtMs = ((parts[0].toDoubleOrNull() ?: return null) * 1000.0).toLong()
    val mode = when (parts[1].toIntOrNull()) {
      1, 2, 3, 6 -> DanmakuMode.Scroll
      4 -> DanmakuMode.Bottom
      5 -> DanmakuMode.Top
      else -> return null
    }
    return DanmakuEntry(
      showAtMs = showAtMs.coerceAtLeast(0L),
      text = text,
      mode = mode,
      color = parseDanmakuColor(parts[3]),
    )
  }

  private fun parseDanmakuColor(value: String): Int {
    val rgb = value.toLongOrNull()?.coerceIn(0L, 0xFFFFFFL)?.toInt() ?: return Color.WHITE
    return Color.rgb(
      (rgb shr 16) and 0xFF,
      (rgb shr 8) and 0xFF,
      rgb and 0xFF,
    )
  }

  private fun String.fixResourceUrl(): String {
    return when {
      startsWith("//") -> "https:$this"
      startsWith("http://") -> "https://${removePrefix("http://")}"
      else -> this
    }
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
    const val DefaultVideoshotColumns = 10
    const val DefaultVideoshotRows = 10
    const val DefaultVideoshotWidth = 160
    const val DefaultVideoshotHeight = 90
    const val MaxDanmakuEntries = 5000
    const val FnvalDash = 16
    const val FnvalH265 = 64
    const val FnvalAv1 = 1024
    const val PlaybackLogTag = "BiliTVNative:Playback"
    val AirJumpCategories = listOf("sponsor", "intro", "outro", "interaction", "selfpromo")
  }
}
