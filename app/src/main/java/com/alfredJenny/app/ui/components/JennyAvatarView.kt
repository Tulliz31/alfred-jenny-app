package com.alfredJenny.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alfredJenny.app.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 2.5D layered avatar for Jenny.
 *
 * Layers (bottom → top):
 *  1. Bokeh background  — slow parallax (25%)
 *  2. Body base          — anchored, breathing offset
 *  3. Eyes               — open / blink (random), slight parallax
 *  4. Mouth              — neutral / talk (looped when TALKING)
 *  5. Hair               — fast parallax (70%) + sway rotation
 *  6. Floating particles — decorative rising dots
 *  7. Vignette overlay   — depth framing
 *
 * Bloom/glow is applied to the body layer via a multi-circle drawBehind.
 * Parallax driven by TYPE_ACCELEROMETER (low-pass filtered).
 */
@Composable
fun JennyAvatarView(
    state: AlfredAvatarState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ── Accelerometer parallax ──────────────────────────────────────────────
    val parallaxX = remember { mutableStateOf(0f) }
    val parallaxY = remember { mutableStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var smoothX = 0f
        var smoothY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                smoothX = smoothX * 0.84f + event.values[0] * 0.16f
                smoothY = smoothY * 0.84f + event.values[1] * 0.16f
                parallaxX.value = (smoothX * 2.5f).coerceIn(-18f, 18f)
                // Gravity baseline: ~9.8 on Y when phone is upright
                parallaxY.value = ((smoothY - 9.8f) * 2.0f).coerceIn(-18f, 18f)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // ── Continuous animations ───────────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "jenny")

    val breathOffset by infinite.animateFloat(
        initialValue = 0f, targetValue = 5f, label = "breath",
        animationSpec = infiniteRepeatable(tween(3400, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    val hairSway by infinite.animateFloat(
        initialValue = -1f, targetValue = 1f, label = "hair",
        animationSpec = infiniteRepeatable(tween(2700, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    // ── Blink (random 2.5–5 s interval, double-blink) ──────────────────────
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 5000))
            isBlinking = true;  delay(120)
            isBlinking = false; delay(90)
            // occasional double-blink
            if (Random.nextFloat() > 0.6f) {
                isBlinking = true;  delay(100)
                isBlinking = false
            }
        }
    }

    // ── Talking mouth ───────────────────────────────────────────────────────
    var talkOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == AlfredAvatarState.TALKING) {
            while (true) {
                talkOpen = !talkOpen
                delay(Random.nextLong(70, 170))
            }
        } else {
            talkOpen = false
        }
    }

    // ── React micro-animation (on new state entry) ──────────────────────────
    val reactScale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        when (state) {
            AlfredAvatarState.THINKING -> {
                reactScale.animateTo(1.04f, tween(75))
                reactScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            }
            AlfredAvatarState.LISTENING -> {
                reactScale.animateTo(1.03f, tween(120))
                reactScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
            }
            else -> Unit
        }
    }

    // ── Compositing ─────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .width(225.dp)
            .height(360.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 — Bokeh background
        Image(
            painter = painterResource(R.drawable.jenny_bg_bokeh),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX.value * 0.25f
                    translationY = parallaxY.value * 0.25f
                    scaleX = 1.08f   // zoom-out so edges don't show during parallax
                    scaleY = 1.08f
                }
        )

        // Layer 2 — Body base (breathing + react)
        Image(
            painter = painterResource(R.drawable.jenny_body_base),
            contentDescription = "Jenny",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .jennyGlow()
                .graphicsLayer {
                    translationY = breathOffset
                    scaleX = reactScale.value
                    scaleY = reactScale.value
                }
        )

        // Layer 3 — Eyes
        Image(
            painter = painterResource(if (isBlinking) R.drawable.jenny_eyes_blink else R.drawable.jenny_eyes_open),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX.value * 0.14f
                    translationY = breathOffset + parallaxY.value * 0.08f
                    scaleX = reactScale.value
                    scaleY = reactScale.value
                }
        )

        // Layer 4 — Mouth
        Image(
            painter = painterResource(
                if (talkOpen && state == AlfredAvatarState.TALKING)
                    R.drawable.jenny_mouth_talk
                else
                    R.drawable.jenny_mouth_neutral
            ),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX.value * 0.10f
                    translationY = breathOffset + parallaxY.value * 0.06f
                    scaleX = reactScale.value
                    scaleY = reactScale.value
                }
        )

        // Layer 5 — Hair (higher parallax + sway)
        Image(
            painter = painterResource(R.drawable.jenny_hair_layer),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX.value * 0.70f + hairSway * 3.5f
                    translationY = parallaxY.value * 0.38f + breathOffset * 0.55f
                    rotationZ = hairSway * 1.4f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
        )

        // Layer 6 — Floating particles
        FloatingParticles(modifier = Modifier.fillMaxSize())

        // Layer 7 — Vignette
        VignetteOverlay(modifier = Modifier.fillMaxSize())
    }
}

// ── Bloom / glow ─────────────────────────────────────────────────────────────

/**
 * Soft purple glow drawn behind the figure using layered translucent ellipses.
 * Works on all API levels without needing BlurMaskFilter or RenderEffect.
 */
private fun Modifier.jennyGlow(): Modifier = this.drawBehind {
    val cx = size.width * 0.5f
    val cy = size.height * 0.38f
    // Draw concentric glow rings, outermost first
    val glowColor = Color(0xFFB060FF)
    for (ring in 6 downTo 1) {
        drawCircle(
            color = glowColor,
            radius = size.width * (0.28f + ring * 0.06f),
            center = Offset(cx, cy),
            alpha = 0.028f
        )
    }
    // Tighter bright core
    drawCircle(color = glowColor, radius = size.width * 0.22f, center = Offset(cx, cy), alpha = 0.06f)
}

// ── Floating particles ───────────────────────────────────────────────────────

@Composable
private fun FloatingParticles(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "particles")

    // 8 particles with explicit per-particle animation specs
    val p0 by t.animateFloat(0f, 1f, label = "p0", animationSpec = infiniteRepeatable(tween(5100, easing = LinearEasing), RepeatMode.Restart))
    val p1 by t.animateFloat(0f, 1f, label = "p1", animationSpec = infiniteRepeatable(tween(4300, delayMillis = 1100, easing = LinearEasing), RepeatMode.Restart))
    val p2 by t.animateFloat(0f, 1f, label = "p2", animationSpec = infiniteRepeatable(tween(6200, delayMillis = 700, easing = LinearEasing), RepeatMode.Restart))
    val p3 by t.animateFloat(0f, 1f, label = "p3", animationSpec = infiniteRepeatable(tween(3900, delayMillis = 2500, easing = LinearEasing), RepeatMode.Restart))
    val p4 by t.animateFloat(0f, 1f, label = "p4", animationSpec = infiniteRepeatable(tween(5500, delayMillis = 900, easing = LinearEasing), RepeatMode.Restart))
    val p5 by t.animateFloat(0f, 1f, label = "p5", animationSpec = infiniteRepeatable(tween(4800, delayMillis = 1900, easing = LinearEasing), RepeatMode.Restart))
    val p6 by t.animateFloat(0f, 1f, label = "p6", animationSpec = infiniteRepeatable(tween(5900, delayMillis = 500, easing = LinearEasing), RepeatMode.Restart))
    val p7 by t.animateFloat(0f, 1f, label = "p7", animationSpec = infiniteRepeatable(tween(4600, delayMillis = 3100, easing = LinearEasing), RepeatMode.Restart))

    // (xFraction 0..1, x-wobble amplitude px, radius dp)
    val cfg = remember {
        listOf(
            Triple(0.12f,  8f, 2.4f),
            Triple(0.26f, -7f, 3.0f),
            Triple(0.41f, 10f, 2.0f),
            Triple(0.56f, -9f, 3.4f),
            Triple(0.69f,  7f, 2.8f),
            Triple(0.83f,-11f, 2.2f),
            Triple(0.33f, 13f, 1.8f),
            Triple(0.74f, -5f, 3.2f)
        )
    }
    val progresses = listOf(p0, p1, p2, p3, p4, p5, p6, p7)

    Canvas(modifier = modifier) {
        progresses.forEachIndexed { i, progress ->
            val (xFrac, wobble, radiusDp) = cfg[i]
            val y = size.height * (1f - progress)
            val x = size.width * xFrac + sin(progress * 2f * PI.toFloat() * 1.5f) * wobble
            val alpha = when {
                progress < 0.12f -> progress / 0.12f
                progress > 0.88f -> (1f - progress) / 0.12f
                else -> 1f
            } * 0.65f
            drawCircle(
                color = Color(0xFFD4A8FF),
                radius = radiusDp * density,
                center = Offset(x, y),
                alpha = alpha
            )
        }
    }
}

// ── Vignette overlay ─────────────────────────────────────────────────────────

@Composable
private fun VignetteOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x70000020)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = maxOf(size.width, size.height) * 0.62f
            )
        )
    }
}
