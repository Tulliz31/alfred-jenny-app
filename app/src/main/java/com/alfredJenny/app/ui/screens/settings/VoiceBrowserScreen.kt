package com.alfredJenny.app.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.data.model.ElevenLabsVoice
import com.alfredJenny.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceBrowserScreen(
    onBack: () -> Unit,
    viewModel: VoiceBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = if (state.companionId == "jenny") JennyPurpleLight else AlfredBlueLight
    val trackColor  = if (state.companionId == "jenny") JennyPurple else AlfredBlue

    val companionLabel = if (state.companionId == "jenny") "Jenny" else "Alfred"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scegli voce · $companionLabel", color = OnBackground, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface),
            )
        },
        containerColor = Background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Cerca voce...", color = OnSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Default.Close, null, tint = OnSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor, unfocusedBorderColor = OnSurfaceVariant,
                    focusedTextColor = OnBackground, unfocusedTextColor = OnBackground,
                    cursorColor = accentColor,
                ),
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("Tutte", state.filterGender == "" && state.filterAccent == "", accentColor) {
                    viewModel.onGenderFilter(""); viewModel.onAccentFilter("")
                }
                FilterChip("Femminile", state.filterGender == "female", accentColor) {
                    viewModel.onGenderFilter(if (state.filterGender == "female") "" else "female")
                }
                FilterChip("Maschile", state.filterGender == "male", accentColor) {
                    viewModel.onGenderFilter(if (state.filterGender == "male") "" else "male")
                }
                FilterChip("Italiano", state.filterAccent == "italian", accentColor) {
                    viewModel.onAccentFilter(if (state.filterAccent == "italian") "" else "italian")
                }
                FilterChip("Americano", state.filterAccent == "american", accentColor) {
                    viewModel.onAccentFilter(if (state.filterAccent == "american") "" else "american")
                }
                FilterChip("Britannico", state.filterAccent == "british", accentColor) {
                    viewModel.onAccentFilter(if (state.filterAccent == "british") "" else "british")
                }
            }

            // Subscription footer info
            state.subscription?.let { sub ->
                LinearProgressIndicator(
                    progress = { (sub.characterCount.toFloat() / sub.characterLimit.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor, trackColor = SurfaceVariant,
                )
                Text(
                    "${sub.characterCount.toLocale()} / ${sub.characterLimit.toLocale()} caratteri · ${sub.tier}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = OnSurfaceVariant, fontSize = 11.sp,
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(state.error!!, color = ErrorRed)
                        TextButton(onClick = { /* reload */ }) {
                            Text("Riprova", color = accentColor)
                        }
                    }
                }
            } else {
                val filtered = viewModel.filteredVoices()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.voiceId }) { voice ->
                        VoiceCard(
                            voice = voice,
                            isCurrentVoice = voice.voiceId == state.currentVoiceId,
                            isPlaying = voice.voiceId == state.playingVoiceId,
                            accentColor = accentColor,
                            trackColor = trackColor,
                            onPlay = { viewModel.playPreview(voice.voiceId) },
                            onStop = { viewModel.stopPreview() },
                            onSelect = { viewModel.selectVoice(voice.voiceId); onBack() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCard(
    voice: ElevenLabsVoice,
    isCurrentVoice: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    trackColor: Color,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onSelect: () -> Unit,
) {
    val borderAnim by animateColorAsState(
        targetValue = when {
            isCurrentVoice -> accentColor
            isPlaying      -> accentColor.copy(alpha = 0.6f)
            else           -> SurfaceVariant
        },
        label = "borderAnim",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha",
    )
    val borderColor = if (isPlaying) accentColor.copy(alpha = pulseAlpha) else borderAnim

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentVoice) trackColor.copy(alpha = 0.12f) else SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
            .border(width = if (isCurrentVoice || isPlaying) 1.5.dp else 1.dp,
                    color = borderColor, shape = RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null,
                    tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(voice.name, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    val meta = listOfNotNull(
                        voice.gender.ifBlank { null }?.replaceFirstChar { it.uppercase() },
                        voice.accent.ifBlank { null }?.replaceFirstChar { it.uppercase() },
                        voice.age.ifBlank { null },
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, color = accentColor, fontSize = 11.sp)
                    }
                }
                if (isCurrentVoice) {
                    Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(0.2f)) {
                        Text("Voce attuale", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Description
            if (voice.description.isNotBlank()) {
                Text(
                    "\"${voice.description.take(100)}${if (voice.description.length > 100) "..." else ""}\"",
                    color = OnSurfaceVariant, fontSize = 12.sp,
                )
            }

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = if (isPlaying) onStop else onPlay,
                    modifier = Modifier.weight(1f).height(38.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) ErrorRed else accentColor),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) ErrorRed else accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Ferma" else "Ascolta",
                        color = if (isPlaying) ErrorRed else accentColor,
                        fontSize = 13.sp,
                    )
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier.height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isCurrentVoice) accentColor.copy(0.3f) else trackColor),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    if (isCurrentVoice) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(if (isCurrentVoice) "Selezionata" else "Usala", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accentColor.copy(alpha = 0.25f) else SurfaceVariant,
        modifier = Modifier
            .border(1.dp, if (selected) accentColor else SurfaceVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) accentColor else OnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun Int.toLocale(): String {
    return "%,d".format(this).replace(",", ".")
}
