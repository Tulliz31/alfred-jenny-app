package com.alfredJenny.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.alfredJenny.app.ui.theme.*

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = AlfredBlueLight) },
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, color = OnSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = { onDismiss(); onOpenSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue)
            ) { Text("Apri Impostazioni") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
        containerColor = Surface,
    )
}
