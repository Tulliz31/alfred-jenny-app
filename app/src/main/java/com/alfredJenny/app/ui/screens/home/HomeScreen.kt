package com.alfredJenny.app.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.local.ConversationEntity
import com.alfredJenny.app.data.model.CompanionDto
import com.alfredJenny.app.ui.theme.*

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selected = state.companions.firstOrNull { it.id == state.selectedCompanionId }
            CompanionAvatar(
                companion = selected,
                isThinking = state.isLoading
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    selected?.name ?: "Alfred",
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    fontSize = 18.sp
                )
                Text(
                    if (state.isLoading) "sta scrivendo..." else "online",
                    color = if (state.isLoading) AccentGlow else SuccessGreen,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = OnSurfaceVariant)
            }
        }

        // ── Companion selector (admin only, more than 1 companion) ────────────
        if (state.companions.size > 1) {
            CompanionSelectorRow(
                companions = state.companions,
                selectedId = state.selectedCompanionId,
                onSelect = viewModel::onCompanionSelected
            )
        }

        // ── Messages ──────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
            if (state.isLoading) {
                item { TypingIndicator() }
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        state.error?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorRed.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    err,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::dismissError) {
                    Text("OK", color = ErrorRed)
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChange,
                placeholder = { Text("Scrivi un messaggio...", color = OnSurfaceVariant) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlfredBlueLight,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = AlfredBlueLight
                ),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = viewModel::sendMessage,
                enabled = state.inputText.isNotBlank() && !state.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (state.inputText.isNotBlank()) AlfredBlue else SurfaceVariant)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Invia", tint = OnBackground)
            }
        }
    }
}

// ── Companion selector tabs ───────────────────────────────────────────────────

@Composable
private fun CompanionSelectorRow(
    companions: List<CompanionDto>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        companions.forEach { companion ->
            val isSelected = companion.id == selectedId
            val accentColor = if (companion.id == "jenny") JennyPurple else AlfredBlue
            val borderColor = if (companion.id == "jenny") JennyPurpleLight else AlfredBlueLight

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.2f),
                modifier = Modifier.clickable { onSelect(companion.id) }
            ) {
                Text(
                    companion.name,
                    color = if (isSelected) OnBackground else borderColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ── Avatar ────────────────────────────────────────────────────────────────────

@Composable
private fun CompanionAvatar(companion: CompanionDto?, isThinking: Boolean) {
    val isJenny = companion?.id == "jenny"
    val gradientColors = if (isJenny) listOf(JennyPurpleLight, JennyPurpleDark)
                         else listOf(AlfredBlueLight, AlfredBlueDark)

    val pulse = rememberInfiniteTransition(label = "avatar")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isThinking) 1.15f else 1.04f,
        label = "avatarScale",
        animationSpec = infiniteRepeatable(
            tween(if (isThinking) 600 else 2000, easing = EaseInOutSine),
            RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.radialGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            companion?.name?.first()?.toString() ?: "A",
            color = OnBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ConversationEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) BubbleUser else BubbleAssistant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.content, color = OnBackground, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Typing indicator ──────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BubbleAssistant)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f, label = "dot$index",
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = index * 150),
                    RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = alpha))
            )
        }
    }
}
