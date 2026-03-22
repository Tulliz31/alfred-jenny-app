package com.alfredJenny.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfredJenny.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Step dots indicator
            StepIndicator(currentStep = state.step, totalSteps = 3)

            // Animated step content
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
                },
                label = "onboarding_step",
            ) { step ->
                when (step) {
                    0 -> UrlStep(state = state, viewModel = viewModel)
                    1 -> LoginStep(state = state, viewModel = viewModel, onComplete = onComplete)
                    2 -> ApiKeyStep(state = state, viewModel = viewModel, onComplete = onComplete)
                }
            }
        }
    }
}

// ── Step indicators ───────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == currentStep) 28.dp else 10.dp, 10.dp)
                    .clip(CircleShape)
                    .background(if (i == currentStep) AlfredBlueLight else OnSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    }
}

// ── Step 0: Backend URL ───────────────────────────────────────────────────────

@Composable
private fun UrlStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.Cloud, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(48.dp))
        Text("Configura il backend", fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 22.sp)
        Text(
            "Inserisci l'URL del tuo server AlfredJenny (es. su Railway o locale).",
            color = OnSurfaceVariant, textAlign = TextAlign.Center, fontSize = 14.sp
        )

        OutlinedTextField(
            value = state.backendUrl,
            onValueChange = viewModel::onUrlChange,
            label = { Text("URL server") },
            placeholder = { Text("https://your-app.railway.app", color = OnSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            trailingIcon = {
                if (state.urlOk) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
            },
            isError = state.urlError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.testConnection() }),
            modifier = Modifier.fillMaxWidth(),
            colors = onboardingFieldColors(),
        )

        if (state.urlError != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Text(state.urlError!!, color = ErrorRed, fontSize = 12.sp)
            }
        }

        if (state.urlOk) {
            Text("✓ Connessione riuscita!", color = SuccessGreen, fontWeight = FontWeight.Medium)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !state.isTesting && state.backendUrl.isNotBlank(),
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlfredBlueLight),
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AlfredBlueLight)
                } else {
                    Text("Test", color = AlfredBlueLight)
                }
            }
            Button(
                onClick = viewModel::goToLogin,
                enabled = state.urlOk,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue),
            ) {
                Text("Avanti →")
            }
        }
    }
}

// ── Step 1: Login ─────────────────────────────────────────────────────────────

@Composable
private fun LoginStep(state: OnboardingUiState, viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val focusManager = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(48.dp))
        Text("Accedi", fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 22.sp)
        Text(
            "Inserisci le credenziali configurate nel file .env del backend.",
            color = OnSurfaceVariant, textAlign = TextAlign.Center, fontSize = 14.sp
        )

        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            colors = onboardingFieldColors(),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = OnSurfaceVariant
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            isError = state.loginError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login(onComplete) }),
            modifier = Modifier.fillMaxWidth(),
            colors = onboardingFieldColors(),
        )

        if (state.loginError != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Text(state.loginError!!, color = ErrorRed, fontSize = 12.sp)
            }
        }

        Button(
            onClick = { viewModel.login(onComplete) },
            enabled = !state.isLoggingIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue),
        ) {
            if (state.isLoggingIn) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = OnBackground)
            } else {
                Text("Accedi →", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Step 2: API key ───────────────────────────────────────────────────────────

@Composable
private fun ApiKeyStep(state: OnboardingUiState, viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.Key, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(48.dp))
        Text("Chiave API AI", fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 22.sp)
        Text(
            "Facoltativo: inserisci la tua chiave OpenAI / Anthropic / Gemini. Puoi farlo in seguito dalle Impostazioni.",
            color = OnSurfaceVariant, textAlign = TextAlign.Center, fontSize = 14.sp
        )

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = viewModel::onApiKeyChange,
            label = { Text("API Key (opzionale)") },
            placeholder = { Text("sk-...", color = OnSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = onboardingFieldColors(),
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AlfredBlue.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AlfredBlueLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Se il backend usa provider AI configurati server-side (consigliato), puoi saltare questo passaggio.",
                    color = AlfredBlueLight, fontSize = 12.sp
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = { viewModel.skipApiKey(onComplete) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Salta", color = OnSurfaceVariant)
            }
            Button(
                onClick = { viewModel.finish(onComplete) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlfredBlue),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = OnBackground)
                } else {
                    Text("Inizia →", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Shared field colors ───────────────────────────────────────────────────────

@Composable
private fun onboardingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AlfredBlueLight,
    unfocusedBorderColor = OnSurfaceVariant,
    focusedLabelColor = AlfredBlueLight,
    cursorColor = AlfredBlueLight,
    focusedTextColor = OnBackground,
    unfocusedTextColor = OnBackground,
    errorBorderColor = ErrorRed,
    errorLabelColor = ErrorRed,
)
