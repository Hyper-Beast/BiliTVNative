package com.kirin.bilitv.core.player

data class PlaybackRequest(
  val bvid: String,
  val cid: Long,
  val title: String,
  val startPositionMs: Long = 0L,
  val aid: Long = 0L,
  val ownerName: String = "",
  val ownerFace: String = "",
  val ownerMid: Long = 0L,
  val viewCount: Int = 0,
  val danmakuCount: Int = 0,
  val pubdate: Long = 0L,
  val preferredQualityId: Int? = null,
  val forceStartPosition: Boolean = false,
  val historyPage: Int = 0,
  val advanceToNextHistoryEpisode: Boolean = false,
) : java.io.Serializable

data class PlaybackInfo(
  val bvid: String,
  val cid: Long,
  val title: String,
  val durationMs: Long,
  val qualities: List<PlaybackQuality>,
  val selectedQuality: PlaybackQuality,
  val videoTracks: List<PlaybackTrack>,
  val audioTracks: List<PlaybackTrack>,
  val headers: BiliPlaybackHeaders,
)

data class PlaybackQuality(
  val id: Int,
  val description: String,
)

data class PlaybackVideoMetadata(
  val aid: Long,
  val bvid: String,
  val cid: Long,
  val title: String,
  val ownerName: String,
  val ownerFace: String,
  val ownerMid: Long,
  val viewCount: Int,
  val danmakuCount: Int,
  val pubdate: Long,
  val pages: List<PlaybackEpisode>,
)

data class PlaybackEpisode(
  val cid: Long,
  val page: Int,
  val title: String,
  val durationSeconds: Int,
)

data class PlaybackTrack(
  val id: Int,
  val baseUrl: String,
  val backupUrls: List<String>,
  val bandwidth: Int,
  val codecs: String,
  val width: Int,
  val height: Int,
  val mimeType: String,
  val segmentBase: PlaybackSegmentBase,
) {
  val isH264: Boolean
    get() = codecs.contains("avc", ignoreCase = true)

  val isH265: Boolean
    get() = codecs.contains("hev", ignoreCase = true) || codecs.contains("hvc", ignoreCase = true)

  val isAv1: Boolean
    get() = codecs.contains("av01", ignoreCase = true)
}

data class PlaybackSegmentBase(
  val initializationRange: String,
  val indexRange: String,
)
