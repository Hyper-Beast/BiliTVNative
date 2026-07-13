package com.kirin.bilitv.core.player

data class PlayerComment(
  val rpid: Long,
  val authorName: String,
  val authorAvatar: String,
  val message: String,
  val likeCount: Int,
  val replyCount: Int,
  val ctime: Long,
)
