package com.alfredJenny.app.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.ui.companion.CompanionManager
import com.alfredJenny.app.ui.screens.home.HomeScreen
import com.alfredJenny.app.ui.screens.jenny.JennyScreen
import com.alfredJenny.app.ui.screens.notes.NotesScreen
import com.alfredJenny.app.ui.screens.smarthome.SmartHomeScreen
import com.alfredJenny.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class MainTab { HOME, NOTES, SMART_HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val userRole: StateFlow<String> = preferencesRepository.userPreferences
        .map { it.userRole }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val smartHomeEnabled: StateFlow<Boolean> = preferencesRepository.userPreferences
        .map { it.smartHomeEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notesEnabled: StateFlow<Boolean> = preferencesRepository.userPreferences
        .map { it.notesEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val smartHomeEnabled by viewModel.smartHomeEnabled.collectAsStateWithLifecycle()
    val notesEnabled by viewModel.notesEnabled.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    val companion = CompanionManager.getActiveCompanion(userRole)
    val isAdmin = userRole == "admin"

    // Welcome splash — shown once per session
    var showWelcome by remember { mutableStateOf(userRole.isNotBlank()) }
    LaunchedEffect(userRole) {
        if (userRole.isNotBlank()) {
            showWelcome = true
            delay(3000)
            showWelcome = false
        }
    }

    // If a feature gets disabled while on that tab, go back to home
    LaunchedEffect(smartHomeEnabled) {
        if (!smartHomeEnabled && selectedTab == MainTab.SMART_HOME) selectedTab = MainTab.HOME
    }
    LaunchedEffect(notesEnabled) {
        if (!notesEnabled && selectedTab == MainTab.NOTES) selectedTab = MainTab.HOME
    }
    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface, contentColor = OnBackground) {
                // Home tab — always visible
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = companion.name) },
                    label = { Text(companion.name) },
                    selected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isAdmin) JennyPurpleLight else AlfredBlueLight,
                        selectedTextColor = if (isAdmin) JennyPurpleLight else AlfredBlueLight,
                        indicatorColor = (if (isAdmin) JennyPurple else AlfredBlue).copy(alpha = 0.2f),
                        unselectedIconColor = OnSurfaceVariant,
                        unselectedTextColor = OnSurfaceVariant,
                    )
                )
                if (notesEnabled) {
                    NavigationBarItem(
                        icon = { Text("📝", style = MaterialTheme.typography.titleMedium) },
                        label = { Text("Note") },
                        selected = selectedTab == MainTab.NOTES,
                        onClick = { selectedTab = MainTab.NOTES },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF66BB6A),
                            selectedTextColor = Color(0xFF66BB6A),
                            indicatorColor = Color(0xFF66BB6A).copy(alpha = 0.2f),
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant,
                        )
                    )
                }
                // Smart Home tab — visible to all when enabled
                if (smartHomeEnabled) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Lightbulb, contentDescription = "Smart Home") },
                        label = { Text("Casa") },
                        selected = selectedTab == MainTab.SMART_HOME,
                        onClick = { selectedTab = MainTab.SMART_HOME },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFA726),
                            selectedTextColor = Color(0xFFFFA726),
                            indicatorColor = Color(0xFFFFA726).copy(alpha = 0.2f),
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant,
                        )
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { onOpenSettings() },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = OnSurfaceVariant,
                        unselectedTextColor = OnSurfaceVariant,
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(200),
                label = "mainTab",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                    MainTab.HOME -> {
                        if (isAdmin) JennyScreen(onOpenSettings = onOpenSettings)
                        else HomeScreen(onOpenSettings = onOpenSettings)
                    }
                    MainTab.NOTES      -> NotesScreen()
                    MainTab.SMART_HOME -> SmartHomeScreen(onOpenSettings = onOpenSettings, isAdmin = isAdmin)
                }
            }

            // Welcome splash
            AnimatedVisibility(
                visible = showWelcome,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = companion.primaryColor.copy(alpha = 0.92f),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = companion.welcomeMessage,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
