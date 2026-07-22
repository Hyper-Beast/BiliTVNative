package com.kirin.bilitv.core.network

import com.kirin.bilitv.core.auth.WbiKeyRepository
import com.kirin.bilitv.core.auth.WbiSigner
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.storage.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray

internal class HomeVideoRepository(
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
      .map(VideoSummaryMappers::fromArchive)
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
      .map(VideoSummaryMappers::fromArchive)
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
      .map(VideoSummaryMappers::fromArchive)
  }

  private suspend fun getRegionVideos(tid: Int, page: Int): List<VideoSummary> {
    val root = apiClient.getJson(
      url = BiliApiEndpoints.RegionLatest,
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
      .map(VideoSummaryMappers::fromArchive)
  }
}
