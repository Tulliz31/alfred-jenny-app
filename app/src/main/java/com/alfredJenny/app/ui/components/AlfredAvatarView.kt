package com.alfredJenny.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alfredJenny.app.R
import kotlinx.coroutines.delay

private val IDLE_FRAMES = listOf(
    R.drawable.alfred_idle_000,
    R.drawable.alfred_idle_001,
    R.drawable.alfred_idle_002,
    R.drawable.alfred_idle_003
)
// Idle blink: long pause on frame 0, quick blink through 1-2-3, back
private val IDLE_DURATIONS = listOf(2800L, 80L, 80L, 80L)

private val TALKING_FRAMES = listOf(
    R.drawable.alfred_talking_000,
    R.drawable.alfred_talking_001,
    R.drawable.alfred_talking_002,
    R.drawable.alfred_talking_003
)
private val TALKING_DURATIONS = listOf(120L, 120L, 120L, 120L)

private val THINKING_FRAMES = listOf(
    R.drawable.alfred_thinking_000,
    R.drawable.alfred_thinking_001,
    R.drawable.alfred_thinking_002
)
private val THINKING_DURATIONS = listOf(500L, 500L, 800L)

private val LISTENING_FRAMES = listOf(
    R.drawable.alfred_listening_000,
    R.drawable.alfred_listening_001,
    R.drawable.alfred_listening_002
)
private val LISTENING_DURATIONS = listOf(350L, 350L, 350L)

@Composable
fun AlfredAvatarView(
    state: AlfredAvatarState,
    modifier: Modifier = Modifier
) {
    val (frames, durations) = remember(state) {
        when (state) {
            AlfredAvatarState.IDLE      -> IDLE_FRAMES      to IDLE_DURATIONS
            AlfredAvatarState.TALKING   -> TALKING_FRAMES   to TALKING_DURATIONS
            AlfredAvatarState.THINKING  -> THINKING_FRAMES  to THINKING_DURATIONS
            AlfredAvatarState.LISTENING -> LISTENING_FRAMES to LISTENING_DURATIONS
        }
    }

    var frameIndex by remember(state) { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        frameIndex = 0
        while (true) {
            delay(durations[frameIndex])
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val currentRes = frames[frameIndex]

    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = currentRes,
            animationSpec = tween(durationMillis = 80),
            label = "avatarFrame"
        ) { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = "Alfred avatar - $state",
                modifier = Modifier.size(300.dp)
            )
        }
    }
}
