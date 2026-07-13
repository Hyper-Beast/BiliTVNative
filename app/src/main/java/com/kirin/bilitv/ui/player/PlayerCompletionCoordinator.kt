package com.kirin.bilitv.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PlayerCompletionCoordinator {
  var reported by mutableStateOf(false)
    private set

  private var actionToken = 0L
  private var actionJob: Job? = null

  fun markReported(): Boolean {
    if (reported) return false
    reported = true
    return true
  }

  fun clearReported() {
    reported = false
  }

  fun cancelPendingAction() {
    actionJob?.cancel()
    actionJob = null
    actionToken += 1L
  }

  fun dispose() {
    actionJob?.cancel()
    actionJob = null
    actionToken += 1L
  }

  fun launchAction(
    coroutineScope: CoroutineScope,
    block: suspend PlayerCompletionActionScope.() -> Unit,
  ) {
    actionJob?.cancel()
    val token = ++actionToken
    actionJob = coroutineScope.launch {
      try {
        PlayerCompletionActionScope(
          token = token,
          isCurrentAction = { activeToken -> actionToken == activeToken && reported },
        ).block()
      } finally {
        if (actionToken == token) {
          actionJob = null
        }
      }
    }
  }
}

internal class PlayerCompletionActionScope internal constructor(
  private val token: Long,
  private val isCurrentAction: (Long) -> Boolean,
) {
  fun isActive(): Boolean {
    return isCurrentAction(token)
  }

  suspend fun delayIfActive(delayMs: Long): Boolean {
    delay(delayMs)
    return isActive()
  }
}
