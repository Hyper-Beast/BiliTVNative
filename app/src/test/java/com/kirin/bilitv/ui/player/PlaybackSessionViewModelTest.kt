package com.kirin.bilitv.ui.player

import androidx.lifecycle.SavedStateHandle
import com.kirin.bilitv.core.player.PlaybackRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSessionViewModelTest {
  @Test
  fun startOrUpdateStoresActiveRequest() {
    val request = playbackRequest()
    val savedStateHandle = SavedStateHandle()
    val viewModel = PlaybackSessionViewModel(savedStateHandle = savedStateHandle)

    viewModel.startOrUpdate(request)

    assertEquals(request, viewModel.activeRequest.value)
    assertEquals(request, savedStateHandle.get<PlaybackRequest>(SavedPlaybackRequestKey))
  }

  @Test
  fun restoresRequestFromSavedStateHandle() {
    val request = playbackRequest()
    val viewModel = PlaybackSessionViewModel(
      savedStateHandle = SavedStateHandle(mapOf(SavedPlaybackRequestKey to request)),
    )

    assertEquals(request, viewModel.activeRequest.value)
  }

  @Test
  fun clearRemovesActiveAndSavedRequest() {
    val request = playbackRequest()
    val savedStateHandle = SavedStateHandle(mapOf(SavedPlaybackRequestKey to request))
    val viewModel = PlaybackSessionViewModel(savedStateHandle = savedStateHandle)

    viewModel.clear()

    assertNull(viewModel.activeRequest.value)
    assertNull(savedStateHandle.get<PlaybackRequest>(SavedPlaybackRequestKey))
  }

  @Test
  fun playbackRequestSurvivesSerializationRoundTrip() {
    val request = playbackRequest()
    val bytes = ByteArrayOutputStream().use { output ->
      ObjectOutputStream(output).use { stream -> stream.writeObject(request) }
      output.toByteArray()
    }
    val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { stream ->
      stream.readObject() as PlaybackRequest
    }

    assertEquals(request, restored)
  }

  private fun playbackRequest(): PlaybackRequest {
    return PlaybackRequest(
      bvid = "BV1TEST",
      cid = 123L,
      title = "Saved playback",
      startPositionMs = 45_000L,
      aid = 456L,
      ownerName = "owner",
      ownerFace = "https://example.com/face.jpg",
      ownerMid = 789L,
      viewCount = 10,
      danmakuCount = 20,
      pubdate = 1_700_000_000L,
      preferredQualityId = 80,
      forceStartPosition = true,
      historyPage = 2,
      advanceToNextHistoryEpisode = false,
    )
  }
}
