package com.kirin.bilitv.core.network

import android.util.Log
import com.kirin.bilitv.core.auth.WbiKeyRepository
import com.kirin.bilitv.core.auth.WbiSigner
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.storage.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

class VideoRepository(
  private val apiClient: BiliApiClient,
  private val wbiKeyRepository: WbiKeyRepository,
  private val wbiSigner: WbiSigner,
  private val sessionStore: SessionStore,
) {
  suspend fun getHomeSectionVideos(
    section: HomeSection,
    page: Int = 1,
    idx: Int = 0,
  ): List<VideoSummary> {
    return when (section) {
      HomeSection.Recommend -> getRecommendVideos(idx)
      HomeSection.Popular -> getPopularVideos(page)
      else -> getRegionVideos(
        tid = section.regionTid ?: return emptyList(),
        page = page,
      )
    }
  }

  suspend fun getRecommendVideos(idx: Int = 0): List<VideoSummary> {
    val sessData = sessionStore.sessData.first()
    val keys = wbiKeyRepository.ensureKeys(sessData)

    val params = mutableMapOf(
      "fresh_idx" to idx.toString(),
      "fresh_type" to "4",
      "ps" to "20",
    )

    val signedParams = if (keys != null) {
      wbiSigner.sign(params, keys.imgKey, keys.subKey)
    } else {
      params
    }

    val root = apiClient.getJson(
      url = BiliApiEndpoints.Recommend,
      params = signedParams,
      sessData = sessData,
    ).rootObject()
    root.requireBiliCodeOk("recommend")

    val item = root.obj("data")?.get("item") as? JsonArray ?: return emptyList()
    return item
      .mapNotNull { it.asObjectOrNull() }
      .filter { it.string("bvid").isNotBlank() }
      .map(::videoFromRecommend)
  }

  suspend fun getRelatedVideos(bvid: String): List<VideoSummary> {
    if (bvid.isBlank()) return emptyList()

    val sessData = sessionStore.sessData.first()
    val root = apiClient.getJson(
      url = BiliApiEndpoints.ArchiveRelated,
      params = mapOf("bvid" to bvid),
      sessData = sessData,
    ).rootObject()
    root.requireBiliCodeOk("archive related")

    val list = root["data"] as? JsonArray ?: return emptyList()
    return list
      .mapNotNull { it.asObjectOrNull() }
      .filter { it.string("bvid").isNotBlank() }
      .map(::videoFromRecommend)
  }

  suspend fun getSpaceVideos(
    mid: Long,
    page: Int = 1,
    order: String = SpaceOrderPubdate,
  ): List<VideoSummary> {
    if (mid <= 0L) return emptyList()

    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    val keys = wbiKeyRepository.ensureKeys(sessData)
    val params = mutableMapOf(
      "mid" to mid.toString(),
      "pn" to page.toString(),
      "ps" to SpacePageSize.toString(),
      "order" to order,
    )
    val signedParams = if (keys != null) {
      wbiSigner.sign(params, keys.imgKey, keys.subKey)
    } else {
      params
    }

    return runCatching {
      getSpaceVideosWithParams(signedParams, sessData, biliJct, context = "space archives")
    }.getOrElse { signedError ->
      logSpaceVideosFailure("signed", signedError)
      val refreshedVideos = if (keys != null) {
        val refreshedKeys = wbiKeyRepository.refreshKeys(sessData)
        if (refreshedKeys != null) {
          val refreshedSignedParams = wbiSigner.sign(params, refreshedKeys.imgKey, refreshedKeys.subKey)
          runCatching {
            getSpaceVideosWithParams(refreshedSignedParams, sessData, biliJct, context = "space archives refreshed")
          }.onFailure { refreshedError ->
            logSpaceVideosFailure("refreshed", refreshedError)
          }.getOrNull()
        } else {
          null
        }
      } else {
        null
      }
      refreshedVideos ?: if (signedParams == params) {
        emptyList()
      } else {
        runCatching {
          getSpaceVideosWithParams(params, sessData, biliJct, context = "space archives fallback")
        }.onFailure { fallbackError ->
          logSpaceVideosFailure("unsigned fallback", fallbackError)
        }.getOrDefault(emptyList())
      }
    }
  }

  private suspend fun getSpaceVideosWithParams(
    params: Map<String, String>,
    sessData: String?,
    biliJct: String?,
    context: String,
  ): List<VideoSummary> {
    val root = apiClient.getJson(
      url = BiliApiEndpoints.SpaceArcSearch,
      params = params,
      sessData = sessData,
      biliJct = biliJct,
    ).rootObject()
    root.requireBiliCodeOk(context)

    val list = root.obj("data")
      ?.obj("list")
      ?.get("vlist") as? JsonArray ?: return emptyList()
    return list
      .mapNotNull { it.asObjectOrNull() }
      .filter { it.string("bvid").isNotBlank() }
      .map(::videoFromSpaceVideo)
  }

  suspend fun checkFollowStatus(mid: Long): Boolean {
    if (mid <= 0L) return false

    val sessData = sessionStore.sessData.first()
    if (sessData.isNullOrBlank()) return false

    val root = apiClient.getJson(
      url = BiliApiEndpoints.Relation,
      params = mapOf("fid" to mid.toString()),
      sessData = sessData,
    ).rootObject()
    root.requireBiliCodeOk("relation")

    val attribute = root.obj("data")?.int("attribute") ?: 0
    return attribute == FollowAttribute || attribute == MutualFollowAttribute
  }

  suspend fun setFollowStatus(mid: Long, follow: Boolean): Boolean {
    if (mid <= 0L) return false

    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    if (sessData.isNullOrBlank() || biliJct.isNullOrBlank()) return false

    val root = apiClient.postFormJson(
      url = BiliApiEndpoints.RelationModify,
      params = mapOf(
        "fid" to mid.toString(),
        "act" to if (follow) FollowAction.toString() else UnfollowAction.toString(),
        "csrf" to biliJct,
      ),
      sessData = sessData,
      biliJct = biliJct,
    ).rootObject()
    root.requireBiliCodeOk("relation modify")
    return true
  }

  private suspend fun getPopularVideos(page: Int): List<VideoSummary> {
    val root = apiClient.getJson(
      url = BiliApiEndpoints.Popular,
      params = mapOf(
        "pn" to page.toString(),
        "ps" to "20",
      ),
    ).rootObject()
    root.requireBiliCodeOk("popular")

    val list = root.obj("data")?.get("list") as? JsonArray ?: return emptyList()
    return list
      .mapNotNull { it.asObjectOrNull() }
      .filter { it.string("bvid").isNotBlank() }
      .map(::videoFromRecommend)
  }

  private suspend fun getRegionVideos(tid: Int, page: Int): List<VideoSummary> {
    val root = apiClient.getJson(
      url = BiliApiEndpoints.Region,
      params = mapOf(
        "rid" to tid.toString(),
        "pn" to page.toString(),
        "ps" to "20",
      ),
    ).rootObject()
    root.requireBiliCodeOk("region")

    val archives = root.obj("data")?.get("archives") as? JsonArray ?: return emptyList()
    return archives
      .mapNotNull { it.asObjectOrNull() }
      .filter { it.string("bvid").isNotBlank() }
      .map(::videoFromRecommend)
  }

  suspend fun searchVideos(
    keyword: String,
    page: Int = 1,
    order: String = SearchOrderTotalRank,
  ): List<VideoSummary> {
    if (keyword.isBlank()) return emptyList()

    val sessData = sessionStore.sessData.first()
    val keys = wbiKeyRepository.ensureKeys(sessData)
    val params = mutableMapOf(
      "keyword" to keyword,
      "search_type" to "video",
      "page" to page.toString(),
      "pagesize" to "20",
      "order" to order,
    )

    val signedParams = if (keys != null) {
      wbiSigner.sign(params, keys.imgKey, keys.subKey)
    } else {
      params
    }

    val result = runCatching {
      val signedRoot = apiClient.getJson(
        url = BiliApiEndpoints.Search,
        params = signedParams,
        sessData = sessData,
      ).rootObject()
      signedRoot.requireBiliCodeOk("search")
      signedRoot.searchResultOrNull()
    }.getOrNull()
      ?: runCatching {
        val unsignedRoot = apiClient.getJson(
          url = BiliApiEndpoints.Search,
          params = params,
        ).rootObject()
        unsignedRoot.requireBiliCodeOk("search fallback")
        unsignedRoot.searchResultOrNull()
      }.getOrNull()
      ?: return emptyList()

    return result
      .mapNotNull { it.asObjectOrNull() }
      .filter { it.string("bvid").isNotBlank() }
      .map(::videoFromSearch)
  }

  suspend fun getSearchSuggestions(keyword: String): List<String> {
    if (keyword.isBlank()) return emptyList()

    val root = apiClient.getJson(
      url = BiliApiEndpoints.SearchSuggest,
      params = mapOf(
        "term" to keyword,
        "main_ver" to "v1",
        "highlight" to "",
      ),
    ).rootObject()
    root.requireBiliCodeOk("search suggestions")

    val tags = root.obj("result")?.get("tag") as? JsonArray ?: return emptyList()
    return tags
      .mapNotNull { it.asObjectOrNull()?.string("value") }
      .filter { it.isNotBlank() }
  }

  suspend fun getDynamicFeed(offset: String = ""): DynamicFeedPage {
    val sessData = sessionStore.sessData.first()
    if (sessData.isNullOrBlank()) {
      return DynamicFeedPage(videos = emptyList(), offset = "", hasMore = false)
    }

    val root = apiClient.getJson(
      url = BiliApiEndpoints.DynamicFeed,
      params = buildMap {
        put("type", "all")
        if (offset.isNotBlank()) {
          put("offset", offset)
        }
      },
      sessData = sessData,
    ).rootObject()
    root.requireBiliCodeOk("dynamic feed")

    val data = root.obj("data") ?: return DynamicFeedPage(videos = emptyList(), offset = "", hasMore = false)
    val items = data["items"] as? JsonArray ?: return DynamicFeedPage(videos = emptyList(), offset = "", hasMore = false)
    val videos = items
      .mapNotNull { it.asObjectOrNull() }
      .mapNotNull(::videoFromDynamicItem)
      .filter { it.bvid.isNotBlank() }

    return DynamicFeedPage(
      videos = videos,
      offset = data.string("offset"),
      hasMore = data.boolean("has_more"),
    )
  }

  suspend fun getHistoryPage(
    pageSize: Int = HistoryPageSize,
    viewAt: Long = 0L,
    max: Long = 0L,
  ): HistoryFeedPage {
    val sessData = sessionStore.sessData.first()
    if (sessData.isNullOrBlank()) {
      return HistoryFeedPage(videos = emptyList(), nextViewAt = 0L, nextMax = 0L, hasMore = false)
    }

    val root = apiClient.getJson(
      url = BiliApiEndpoints.HistoryCursor,
      params = buildMap {
        put("ps", pageSize.toString())
        if (viewAt > 0L) {
          put("view_at", viewAt.toString())
        }
        if (max > 0L) {
          put("max", max.toString())
        }
      },
      sessData = sessData,
    ).rootObject()
    root.requireBiliCodeOk("history")

    val data = root.obj("data") ?: return HistoryFeedPage(videos = emptyList(), nextViewAt = 0L, nextMax = 0L, hasMore = false)
    val list = data["list"] as? JsonArray ?: return HistoryFeedPage(videos = emptyList(), nextViewAt = 0L, nextMax = 0L, hasMore = false)
    val videos = list
      .mapNotNull { it.asObjectOrNull() }
      .map(::videoFromHistory)
      .filter { it.bvid.isNotBlank() || it.isLive }

    val cursor = data.obj("cursor")
    return HistoryFeedPage(
      videos = videos,
      nextViewAt = cursor?.long("view_at") ?: 0L,
      nextMax = cursor?.long("max") ?: 0L,
      hasMore = videos.isNotEmpty() && cursor != null,
    )
  }

  private fun JsonObject.searchResultOrNull(): JsonArray? {
    return obj("data")?.get("result") as? JsonArray
  }

  private fun videoFromRecommend(json: JsonObject): VideoSummary {
    val owner = json.obj("owner")
    val stat = json.obj("stat")
    return VideoSummary(
      bvid = json.string("bvid"),
      title = json.string("title"),
      pic = fixPicUrl(json.string("pic")),
      ownerName = owner?.string("name").orEmpty(),
      ownerFace = fixPicUrl(owner?.string("face").orEmpty()),
      ownerMid = owner?.long("mid") ?: 0L,
      view = BiliNumberParser.toInt(stat?.get("view")),
      danmaku = BiliNumberParser.toInt(stat?.get("danmaku")),
      duration = BiliNumberParser.parseDuration(json["duration"]),
      pubdate = json.long("pubdate"),
      badge = filterBadge(json.string("badge")),
    )
  }

  private fun videoFromDynamicItem(json: JsonObject): VideoSummary? {
    if (json["visible"]?.jsonPrimitive?.booleanOrNull == false) {
      return null
    }

    val modules = json.obj("modules") ?: return null
    val dynamicModule = modules.obj("module_dynamic") ?: return null
    val major = dynamicModule.obj("major") ?: return null
    if (major.string("type") != "MAJOR_TYPE_ARCHIVE") {
      return null
    }

    val archive = major.obj("archive") ?: return null
    val author = modules.obj("module_author")
    val stat = archive.obj("stat")
    return VideoSummary(
      bvid = archive.string("bvid"),
      title = archive.string("title"),
      pic = fixPicUrl(archive.string("cover")),
      ownerName = author?.string("name").orEmpty(),
      ownerFace = fixPicUrl(author?.string("face").orEmpty()),
      ownerMid = author?.long("mid") ?: 0L,
      view = BiliNumberParser.toInt(stat?.get("play") ?: stat?.get("view")),
      danmaku = BiliNumberParser.toInt(stat?.get("danmaku")),
      duration = BiliNumberParser.parseDuration(archive["duration_text"]),
      pubdate = author?.long("pub_ts") ?: 0L,
      badge = filterBadge(archive.obj("badge")?.string("text").orEmpty()),
    )
  }

  private fun videoFromHistory(json: JsonObject): VideoSummary {
    val history = json.obj("history")
    val cover = json.string("cover").ifBlank { json.string("pic") }
    val badge = json.string("badge")
    val business = history?.string("business").orEmpty()
    val isLive = json.int("live_status") == 1 ||
      business == "live" ||
      badge.contains("\u76f4\u64ad") ||
      badge == "\u672a\u5f00\u64ad"

    return VideoSummary(
      bvid = history?.string("bvid").orEmpty(),
      title = json.string("title"),
      pic = fixPicUrl(cover),
      ownerName = json.string("author_name"),
      ownerFace = fixPicUrl(json.string("author_face")),
      ownerMid = json.long("author_mid"),
      view = BiliNumberParser.toInt(json.obj("stat")?.get("view")),
      danmaku = BiliNumberParser.toInt(json.obj("stat")?.get("danmaku")),
      duration = BiliNumberParser.parseDuration(json["duration"]),
      pubdate = json.long("pubdate"),
      badge = filterBadge(badge),
      progress = json.int("progress"),
      viewAt = json.long("view_at"),
      cid = history?.long("cid")?.takeIf { it != 0L } ?: (history?.long("oid") ?: 0L),
      historyPage = history?.int("page") ?: 0,
      historyPart = history?.string("part").orEmpty(),
      historyVideos = json.int("videos"),
      isLive = isLive,
    )
  }

  private fun videoFromSearch(json: JsonObject): VideoSummary {
    return VideoSummary(
      bvid = json.string("bvid"),
      title = stripHtmlTags(json.string("title")),
      pic = fixPicUrl(json.string("pic")),
      ownerName = json.string("author"),
      ownerFace = fixPicUrl(json.searchOwnerFace()),
      ownerMid = json.long("mid"),
      view = BiliNumberParser.toInt(json["play"]),
      danmaku = BiliNumberParser.toInt(json["danmaku"]),
      duration = BiliNumberParser.parseDuration(json["duration"]),
      pubdate = json.long("pubdate"),
      badge = filterBadge(json.string("badge")),
    )
  }

  private fun videoFromSpaceVideo(json: JsonObject): VideoSummary {
    return VideoSummary(
      bvid = json.string("bvid"),
      title = json.string("title"),
      pic = fixPicUrl(json.string("pic")),
      ownerName = json.string("author"),
      ownerFace = "",
      ownerMid = json.long("mid"),
      view = BiliNumberParser.toInt(json["play"]),
      danmaku = BiliNumberParser.toInt(json["video_review"]),
      duration = BiliNumberParser.parseDuration(json["length"]),
      pubdate = json.long("created"),
      badge = filterBadge(json.string("badge")),
    )
  }

  private fun JsonObject.searchOwnerFace(): String {
    return string("upic")
      .ifBlank { string("face") }
      .ifBlank { string("avatar") }
      .ifBlank { obj("owner")?.string("face").orEmpty() }
  }

  private fun fixPicUrl(url: String): String {
    return when {
      url.startsWith("//") -> "https:$url"
      url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
      else -> url
    }
  }

  private fun stripHtmlTags(text: String): String {
    return text.replace(HtmlTagRegex, "")
  }

  private fun filterBadge(badge: String): String {
    return if (badge == "\u6295\u7a3f\u89c6\u9891" || badge == "\u6295\u7a3f") "" else badge
  }

  private fun logSpaceVideosFailure(stage: String, error: Throwable) {
    Log.w(LogTag, "space videos $stage failed: ${error.toSpaceVideoBrief()}")
  }

  private fun Throwable.toSpaceVideoBrief(): String {
    return when (this) {
      is BiliApiCodeException -> "code=$code message=$biliMessage"
      is BiliNetworkException -> "http=$statusCode body=${responseBody.take(LogBodyPreviewLength)}"
      else -> "${javaClass.simpleName}: ${message.orEmpty()}"
    }
  }

  private companion object {
    const val LogTag = "BiliVideoRepository"
    const val LogBodyPreviewLength = 160
    const val SearchOrderTotalRank = "totalrank"
    const val HistoryPageSize = 30
    const val SpaceOrderPubdate = "pubdate"
    const val SpacePageSize = 30
    const val FollowAction = 1
    const val UnfollowAction = 2
    const val FollowAttribute = 2
    const val MutualFollowAttribute = 6
    val HtmlTagRegex = Regex("<[^>]*>")
  }
}

data class DynamicFeedPage(
  val videos: List<VideoSummary>,
  val offset: String,
  val hasMore: Boolean,
)

data class HistoryFeedPage(
  val videos: List<VideoSummary>,
  val nextViewAt: Long,
  val nextMax: Long,
  val hasMore: Boolean,
)
