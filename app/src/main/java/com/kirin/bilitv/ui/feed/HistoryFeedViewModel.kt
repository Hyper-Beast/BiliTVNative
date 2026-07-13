package com.kirin.bilitv.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kirin.bilitv.core.network.VideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class HistoryFeedViewModel(
  private val videoRepository: VideoRepository,
) : ViewModel() {
  private val _viewState = MutableStateFlow(UserFeedViewState())
  val viewState: StateFlow<UserFeedViewState> = _viewState.asStateFlow()

  private var nextViewAt = 0L
  private var nextMax = 0L
  private var loadedOnce = false
  private var handledManualRefreshKey = 0
  private var firstPageRequestId = 0
  private var firstPageJob: Job? = null
  private var nextPageJob: Job? = null

  fun loadFirstPage(forceRefresh: Boolean) {
    if (!forceRefresh && loadedOnce) {
      return
    }

    val requestId = ++firstPageRequestId
    nextViewAt = 0L
    nextMax = 0L
    firstPageJob?.cancel()
    nextPageJob?.cancel()
    _viewState.value = UserFeedViewState(UserFeedState.Loading)

    firstPageJob = viewModelScope.launch {
      var nextViewAtValue = 0L
      var nextMaxValue = 0L
      val nextState = try {
        val page = videoRepository.getHistoryPage()
        nextViewAtValue = page.nextViewAt
        nextMaxValue = page.nextMax
        if (page.videos.isEmpty()) {
          UserFeedState.Empty
        } else {
          UserFeedState.Success(
            videos = page.videos,
            loadingMore = false,
            endReached = !page.hasMore,
            loadMoreError = "",
          )
        }
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        UserFeedState.Failed(error.message.orEmpty())
      }

      if (firstPageRequestId != requestId) {
        return@launch
      }
      loadedOnce = true
      nextViewAt = nextViewAtValue
      nextMax = nextMaxValue
      _viewState.value = UserFeedViewState(nextState)
    }
  }

  fun refreshForManualKey(manualRefreshKey: Int): Boolean {
    if (manualRefreshKey <= 0 || manualRefreshKey == handledManualRefreshKey) {
      return false
    }
    handledManualRefreshKey = manualRefreshKey
    loadFirstPage(forceRefresh = true)
    return true
  }

  fun retry() {
    loadFirstPage(forceRefresh = true)
  }

  fun loadNextPage() {
    val currentState = _viewState.value.state as? UserFeedState.Success ?: return
    if (currentState.loadingMore || currentState.endReached) {
      return
    }

    val viewAtToLoad = nextViewAt
    val maxToLoad = nextMax
    _viewState.value = UserFeedViewState(
      currentState.copy(loadingMore = true, loadMoreError = ""),
    )

    nextPageJob?.cancel()
    nextPageJob = viewModelScope.launch {
      try {
        val page = videoRepository.getHistoryPage(
          viewAt = viewAtToLoad,
          max = maxToLoad,
        )
        val latestState = _viewState.value.state as? UserFeedState.Success ?: return@launch
        val mergedVideos = latestState.videos.appendUniqueFeedVideos(nextVideos = page.videos)
        nextViewAt = page.nextViewAt
        nextMax = page.nextMax
        _viewState.value = UserFeedViewState(
          latestState.copy(
            videos = mergedVideos,
            loadingMore = false,
            endReached = !page.hasMore ||
              page.videos.isEmpty() ||
              mergedVideos.size == latestState.videos.size,
            loadMoreError = "",
          ),
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        val latestState = _viewState.value.state as? UserFeedState.Success ?: return@launch
        _viewState.value = UserFeedViewState(
          latestState.copy(loadingMore = false, loadMoreError = error.message.orEmpty()),
        )
      }
    }
  }

  companion object {
    fun factory(videoRepository: VideoRepository): ViewModelProvider.Factory {
      return viewModelFactory {
        initializer {
          HistoryFeedViewModel(videoRepository = videoRepository)
        }
      }
    }
  }
}
