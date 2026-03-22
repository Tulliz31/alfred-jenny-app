package com.alfredJenny.app.ui.screens.avatar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.ui.theme.*
import kotlinx.coroutines.delay

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * Avatar manager screen.
 * @param mode "alfred" or "jenny" — determines which tab opens by default.
 */
@Composable
fun AvatarManagerScreen(
    mode: String = "alfred",
    onBack: () -> Unit,
    viewModel: AvatarManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(if (mode == "jenny") 1 else 0) }

    // Feedback snackbar
    state.feedback?.let { msg ->
        LaunchedEffect(msg) { delay(2500); viewModel.dismissFeedback() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = OnBackground)
            }
            Text(
                "Gestione Avatar",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.weight(1f)
            )
        }

        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = AlfredBlueLight,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Alfred", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Jenny", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AlfredAvatarTab()
                1 -> JennyAvatarTab(state = state, viewModel = viewModel)
            }
        }

        // Feedback banner
        state.feedback?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = SuccessGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Alfred tab ────────────────────────────────────────────────────────────────

@Composable
private fun AlfredAvatarTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AlfredBlue.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = AlfredBlueLight, modifier = Modifier.size(32.dp))
                Column {
                    Text("Avatar geometrico animato", color = OnBackground,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Alfred usa un avatar vettoriale generato via Compose Animations.",
                        color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        Text("Come personalizzare Alfred", fontWeight = FontWeight.SemiBold,
            color = AlfredBlueLight, fontSize = 15.sp)

        AlfredInstructionCard(
            step = "1", title = "Prepara le immagini",
            body = "Crea file SVG o PNG di 200×200 px per ciascuno stato e frame.\n" +
                   "I file devono avere gli stessi nomi dei drawable correnti."
        )
        AlfredInstructionCard(
            step = "2", title = "Nomi file richiesti",
            body = "IDLE (4 frame): alfred_idle_000 … alfred_idle_003\n" +
                   "TALKING (4 frame): alfred_talking_000 … alfred_talking_003\n" +
                   "THINKING (3 frame): alfred_thinking_000 … alfred_thinking_002\n" +
                   "LISTENING (3 frame): alfred_listening_000 … alfred_listening_002"
        )
        AlfredInstructionCard(
            step = "3", title = "Copia nella cartella drawable",
            body = "Sostituisci i file in app/src/main/res/drawable/ e lancia un rebuild."
        )

        HorizontalDivider(color = SurfaceVariant)

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormatRow("Formato consigliato", "Android Vector Drawable (.xml)")
                FormatRow("Viewport", "200 × 200")
                FormatRow("Sfondo", "Trasparente o cerchio #1a3a5c")
            }
        }
    }
}

// ── Jenny tab ─────────────────────────────────────────────────────────────────

@Composable
private fun JennyAvatarTab(
    state: AvatarManagerUiState,
    viewModel: AvatarManagerViewModel,
) {
    // Keep track of which slot is waiting for the file picker
    var pendingFilename by remember { mutableStateOf<String?>(null) }
    var pendingCustomIndex by remember { mutableStateOf<Int?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingFilename?.let { viewModel.importJennyFile(it, uri) }
        pendingCustomIndex?.let { viewModel.importCustomOutfit(it, uri) }
        pendingFilename = null
        pendingCustomIndex = null
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = JennyPurpleLight)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Body (outfit) ─────────────────────────────────────────────────────
        JennySlotSection(
            title = "Body (Outfit)",
            slots = state.jennyBodySlots,
            onImport = { filename ->
                pendingFilename = filename
                pendingCustomIndex = null
                launcher.launch("image/*")
            },
            onRemove = viewModel::removeJennyFile,
        )

        HorizontalDivider(color = SurfaceVariant)

        // ── Eyes ──────────────────────────────────────────────────────────────
        JennySlotSection(
            title = "Occhi",
            slots = state.jennyEyeSlots,
            onImport = { filename ->
                pendingFilename = filename
                pendingCustomIndex = null
                launcher.launch("image/*")
            },
            onRemove = viewModel::removeJennyFile,
        )

        HorizontalDivider(color = SurfaceVariant)

        // ── Mouth ─────────────────────────────────────────────────────────────
        JennySlotSection(
            title = "Bocca",
            slots = state.jennyMouthSlots,
            onImport = { filename ->
                pendingFilename = filename
                pendingCustomIndex = null
                launcher.launch("image/*")
            },
            onRemove = viewModel::removeJennyFile,
        )

        HorizontalDivider(color = SurfaceVariant)

        // ── Custom outfits ────────────────────────────────────────────────────
        Text(
            "Outfit personalizzati (max 6)",
            fontWeight = FontWeight.SemiBold,
            color = JennyPurpleLight,
            fontSize = 15.sp
        )
        Text(
            "Gli outfit importati appariranno automaticamente nella barra outfit di Jenny.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

        state.customOutfits.forEach { slot ->
            CustomOutfitRow(
                slot = slot,
                onNameChange = { viewModel.updateCustomOutfitNameLocal(slot.index, it) },
                onNameSave = { viewModel.saveCustomOutfitName(slot.index, slot.name) },
                onImport = {
                    pendingFilename = null
                    pendingCustomIndex = slot.index
                    launcher.launch("image/*")
                },
                onRemove = { viewModel.removeCustomOutfit(slot.index) },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Jenny slot section ────────────────────────────────────────────────────────

@Composable
private fun JennySlotSection(
    title: String,
    slots: List<AvatarSlot>,
    onImport: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Text(title, fontWeight = FontWeight.SemiBold, color = JennyPurpleLight, fontSize = 15.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.forEach { slot ->
            JennySlotRow(slot = slot, onImport = { onImport(slot.filename) }, onRemove = { onRemove(slot.filename) })
        }
    }
}

@Composable
private fun JennySlotRow(
    slot: AvatarSlot,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail
            ThumbnailBox(bitmap = slot.thumbnail, hasContent = slot.hasFilesDir || slot.hasAssets)

            // Label + status
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.label, color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    text = when {
                        slot.hasFilesDir -> "✅ Personalizzato"
                        slot.hasAssets   -> "📦 Default (asset)"
                        else             -> "❌ Mancante"
                    },
                    color = when {
                        slot.hasFilesDir -> SuccessGreen
                        slot.hasAssets   -> AlfredBlueLight
                        else             -> ErrorRed
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Actions
            IconButton(onClick = onImport, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FileUpload, contentDescription = "Importa",
                    tint = JennyPurpleLight, modifier = Modifier.size(20.dp))
            }
            if (slot.hasFilesDir) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi",
                        tint = ErrorRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Custom outfit row ─────────────────────────────────────────────────────────

@Composable
private fun CustomOutfitRow(
    slot: CustomOutfitSlot,
    onNameChange: (String) -> Unit,
    onNameSave: () -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (slot.hasFilesDir) JennyPurple.copy(alpha = 0.1f) else SurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp,
                if (slot.hasFilesDir) JennyPurple else SurfaceVariant,
                RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThumbnailBox(bitmap = slot.thumbnail, hasContent = slot.hasFilesDir)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Outfit ${slot.index + 1}",
                        color = if (slot.hasFilesDir) JennyPurpleLight else OnSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        if (slot.hasFilesDir) "✅ PNG importato" else "⚠️ Nessun file",
                        color = if (slot.hasFilesDir) SuccessGreen else OnSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(onClick = onImport, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Importa",
                        tint = JennyPurpleLight, modifier = Modifier.size(20.dp))
                }
                if (slot.hasFilesDir) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Rimuovi",
                            tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (slot.hasFilesDir) {
                OutlinedTextField(
                    value = slot.name,
                    onValueChange = onNameChange,
                    label = { Text("Nome outfit") },
                    placeholder = { Text("Es. Gym, Lavoro…", color = OnSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onNameSave) {
                            Icon(Icons.Default.Save, contentDescription = "Salva",
                                tint = JennyPurpleLight, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JennyPurpleLight,
                        unfocusedBorderColor = OnSurfaceVariant,
                        focusedLabelColor = JennyPurpleLight,
                        cursorColor = JennyPurpleLight,
                        focusedTextColor = OnBackground,
                        unfocusedTextColor = OnBackground,
                    )
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun ThumbnailBox(bitmap: ImageBitmap?, hasContent: Boolean) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasContent) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = JennyPurpleLight,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.HideImage, contentDescription = null,
                tint = OnSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun AlfredInstructionCard(step: String, title: String, body: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = AlfredBlue, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(step, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = OnBackground, fontSize = 14.sp)
                Text(body, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FormatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f))
        Text(value, color = OnBackground, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}
