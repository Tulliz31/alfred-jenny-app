package com.alfredJenny.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alfredJenny.app.R
import com.alfredJenny.app.ui.theme.AccentGlow
import com.alfredJenny.app.ui.theme.AlfredBlue
import com.alfredJenny.app.ui.theme.AlfredBlueLight
import kotlinx.coroutines.delay

// ── Sprite frame lists ────────────────────────────────────────────────────────

private val IDLE_FRAMES = listOf(
    R.drawable.alfred_idle_000,
    R.drawable.alfred_idle_001,
    R.drawable.alfred_idle_002,
    R.drawable.alfred_idle_003
)
// Long rest on frame 0, quick blink through frames 1-3
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

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Animated Alfred avatar.
 *
 * Layer composition:
 *  1. LISTENING pulsing rings (when in LISTENING state)
 *  2. Sprite sheet (Crossfade between frames, per-state timing)
 *
 * State-specific Compose animations applied via graphicsLayer:
 *  • IDLE      — subtle heartbeat scale (1.0 → 1.02)
 *  • TALKING   — vertical bob (translationY 0 → −8dp)
 *  • THINKING  — gentle head tilt (rotationZ ±2.5°)
 *  • LISTENING — pulsing concentric rings + no transform on sprite
 */
@Composable
fun AlfredAvatarView(
    state: AlfredAvatarState,
    modifier: Modifier = Modifier,
) {
    // ── Sprite sequencer ─────────────────────────────────────────────────────
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

    // ── Continuous Compose animations ────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "alfred")

    // IDLE: heartbeat pulse
    val idleScale by infinite.animateFloat(
        initialValue = 1.0f, targetValue = 1.02f, label = "idle",
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    // TALKING: bob up/down
    val talkBob by infinite.animateFloat(
        initialValue = 0f, targetValue = -8f, label = "talk",
        animationSpec = infiniteRepeatable(tween(300, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    // THINKING: gentle head tilt
    val thinkRot by infinite.animateFloat(
        initialValue = -2.5f, targetValue = 2.5f, label = "think",
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    // LISTENING: ring scale + alpha
    val ringScale by infinite.animateFloat(
        initialValue = 1.0f, targetValue = 1.16f, label = "ring",
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.70f, targetValue = 0.12f, label = "ringA",
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    // ── Compositing ──────────────────────────────────────────────────────────
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 — Listening rings (behind sprite)
        if (state == AlfredAvatarState.LISTENING) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(236.dp)
                    .scale(ringScale)
                    .border(3.dp, AccentGlow.copy(alpha = ringAlpha), CircleShape)
            )
            // Inner ring
            Box(
                modifier = Modifier
                    .size(216.dp)
                    .scale(ringScale * 0.93f)
                    .border(1.5.dp, AlfredBlueLight.copy(alpha = ringAlpha * 0.55f), CircleShape)
            )
        }

        // Layer 2 — Sprite with per-state transform
        Crossfade(
            targetState = frames[frameIndex],
            animationSpec = tween(durationMillis = 80),
            label = "alfredFrame",
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    when (state) {
                        AlfredAvatarState.IDLE -> {
                            scaleX = idleScale
                            scaleY = idleScale
                        }
                        AlfredAvatarState.TALKING -> {
                            translationY = talkBob
                        }
                        AlfredAvatarState.THINKING -> {
                            rotationZ = thinkRot
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.6f)
                        }
                        AlfredAvatarState.LISTENING -> Unit
                    }
                }
        ) { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = "Alfred - $state",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}
