package com.alfredJenny.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Bitmap loader ─────────────────────────────────────────────────────────────

/**
 * Loads an ImageBitmap from the app's assets folder on IO dispatcher.
 * Returns null while loading or on error.
 * Result is remembered per [assetPath] + [sampleSize].
 */
@Composable
fun rememberAssetBitmap(assetPath: String, sampleSize: Int = 1): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(assetPath, sampleSize) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(assetPath, sampleSize) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.assets.open("jenny/$assetPath").use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

// ── Main composable ───────────────────────────────────────────────────────────

/**
 * Puppet-style Jenny avatar built from layered transparent PNGs.
 *
 * Layers (bottom → top):
 *  1. Body   — outfit-specific full-body sprite + breathing bob + glow ring
 *  2. Eyes   — emotion/blink state, Crossfade 150 ms
 *  3. Mouth  — lip-sync state driven by audioAmplitude
 *  4. Stars  — floating particle system (rising glitter)
 *  5. Vignette — purple radial vignette
 *
 * Overlay positioning (BoxWithConstraints):
 *  • Eyes   centred at 35% of body height from top
 *  • Mouth  centred at 52% of body height from top
 *
 * Outfit bar row is rendered at the bottom of the outer Column.
 */
@Composable
fun JennyAvatarView(
    state: AlfredAvatarState,
    outfit: JennyOutfit = JennyOutfit.CASUAL,
    eyeEmotion: EyeState = EyeState.OPEN,
    audioAmplitude: Float = 0f,
    onOutfitChange: (JennyOutfit) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // ── Accelerometer parallax ────────────────────────────────────────────────
    val parallaxX = remember { mutableStateOf(0f) }
    val parallaxY = remember { mutableStateOf(0f) }
    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var sx = 0f; var sy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                sx = sx * 0.85f + e.values[0] * 0.15f
                sy = sy * 0.85f + e.values[1] * 0.15f
                parallaxX.value = (sx * 2.5f).coerceIn(-16f, 16f)
                parallaxY.value = ((sy - 9.8f) * 1.8f).coerceIn(-16f, 16f)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        accel?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sm.unregisterListener(listener) }
    }

    // ── Continuous animations ─────────────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "jenny")
    val breathOffset by infinite.animateFloat(
        0f, 5f, label = "breath",
        animationSpec = infiniteRepeatable(tween(3400, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val reactScale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        when (state) {
            AlfredAvatarState.THINKING  -> {
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

    // ── Blink (random 3–6 s, full sequence: HALF → CLOSED → HALF) ────────────
    var blinkPhase by remember { mutableStateOf<EyeState?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 6000))
            blinkPhase = EyeState.HALF;  delay(55)
            blinkPhase = EyeState.CLOSED; delay(80)
            blinkPhase = EyeState.HALF;  delay(55)
            blinkPhase = null
            if (Random.nextFloat() > 0.55f) {
                delay(110)
                blinkPhase = EyeState.HALF;  delay(55)
                blinkPhase = EyeState.CLOSED; delay(75)
                blinkPhase = EyeState.HALF;  delay(55)
                blinkPhase = null
            }
        }
    }
    val effectiveEyeState = blinkPhase ?: eyeEmotion

    // ── Mouth via amplitude ───────────────────────────────────────────────────
    val mouthState = if (state == AlfredAvatarState.TALKING) {
        when {
            audioAmplitude < 0.2f -> MouthState.CLOSED
            audioAmplitude < 0.4f -> MouthState.OPEN_S
            audioAmplitude < 0.7f -> MouthState.OPEN_M
            else                  -> MouthState.OPEN_L
        }
    } else {
        MouthState.SMILE
    }

    // ── Bitmap preloading ─────────────────────────────────────────────────────
    // Body — all outfits preloaded upfront so Crossfade(300ms) has both bitmaps ready
    val bodyBitmaps = remember { mutableStateMapOf<JennyOutfit, ImageBitmap>() }
    LaunchedEffect(Unit) {
        JennyOutfit.values().forEach { o ->
            val bmp = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open("jenny/${o.assetFile}").use { s ->
                        BitmapFactory.decodeStream(s)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            if (bmp != null) bodyBitmaps[o] = bmp
        }
    }

    // Eyes — preloaded for current effective state (Crossfade handles transitions)
    val eyeBitmap = rememberAssetBitmap(effectiveEyeState.assetFile)

    // Mouth — current state
    val mouthBitmap = rememberAssetBitmap(mouthState.assetFile)

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(modifier = modifier) {

        // ── Puppet canvas ─────────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val bW = maxWidth
            val bH = maxHeight

            // Layer 1 — Body (glow + breathing + react) — Crossfade 300ms on outfit change
            Crossfade(
                targetState = outfit,
                animationSpec = tween(300),
                label = "bodyOutfit"
            ) { currentOutfit ->
                bodyBitmaps[currentOutfit]?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = "Jenny",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .jennyBodyGlow()
                            .graphicsLayer {
                                translationX = parallaxX.value * 0.08f
                                translationY = breathOffset + parallaxY.value * 0.04f
                                scaleX = reactScale.value
                                scaleY = reactScale.value
                            }
                    )
                }
            }

            // Layer 2 — Eyes (Crossfade 150 ms between states)
            eyeBitmap?.let { bmp ->
                val eyeW = bW * 0.54f
                val eyeH = eyeW * (bmp.height.toFloat() / bmp.width.toFloat())
                Crossfade(
                    targetState = bmp,
                    animationSpec = tween(150),
                    label = "eyes",
                    modifier = Modifier
                        .size(eyeW, eyeH)
                        .align(Alignment.TopCenter)
                        .offset(
                            x = (parallaxX.value * 0.14f).dp,
                            y = (bH * 0.35f) - (eyeH / 2) + (breathOffset + parallaxY.value * 0.08f).dp
                        )
                        .graphicsLayer {
                            scaleX = reactScale.value
                            scaleY = reactScale.value
                        }
                ) { b ->
                    Image(
                        bitmap = b,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Layer 3 — Mouth (120 ms lip-sync cycle)
            mouthBitmap?.let { bmp ->
                val mW = bW * 0.38f
                val mH = mW * (bmp.height.toFloat() / bmp.width.toFloat())
                Crossfade(
                    targetState = bmp,
                    animationSpec = tween(80),
                    label = "mouth",
                    modifier = Modifier
                        .size(mW, mH)
                        .align(Alignment.TopCenter)
                        .offset(
                            x = (parallaxX.value * 0.10f).dp,
                            y = (bH * 0.52f) - (mH / 2) + (breathOffset + parallaxY.value * 0.06f).dp
                        )
                        .graphicsLayer {
                            scaleX = reactScale.value
                            scaleY = reactScale.value * if (state == AlfredAvatarState.TALKING)
                                0.85f + audioAmplitude * 0.35f else 1f
                        }
                ) { b ->
                    Image(
                        bitmap = b,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Layer 4 — Floating stars
            JennyStarParticles(modifier = Modifier.fillMaxSize())

            // Layer 5 — Vignette
            JennyVignette(modifier = Modifier.fillMaxSize())
        }

        // ── Outfit bar ────────────────────────────────────────────────────────
        JennyOutfitBar(
            currentOutfit = outfit,
            onSelect = onOutfitChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

// ── Outfit bar ────────────────────────────────────────────────────────────────

@Composable
fun JennyOutfitBar(
    currentOutfit: JennyOutfit,
    onSelect: (JennyOutfit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JennyOutfit.values().forEach { o ->
            val thumb = rememberAssetBitmap(o.assetFile, sampleSize = 4)
            val isActive = (o == currentOutfit)
            Box(
                modifier = Modifier
                    .size(52.dp, 72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) JennyPurple.copy(alpha = 0.5f) else SurfaceVariant
                    )
                    .border(
                        width = if (isActive) 2.dp else 0.5.dp,
                        color = if (isActive) JennyPurpleLight else SurfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = o.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Label at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = o.label,
                        color = if (isActive) JennyPurpleLight else OnSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Bloom / glow ──────────────────────────────────────────────────────────────

private fun Modifier.jennyBodyGlow(): Modifier = this.drawBehind {
    val cx = size.width * 0.5f
    val cy = size.height * 0.36f
    val glowColor = Color(0xFFB060FF)
    for (ring in 7 downTo 1) {
        drawCircle(
            color = glowColor,
            radius = size.width * (0.26f + ring * 0.06f),
            center = Offset(cx, cy),
            alpha = 0.025f
        )
    }
    drawCircle(color = glowColor, radius = size.width * 0.20f, center = Offset(cx, cy), alpha = 0.055f)
}

// ── Floating star particles ───────────────────────────────────────────────────

@Composable
private fun JennyStarParticles(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "stars")

    val s0 by t.animateFloat(0f, 1f, label = "s0", animationSpec = infiniteRepeatable(tween(5100, easing = LinearEasing), RepeatMode.Restart))
    val s1 by t.animateFloat(0f, 1f, label = "s1", animationSpec = infiniteRepeatable(tween(4300, delayMillis = 1100, easing = LinearEasing), RepeatMode.Restart))
    val s2 by t.animateFloat(0f, 1f, label = "s2", animationSpec = infiniteRepeatable(tween(6200, delayMillis = 700, easing = LinearEasing), RepeatMode.Restart))
    val s3 by t.animateFloat(0f, 1f, label = "s3", animationSpec = infiniteRepeatable(tween(3900, delayMillis = 2500, easing = LinearEasing), RepeatMode.Restart))
    val s4 by t.animateFloat(0f, 1f, label = "s4", animationSpec = infiniteRepeatable(tween(5500, delayMillis = 900, easing = LinearEasing), RepeatMode.Restart))
    val s5 by t.animateFloat(0f, 1f, label = "s5", animationSpec = infiniteRepeatable(tween(4800, delayMillis = 1900, easing = LinearEasing), RepeatMode.Restart))
    val s6 by t.animateFloat(0f, 1f, label = "s6", animationSpec = infiniteRepeatable(tween(5900, delayMillis = 500, easing = LinearEasing), RepeatMode.Restart))
    val s7 by t.animateFloat(0f, 1f, label = "s7", animationSpec = infiniteRepeatable(tween(4600, delayMillis = 3100, easing = LinearEasing), RepeatMode.Restart))

    // (xFraction, wobble px, starSize dp, rotationOffset deg)
    val cfg = remember {
        listOf(
            listOf(0.10f,  8f, 3.0f, 0f),
            listOf(0.24f, -6f, 2.4f, 45f),
            listOf(0.39f, 10f, 2.8f, 22f),
            listOf(0.54f, -9f, 3.2f, 67f),
            listOf(0.68f,  7f, 2.6f, 10f),
            listOf(0.82f,-10f, 2.2f, 55f),
            listOf(0.31f, 12f, 2.0f, 33f),
            listOf(0.73f, -5f, 3.4f, 78f),
        )
    }
    val progresses = listOf(s0, s1, s2, s3, s4, s5, s6, s7)
    val starColor = Color(0xFFE8C8FF)

    Canvas(modifier = modifier) {
        progresses.forEachIndexed { i, progress ->
            val (xFrac, wobble, sizeDp, rotDeg) = cfg[i]
            val y = size.height * (1f - progress)
            val x = size.width * xFrac + sin(progress * 2f * PI.toFloat() * 1.5f) * wobble
            val alpha = when {
                progress < 0.12f -> (progress / 0.12f) * 0.7f
                progress > 0.88f -> ((1f - progress) / 0.12f) * 0.7f
                else             -> 0.7f
            }
            val r = sizeDp * density
            val angle = rotDeg * PI.toFloat() / 180f

            // 4-pointed star (diamond with arm tips)
            val path = androidx.compose.ui.graphics.Path()
            val outerR = r
            val innerR = r * 0.42f
            for (k in 0 until 8) {
                val a = angle + k * (PI.toFloat() / 4f)
                val rr = if (k % 2 == 0) outerR else innerR
                val px = x + rr * cos(a)
                val py = y + rr * sin(a)
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color = starColor, alpha = alpha)
        }
    }
}

// ── Vignette ──────────────────────────────────────────────────────────────────

@Composable
private fun JennyVignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x80100018)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = maxOf(size.width, size.height) * 0.62f
            )
        )
    }
}
