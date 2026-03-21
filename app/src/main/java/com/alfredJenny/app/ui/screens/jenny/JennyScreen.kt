package com.alfredJenny.app.ui.screens.jenny

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfredJenny.app.ui.theme.*

/**
 * Jenny Screen — companion admin interface (placeholder for future features).
 */
@Composable
fun JennyScreen() {
    val pulse = rememberInfiniteTransition(label = "jenny")
    val scale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f, label = "jennyScale",
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size((90 * scale).dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(JennyPurpleLight, JennyPurpleDark))),
                contentAlignment = Alignment.Center
            ) {
                Text("J", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 36.sp)
            }
            Text("Jenny", style = MaterialTheme.typography.displayLarge, color = OnBackground)
            Text(
                "Companion admin — prossimamente",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
    }
}
