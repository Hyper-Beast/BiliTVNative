package com.kirin.bilitv.ui.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kirin.bilitv.core.model.VideoSummary
import com.kirin.bilitv.ui.theme.BiliMotion

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalPlaybackSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalPlaybackSharedAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaybackSharedTransitionLayout(content: @Composable () -> Unit) {
  SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
    CompositionLocalProvider(
      LocalPlaybackSharedTransitionScope provides this,
    ) {
      content()
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaybackSharedAnimatedScope(
  animatedVisibilityScope: AnimatedVisibilityScope,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalPlaybackSharedAnimatedVisibilityScope provides animatedVisibilityScope,
  ) {
    content()
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.playbackSharedBounds(
  key: String?,
  enabled: Boolean = true,
): Modifier {
  val sharedScope = LocalPlaybackSharedTransitionScope.current
  val animatedVisibilityScope = LocalPlaybackSharedAnimatedVisibilityScope.current
  if (!enabled || key.isNullOrBlank() || sharedScope == null || animatedVisibilityScope == null) {
    return this
  }

  return with(sharedScope) {
    sharedElement(
      sharedContentState = rememberSharedContentState(key),
      animatedVisibilityScope = animatedVisibilityScope,
      zIndexInOverlay = 1f,
      boundsTransform = BoundsTransform { _, _ ->
        tween(
          durationMillis = BiliMotion.PlaybackHeroTransitionMs,
          easing = BiliMotion.PlaybackHeroEasing,
        )
      },
    )
  }
}

fun VideoSummary.playbackSharedTransitionKey(): String {
  return bvid.ifBlank {
    when {
      cid > 0L -> "cid-$cid"
      pic.isNotBlank() -> "pic-${pic.hashCode()}"
      else -> title
    }
  }
}
