package com.kirin.bilitv.ui.feed

import com.kirin.bilitv.core.model.VideoSummary

internal data class UserFeedViewState(
  val state: UserFeedState = UserFeedState.Loading,
)

internal sealed interface UserFeedState {
  data object Loading : UserFeedState
  data object Empty : UserFeedState
  data class Failed(val message: String) : UserFeedState
  data class Success(
    val videos: List<VideoSummary>,
    val loadingMore: Boolean,
    val endReached: Boolean,
    val loadMoreError: String,
  ) : UserFeedState
}

internal fun List<VideoSummary>.appendUniqueFeedVideos(nextVideos: List<VideoSummary>): List<VideoSummary> {
  if (nextVideos.isEmpty()) {
    return this
  }
  val knownKeys = mapIndexedTo(mutableSetOf()) { index, video -> video.userFeedKey(index) }
  return this + nextVideos.filterIndexed { index, video -> knownKeys.add(video.userFeedKey(index)) }
}

internal fun VideoSummary.userFeedKey(index: Int): String {
  return bvid.ifBlank {
    "cid-$cid-view-$viewAt-$index"
  }
}
