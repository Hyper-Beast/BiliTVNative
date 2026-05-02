package com.kirin.bilitv.core.model

data class VideoSummary(
  val bvid: String,
  val title: String,
  val pic: String,
  val ownerName: String,
  val ownerFace: String,
  val ownerMid: Long,
  val view: Int,
  val danmaku: Int,
  val duration: Int,
  val pubdate: Long,
  val badge: String,
  val progress: Int = ProgressUnset,
  val viewAt: Long = 0L,
  val cid: Long = 0L,
  val historyPage: Int = 0,
  val historyPart: String = "",
  val historyVideos: Int = 0,
  val isLive: Boolean = false,
)

const val ProgressUnset = -1
