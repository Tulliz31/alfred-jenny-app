package com.alfredJenny.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.ui.theme.*

@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        destination?.let { onNavigate(it) }
    }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AlfredBlueDark, Background),
                    radius = 1400f,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Glow ring behind the logo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((110 * scale).dp)
                        .clip(CircleShape)
                        .background(AlfredBlueLight.copy(alpha = glowAlpha * 0.3f))
                )
                Box(
                    modifier = Modifier
                        .size((80 * scale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AlfredBlueLight, AlfredBlueDark)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "AJ",
                        color = OnBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Alfred & Jenny",
                style = MaterialTheme.typography.headlineMedium,
                color = OnBackground,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )

            Text(
                "Il tuo assistente AI personale",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
