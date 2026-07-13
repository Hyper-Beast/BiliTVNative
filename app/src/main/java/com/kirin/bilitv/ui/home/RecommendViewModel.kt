package com.kirin.bilitv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.core.network.VideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class RecommendViewModel(
  private val videoRepository: VideoRepository,
) : ViewModel() {
  private val _viewState = MutableStateFlow(RecommendViewState())
  val viewState: StateFlow<RecommendViewState> = _viewState.asStateFlow()

  private var nextLoadRequestId = 0
  private var handledManualRefreshKey = 0
  private val firstPageRequestIds = mutableMapOf<String, Int>()
  private val firstPageJobs = mutableMapOf<String, Job>()
  private val nextPageJobs = mutableMapOf<String, Job>()

  fun setEnabledHomeSections(enabledHomeSections: Set<HomeSection>) {
    val sections = enabledHomeSections.toOrderedSections()
    val sectionKeys = sections.mapTo(mutableSetOf()) { section -> section.key }
    val current = _viewState.value
    val selectedSectionKey = current.selectedSectionKey
      .takeIf { key -> key in sectionKeys }
      ?: sections.first().key
    val activeSectionKey = current.activeSectionKey
      .takeIf { key -> key in sectionKeys }
      ?: selectedSectionKey

    val nextState = current.copy(
      sections = sections,
      selectedSectionKey = selectedSectionKey,
      activeSectionKey = activeSectionKey,
      sectionStates = current.sectionStates.filterKeys { key -> key in sectionKeys },
      loadedSectionKeys = current.loadedSectionKeys.filterTo(mutableSetOf()) { key -> key in sectionKeys },
      sectionRefreshKeys = current.sectionRefreshKeys.filterKeys { key -> key in sectionKeys },
    )
    _viewState.value = nextState

    if (nextState.sectionStates[activeSectionKey] == null) {
      requestFirstPage(
        section = sections.first { section -> section.key == activeSectionKey },
        refreshKey = nextState.sectionRefreshKeys[activeSectionKey] ?: 0,
      )
    }
  }

  fun previewSection(section: HomeSection) {
    if (!section.isVisible()) return

    _viewState.value = _viewState.value.copy(selectedSectionKey = section.key)
  }

  fun activateSection(section: HomeSection) {
    if (!section.isVisible()) return

    _viewState.value = _viewState.value.copy(
      selectedSectionKey = section.key,
      activeSectionKey = section.key,
    )
  }

  fun selectSection(section: HomeSection, forceRefresh: Boolean) {
    val current = _viewState.value
    if (!section.isVisible(current)) return

    val hasLoadedSection = section.key in current.loadedSectionKeys
    val nextRefreshKey = if (forceRefresh) {
      (current.sectionRefreshKeys[section.key] ?: 0) + 1
    } else {
      current.sectionRefreshKeys[section.key] ?: 0
    }
    _viewState.value = current.copy(
      selectedSectionKey = section.key,
      activeSectionKey = section.key,
      sectionRefreshKeys = current.sectionRefreshKeys + (section.key to nextRefreshKey),
    )

    if (forceRefresh || !hasLoadedSection) {
      requestFirstPage(section = section, refreshKey = nextRefreshKey)
    }
  }

  fun refreshActiveSection() {
    val current = _viewState.value
    val activeSection = current.activeSection() ?: return
    val nextRefreshKey = (current.sectionRefreshKeys[activeSection.key] ?: 0) + 1
    _viewState.value = current.copy(
      sectionRefreshKeys = current.sectionRefreshKeys + (activeSection.key to nextRefreshKey),
    )
    requestFirstPage(section = activeSection, refreshKey = nextRefreshKey)
  }

  fun refreshForManualKey(manualRefreshKey: Int): Boolean {
    if (manualRefreshKey <= 0 || manualRefreshKey == handledManualRefreshKey) {
      return false
    }
    handledManualRefreshKey = manualRefreshKey
    refreshActiveSection()
    return true
  }

  fun loadNextPage(section: HomeSection) {
    val current = _viewState.value
    if (!section.isVisible(current)) return

    val currentState = current.sectionStates[section.key] as? RecommendState.Success ?: return
    if (currentState.loadingMore || currentState.endReached) return

    val pageToLoad = currentState.nextPage
    val refreshKey = current.sectionRefreshKeys[section.key] ?: 0
    _viewState.value = current.copy(
      sectionStates = current.sectionStates + (
        section.key to currentState.copy(
          loadingMore = true,
          loadMoreError = "",
        )
      ),
    )

    nextPageJobs[section.key]?.cancel()
    nextPageJobs[section.key] = viewModelScope.launch {
      val nextState = try {
        val nextVideos = videoRepository.getHomeSectionVideos(
          section = section,
          page = pageToLoad,
          idx = if (section == HomeSection.Recommend) {
            refreshKey + pageToLoad - FirstPage
          } else {
            0
          },
        )
        val latest = _viewState.value
        val latestState = latest.sectionStates[section.key] as? RecommendState.Success
          ?: return@launch
        if (
          latest.sectionRefreshKeys[section.key] != refreshKey ||
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
        val latestState = _viewState.value.sectionStates[section.key] as? RecommendState.Success
          ?: return@launch
        latestState.copy(
          loadingMore = false,
          loadMoreError = error.message.orEmpty(),
        )
      }
      val latest = _viewState.value
      _viewState.value = latest.copy(
        sectionStates = latest.sectionStates + (section.key to nextState),
      )
    }
  }

  private fun requestFirstPage(section: HomeSection, refreshKey: Int) {
    if (!section.isVisible()) return

    val requestId = ++nextLoadRequestId
    firstPageRequestIds[section.key] = requestId
    firstPageJobs[section.key]?.cancel()
    val current = _viewState.value
    _viewState.value = current.copy(
      sectionStates = current.sectionStates + (section.key to RecommendState.Loading),
      sectionRefreshKeys = current.sectionRefreshKeys + (section.key to refreshKey),
    )

    firstPageJobs[section.key] = viewModelScope.launch {
      val nextState = try {
        val videos = videoRepository.getHomeSectionVideos(
          section = section,
          page = FirstPage,
          idx = if (section == HomeSection.Recommend) refreshKey else 0,
        )
        if (videos.isEmpty()) {
          RecommendState.Empty
        } else {
          RecommendState.Success(
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
        RecommendState.Failed(error.message.orEmpty())
      }

      if (firstPageRequestIds[section.key] != requestId) {
        return@launch
      }
      val latest = _viewState.value
      _viewState.value = latest.copy(
        loadedSectionKeys = latest.loadedSectionKeys + section.key,
        sectionStates = latest.sectionStates + (section.key to nextState),
      )
    }
  }

  private fun HomeSection.isVisible(state: RecommendViewState = _viewState.value): Boolean {
    return state.sections.any { section -> section == this }
  }

  private fun RecommendViewState.activeSection(): HomeSection? {
    return sections.firstOrNull { section -> section.key == activeSectionKey }
  }

  companion object {
    fun factory(videoRepository: VideoRepository): ViewModelProvider.Factory {
      return viewModelFactory {
        initializer {
          RecommendViewModel(videoRepository = videoRepository)
        }
      }
    }
  }
}

internal data class RecommendViewState(
  val sections: List<HomeSection> = listOf(HomeSection.Recommend),
  val selectedSectionKey: String = HomeSection.Recommend.key,
  val activeSectionKey: String = HomeSection.Recommend.key,
  val sectionStates: Map<String, RecommendState> = emptyMap(),
  val loadedSectionKeys: Set<String> = emptySet(),
  val sectionRefreshKeys: Map<String, Int> = emptyMap(),
)

internal sealed interface RecommendState {
  data object Loading : RecommendState
  data object Empty : RecommendState
  data class Success(
    val videos: List<VideoSummary>,
    val nextPage: Int,
    val loadingMore: Boolean,
    val endReached: Boolean,
    val loadMoreError: String,
  ) : RecommendState
  data class Failed(val message: String) : RecommendState
}

private fun Set<HomeSection>.toOrderedSections(): List<HomeSection> {
  return HomeSection.DefaultOrder.filter { section -> section in this }
    .ifEmpty { listOf(HomeSection.Recommend) }
}

private fun List<VideoSummary>.appendUniqueByBvid(nextVideos: List<VideoSummary>): List<VideoSummary> {
  if (nextVideos.isEmpty()) {
    return this
  }
  val knownBvids = mapTo(mutableSetOf()) { video -> video.bvid }
  return this + nextVideos.filter { video -> knownBvids.add(video.bvid) }
}

private const val FirstPage = 1
private const val PageSize = 20
