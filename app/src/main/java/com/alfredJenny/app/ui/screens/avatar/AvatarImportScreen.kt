package com.alfredJenny.app.ui.screens.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfredJenny.app.ui.theme.*

@Composable
fun AvatarImportScreen(onBack: () -> Unit) {
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
                "Avatar personalizzato",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon + title
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = AlfredBlueLight,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sostituisci i frame dell'avatar",
                        fontWeight = FontWeight.Bold,
                        color = OnBackground,
                        fontSize = 18.sp
                    )
                }
            }

            HorizontalDivider(color = SurfaceVariant)

            // Instructions
            Text(
                "Come personalizzare Alfred",
                fontWeight = FontWeight.SemiBold,
                color = AlfredBlueLight,
                fontSize = 15.sp
            )

            InstructionCard(
                step = "1",
                title = "Prepara le immagini",
                body = "Crea file SVG o PNG di 200×200 px per ciascuno stato e frame. " +
                       "I file devono avere esattamente gli stessi nomi dei placeholder."
            )

            InstructionCard(
                step = "2",
                title = "Nomi file richiesti",
                body = "IDLE (4 frame):\nalfred_idle_000 … alfred_idle_003\n\n" +
                       "TALKING (4 frame):\nalfred_talking_000 … alfred_talking_003\n\n" +
                       "THINKING (3 frame):\nalfred_thinking_000 … alfred_thinking_002\n\n" +
                       "LISTENING (3 frame):\nalfred_listening_000 … alfred_listening_002"
            )

            InstructionCard(
                step = "3",
                title = "Copia nella cartella drawable",
                body = "Sostituisci i file nella cartella:\n" +
                       "app/src/main/res/drawable/\n\n" +
                       "I file Vector Drawable (.xml) vengono compilati nell'APK al momento del build."
            )

            InstructionCard(
                step = "4",
                title = "Rebuild e test",
                body = "Esegui un nuovo build del progetto in Android Studio " +
                       "(Build → Rebuild Project) per vedere le modifiche in app."
            )

            HorizontalDivider(color = SurfaceVariant)

            Text(
                "Formato consigliato",
                fontWeight = FontWeight.SemiBold,
                color = AlfredBlueLight,
                fontSize = 15.sp
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormatRow("Formato", "Android Vector Drawable (.xml)")
                    FormatRow("Viewport", "200 × 200")
                    FormatRow("Profondità colore", "32-bit RGBA")
                    FormatRow("Sfondo", "Trasparente o cerchio #1a3a5c")
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue)
            ) {
                Text("Torna alle impostazioni", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InstructionCard(step: String, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AlfredBlue,
                modifier = Modifier.size(32.dp)
            ) {
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
        Text(label, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, color = OnBackground, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
