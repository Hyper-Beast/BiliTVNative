package com.kirin.bilitv.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kirin.bilitv.core.player.PlaybackRequest
import kotlinx.coroutines.flow.StateFlow

internal class PlaybackSessionViewModel(
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val activeRequest: StateFlow<PlaybackRequest?> = savedStateHandle.getStateFlow(
    key = SavedPlaybackRequestKey,
    initialValue = null,
  )

  fun startOrUpdate(request: PlaybackRequest) {
    if (activeRequest.value == request) return

    savedStateHandle[SavedPlaybackRequestKey] = request
  }

  fun clear() {
    if (activeRequest.value == null) return

    savedStateHandle[SavedPlaybackRequestKey] = null
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        PlaybackSessionViewModel(savedStateHandle = createSavedStateHandle())
      }
    }
  }
}

internal const val SavedPlaybackRequestKey = "active_playback_request"
