package com.alfredJenny.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.delay

// ── Sprite config ─────────────────────────────────────────────────────────────

data class AlfredSpriteConfig(
    val state: AlfredAvatarState,
    val frames: List<String>,
    val fps: Int,
    val loop: Boolean,
) {
    val frameDurationMs: Long get() = 1000L / fps.coerceAtLeast(1)
}

private val SPRITE_CONFIGS = mapOf(
    AlfredAvatarState.IDLE to AlfredSpriteConfig(
        state = AlfredAvatarState.IDLE,
        frames = listOf(
            "alfred_idle_000.png", "alfred_idle_001.png",
            "alfred_idle_002.png", "alfred_idle_003.png",
        ),
        fps = 2, loop = true,
    ),
    AlfredAvatarState.TALKING to AlfredSpriteConfig(
        state = AlfredAvatarState.TALKING,
        frames = listOf(
            "alfred_talking_000.png", "alfred_talking_001.png",
            "alfred_talking_002.png", "alfred_talking_003.png",
        ),
        fps = 8, loop = true,
    ),
    AlfredAvatarState.THINKING to AlfredSpriteConfig(
        state = AlfredAvatarState.THINKING,
        frames = listOf(
            "alfred_thinking_000.png", "alfred_thinking_001.png",
            "alfred_thinking_002.png",
        ),
        fps = 3, loop = true,
    ),
    AlfredAvatarState.LISTENING to AlfredSpriteConfig(
        state = AlfredAvatarState.LISTENING,
        frames = listOf(
            "alfred_listening_000.png", "alfred_listening_001.png",
            "alfred_listening_002.png",
        ),
        fps = 4, loop = true,
    ),
)

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Animated Alfred avatar using real PNG sprites loaded from assets/alfred/.
 *
 * Layers:
 *  1. LISTENING pulsing rings (behind sprite)
 *  2. Sprite (Crossfade, Coil AsyncImage from assets)
 *     • Bottom shadow (BlurMaskFilter ellipse, always visible)
 *     • TALKING: blue glow (BlurMaskFilter circle, animated alpha)
 *  3. Per-state Compose transforms via graphicsLayer
 *
 * Fallback: if assets/alfred/ is empty or missing, shows geometric avatar
 * with "Sprite non trovati" label instead of crashing.
 */
@Composable
fun AlfredAvatarView(
    state: AlfredAvatarState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val spriteAvailable = remember {
        try { context.assets.list("alfred")?.isNotEmpty() == true } catch (_: Exception) { false }
    }

    if (!spriteAvailable) {
        FallbackAvatarView(state = state, modifier = modifier)
        return
    }

    val config = SPRITE_CONFIGS[state] ?: SPRITE_CONFIGS[AlfredAvatarState.IDLE]!!

    var frameIndex by remember(state) { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        frameIndex = 0
        while (true) {
            delay(config.frameDurationMs)
            frameIndex = (frameIndex + 1) % config.frames.size
        }
    }

    // ── Continuous Compose animations ─────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "alfred")

    val idleScale by infinite.animateFloat(
        1.0f, 1.02f, label = "idle",
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
    )
    val talkBob by infinite.animateFloat(
        0f, -8f, label = "talk",
        animationSpec = infiniteRepeatable(tween(300, easing = EaseInOutSine), RepeatMode.Reverse),
    )
    val thinkRot by infinite.animateFloat(
        -2.5f, 2.5f, label = "think",
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
    )
    val ringScale by infinite.animateFloat(
        1.0f, 1.16f, label = "ring",
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
    )
    val ringAlpha by infinite.animateFloat(
        0.70f, 0.12f, label = "ringA",
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
    )
    val glowAlpha by infinite.animateFloat(
        0.35f, 0.65f, label = "glow",
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse),
    )

    // Reusable native paints (recreated only when state changes)
    val shadowPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val glowPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            maskFilter = android.graphics.BlurMaskFilter(52f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }

    val currentFileName = config.frames[frameIndex]
    val isTalking   = state == AlfredAvatarState.TALKING
    val isListening = state == AlfredAvatarState.LISTENING

    Box(
        modifier = modifier.graphicsLayer {},
        contentAlignment = Alignment.Center,
    ) {

        // Layer 1 — LISTENING rings (behind sprite)
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(236.dp)
                    .scale(ringScale)
                    .border(3.dp, AccentGlow.copy(alpha = ringAlpha), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(216.dp)
                    .scale(ringScale * 0.93f)
                    .border(1.5.dp, AlfredBlueLight.copy(alpha = ringAlpha * 0.55f), CircleShape),
            )
        }

        // Layer 2 — Sprite with shadow + talking glow via drawBehind
        Crossfade(
            targetState = currentFileName,
            animationSpec = tween(durationMillis = 200),
            label = "alfredFrame",
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        // Bottom shadow (always)
                        shadowPaint.color = AlfredBlue.copy(alpha = 0.22f).toArgb()
                        canvas.nativeCanvas.drawOval(
                            size.width * 0.20f, size.height * 0.88f,
                            size.width * 0.80f, size.height * 0.99f,
                            shadowPaint,
                        )
                        // TALKING: animated blue glow around sprite
                        if (isTalking) {
                            glowPaint.color = AlfredBlue.copy(alpha = glowAlpha * 0.6f).toArgb()
                            canvas.nativeCanvas.drawCircle(
                                size.width / 2f,
                                size.height * 0.50f,
                                size.minDimension * 0.38f,
                                glowPaint,
                            )
                        }
                    }
                }
                .graphicsLayer {
                    when (state) {
                        AlfredAvatarState.IDLE -> {
                            scaleX = idleScale
                            scaleY = idleScale
                        }
                        AlfredAvatarState.TALKING  -> translationY = talkBob
                        AlfredAvatarState.THINKING -> {
                            rotationZ = thinkRot
                            transformOrigin = TransformOrigin(0.5f, 0.6f)
                        }
                        AlfredAvatarState.LISTENING -> Unit
                    }
                },
        ) { fileName ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/alfred/$fileName")
                    .crossfade(false)
                    .build(),
                contentDescription = "Alfred - $state",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Geometric fallback ────────────────────────────────────────────────────────

@Composable
private fun FallbackAvatarView(
    state: AlfredAvatarState,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "fallback")
    val scale by infinite.animateFloat(
        1f, 1.05f, label = "fallbackScale",
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .border(2.dp, AlfredBlueLight.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", color = AlfredBlueLight, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Sprite non trovati",
                    color = AlfredBlueLight.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
