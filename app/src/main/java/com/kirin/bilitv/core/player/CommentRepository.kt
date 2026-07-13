package com.kirin.bilitv.core.player

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
import kotlinx.serialization.json.JsonObject

internal class CommentRepository(
  private val apiClient: BiliApiClient,
  private val sessionStore: SessionStore,
) {
  suspend fun getComments(aid: Long): List<PlayerComment> {
    if (aid <= 0L) return emptyList()
    val sessData = sessionStore.sessData.first()
    val biliJct = sessionStore.biliJct.first()
    val primaryComments = runCatching {
      val root = apiClient.getJson(
        url = BiliApiEndpoints.ArchiveRepliesMain,
        params = mapOf(
          "type" to ArchiveReplyType,
          "oid" to aid.toString(),
          "mode" to ReplyModeHot,
          "ps" to PageSize,
          "plat" to ReplyPlatformWeb,
          "web_location" to ReplyWebLocation,
          "seek_rpid" to FirstReplyId,
          "pagination_str" to FirstPagePagination,
        ),
        sessData = sessData,
        biliJct = biliJct,
      ).rootObject()
      root.requireBiliCodeOk("archive main replies")
      root.obj("data")?.mainReplyComments().orEmpty()
    }.getOrDefault(emptyList())
    if (primaryComments.isNotEmpty()) {
      return primaryComments
    }

    val root = apiClient.getJson(
      url = BiliApiEndpoints.ArchiveReplies,
      params = mapOf(
        "type" to ArchiveReplyType,
        "oid" to aid.toString(),
        "sort" to ReplySortHot,
        "pn" to FirstPage,
        "ps" to PageSize,
      ),
      sessData = sessData,
      biliJct = biliJct,
    ).rootObject()
    root.requireBiliCodeOk("archive replies")
    val replies = root.obj("data")?.get("replies") as? JsonArray ?: return emptyList()
    return replies.mapNotNull { element ->
      element.asObjectOrNull()?.toPlayerComment()
    }
  }

  private fun JsonObject.mainReplyComments(): List<PlayerComment> {
    return listOf("hots", "replies")
      .flatMap { name -> commentArray(name) }
      .mapNotNull { element -> element.asObjectOrNull()?.toPlayerComment() }
      .distinctBy(PlayerComment::rpid)
  }

  private fun JsonObject.commentArray(name: String): List<kotlinx.serialization.json.JsonElement> {
    return (this[name] as? JsonArray)?.toList().orEmpty()
  }

  private fun JsonObject.toPlayerComment(): PlayerComment? {
    val content = obj("content")
    val message = content?.string("message").orEmpty().trim()
    if (message.isBlank()) return null
    val member = obj("member")
    return PlayerComment(
      rpid = long("rpid"),
      authorName = member?.string("uname").orEmpty(),
      authorAvatar = member?.string("avatar").orEmpty(),
      message = message,
      likeCount = BiliNumberParser.toInt(this["like"]),
      replyCount = int("rcount"),
      ctime = long("ctime"),
    )
  }

  private companion object {
    const val ArchiveReplyType = "1"
    const val ReplyModeHot = "3"
    const val ReplySortHot = "2"
    const val FirstPage = "1"
    const val PageSize = "20"
    const val ReplyPlatformWeb = "1"
    const val ReplyWebLocation = "1315875"
    const val FirstReplyId = "0"
    const val FirstPagePagination = """{"offset":""}"""
  }
}
