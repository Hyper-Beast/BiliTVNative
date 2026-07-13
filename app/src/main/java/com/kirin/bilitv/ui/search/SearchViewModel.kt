package com.kirin.bilitv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.network.VideoRepository
import com.kirin.bilitv.core.storage.SearchHistoryStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SearchViewModel(
  private val videoRepository: VideoRepository,
  private val searchHistoryStore: SearchHistoryStore,
) : ViewModel() {
  private val _viewState = MutableStateFlow(SearchViewState())
  val viewState: StateFlow<SearchViewState> = _viewState.asStateFlow()

  private var suggestionsJob: Job? = null
  private var firstPageJob: Job? = null
  private var nextPageJob: Job? = null
  private var firstPageRequestId = 0

  init {
    viewModelScope.launch {
      searchHistoryStore.history.collect { history ->
        _viewState.value = _viewState.value.copy(searchHistory = history)
      }
    }
  }

  fun updateSearchText(nextText: String) {
    _viewState.value = _viewState.value.copy(searchText = nextText)
    requestSuggestions(nextText)
  }

  fun startSearch(rawQuery: String) {
    val query = rawQuery.trim()
    if (query.isBlank()) return

    viewModelScope.launch {
      searchHistoryStore.add(query)
    }

    val current = _viewState.value
    val queryChanged = current.activeQuery != query
    val nextOrderKey = if (queryChanged) SearchSortOptions.first().key else current.selectedOrderKey
    _viewState.value = current.copy(
      searchText = query,
      activeQuery = query,
      selectedOrderKey = nextOrderKey,
      resultState = SearchResultState.Loading,
    )
    loadFirstPage(query = query, orderKey = nextOrderKey)
  }

  fun backToKeyboard() {
    _viewState.value = _viewState.value.copy(activeQuery = null)
  }

  fun clear() {
    suggestionsJob?.cancel()
    firstPageJob?.cancel()
    nextPageJob?.cancel()
    _viewState.value = _viewState.value.copy(
      searchText = "",
      activeQuery = null,
      suggestions = emptyList(),
      selectedOrderKey = SearchSortOptions.first().key,
      resultState = SearchResultState.Loading,
    )
  }

  fun clearSearchHistory() {
    viewModelScope.launch {
      searchHistoryStore.clear()
    }
  }

  fun selectOrder(orderKey: String) {
    if (SearchSortOptions.none { option -> option.key == orderKey }) return

    val current = _viewState.value
    if (current.selectedOrderKey == orderKey) return

    val query = current.activeQuery ?: return
    _viewState.value = current.copy(
      selectedOrderKey = orderKey,
      resultState = SearchResultState.Loading,
    )
    loadFirstPage(query = query, orderKey = orderKey)
  }

  fun retry() {
    val current = _viewState.value
    val query = current.activeQuery ?: return
    _viewState.value = current.copy(resultState = SearchResultState.Loading)
    loadFirstPage(query = query, orderKey = current.selectedOrderKey)
  }

  fun loadNextPage() {
    val current = _viewState.value
    val query = current.activeQuery ?: return
    val currentState = current.resultState as? SearchResultState.Success ?: return
    if (currentState.loadingMore || currentState.endReached) return

    val pageToLoad = currentState.nextPage
    val orderToLoad = current.selectedOrderKey
    _viewState.value = current.copy(
      resultState = currentState.copy(
        loadingMore = true,
        loadMoreError = "",
      ),
    )

    nextPageJob?.cancel()
    nextPageJob = viewModelScope.launch {
      val nextState = try {
        val nextVideos = videoRepository.searchVideos(
          keyword = query,
          page = pageToLoad,
          order = orderToLoad,
        )
        val latest = _viewState.value
        val latestState = latest.resultState as? SearchResultState.Success ?: return@launch
        if (
          latest.activeQuery != query ||
          latest.selectedOrderKey != orderToLoad ||
          latestState.nextPage != pageToLoad
        ) {
          return@launch
        }
        val mergedVideos = latestState.videos.appendUniqueByBvid(nextVideos)
        latestState.copy(
          videos = mergedVideos,
          nextPage = pageToLoad + 1,
          loadingMore = false,
          endReached = nextVideos.size < PageSize ||
            mergedVideos.size == latestState.videos.size,
          loadMoreError = "",
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        val latestState = _viewState.value.resultState as? SearchResultState.Success
          ?: return@launch
        latestState.copy(
          loadingMore = false,
          loadMoreError = error.message.orEmpty(),
        )
      }
      val latest = _viewState.value
      _viewState.value = latest.copy(resultState = nextState)
    }
  }

  private fun requestSuggestions(searchText: String) {
    suggestionsJob?.cancel()
    if (searchText.isBlank()) {
      _viewState.value = _viewState.value.copy(suggestions = emptyList())
      return
    }

    suggestionsJob = viewModelScope.launch {
      delay(SearchSuggestionDebounceMs)
      val suggestions = runCatching {
        videoRepository.getSearchSuggestions(searchText.trim())
      }.getOrElse {
        emptyList()
      }
      if (_viewState.value.searchText == searchText) {
        _viewState.value = _viewState.value.copy(suggestions = suggestions)
      }
    }
  }

  private fun loadFirstPage(query: String, orderKey: String) {
    val requestId = ++firstPageRequestId
    firstPageJob?.cancel()
    nextPageJob?.cancel()
    firstPageJob = viewModelScope.launch {
      val nextState = try {
        val videos = videoRepository.searchVideos(
          keyword = query,
          page = FirstPage,
          order = orderKey,
        )
        if (videos.isEmpty()) {
          SearchResultState.Empty
        } else {
          SearchResultState.Success(
            videos = videos,
            nextPage = FirstPage + 1,
            loadingMore = false,
            endReached = videos.size < PageSize,
            loadMoreError = "",
          )
        }
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        SearchResultState.Failed(error.message.orEmpty())
      }

      val latest = _viewState.value
      if (
        firstPageRequestId != requestId ||
        latest.activeQuery != query ||
        latest.selectedOrderKey != orderKey
      ) {
        return@launch
      }
      _viewState.value = latest.copy(resultState = nextState)
    }
  }

  companion object {
    fun factory(
      videoRepository: VideoRepository,
      searchHistoryStore: SearchHistoryStore,
    ): ViewModelProvider.Factory {
      return viewModelFactory {
        initializer {
          SearchViewModel(
            videoRepository = videoRepository,
            searchHistoryStore = searchHistoryStore,
          )
        }
      }
    }
  }
}

internal data class SearchViewState(
  val searchText: String = "",
  val activeQuery: String? = null,
  val suggestions: List<String> = emptyList(),
  val searchHistory: List<String> = emptyList(),
  val selectedOrderKey: String = SearchSortOptions.first().key,
  val resultState: SearchResultState = SearchResultState.Loading,
)

internal sealed interface SearchResultState {
  data object Loading : SearchResultState
  data object Empty : SearchResultState
  data class Failed(val message: String) : SearchResultState
  data class Success(
    val videos: List<VideoSummary>,
    val nextPage: Int,
    val loadingMore: Boolean,
    val endReached: Boolean,
    val loadMoreError: String,
  ) : SearchResultState
}

internal data class SearchSortOption(
  val key: String,
  val titleRes: Int,
)

internal val SearchSortOptions = listOf(
  SearchSortOption("totalrank", R.string.search_sort_totalrank),
  SearchSortOption("click", R.string.search_sort_click),
  SearchSortOption("pubdate", R.string.search_sort_pubdate),
  SearchSortOption("dm", R.string.search_sort_dm),
)

private fun List<VideoSummary>.appendUniqueByBvid(nextVideos: List<VideoSummary>): List<VideoSummary> {
  if (nextVideos.isEmpty()) {
    return this
  }
  val knownBvids = mapTo(mutableSetOf()) { video -> video.bvid }
  return this + nextVideos.filter { video -> knownBvids.add(video.bvid) }
}

private const val SearchSuggestionDebounceMs = 250L
private const val FirstPage = 1
private const val PageSize = 20
