package com.alfredJenny.app.ui.screens.main

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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.ui.screens.home.HomeScreen
import com.alfredJenny.app.ui.screens.jenny.JennyScreen
import com.alfredJenny.app.ui.screens.notes.NotesScreen
import com.alfredJenny.app.ui.screens.smarthome.SmartHomeScreen
import com.alfredJenny.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private enum class MainTab { HOME, JENNY, NOTES, SMART_HOME, SETTINGS }

data class MainUiState(val jennyEnabled: Boolean = false, val smartHomeEnabled: Boolean = false, val notesEnabled: Boolean = true)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val jennyEnabled: StateFlow<Boolean> = preferencesRepository.userPreferences
        .map { it.jennyEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
    val jennyEnabled by viewModel.jennyEnabled.collectAsStateWithLifecycle()
    val smartHomeEnabled by viewModel.smartHomeEnabled.collectAsStateWithLifecycle()
    val notesEnabled by viewModel.notesEnabled.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    // If a feature gets disabled, go back to home
    LaunchedEffect(jennyEnabled) {
        if (!jennyEnabled && selectedTab == MainTab.JENNY) selectedTab = MainTab.HOME
    }
    LaunchedEffect(smartHomeEnabled) {
        if (!smartHomeEnabled && selectedTab == MainTab.SMART_HOME) selectedTab = MainTab.HOME
    }
    LaunchedEffect(notesEnabled) {
        if (!notesEnabled && selectedTab == MainTab.NOTES) selectedTab = MainTab.HOME
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            val showBar = jennyEnabled || smartHomeEnabled || notesEnabled
            if (showBar) {
                NavigationBar(containerColor = Surface, contentColor = OnBackground) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Alfred") },
                        label = { Text("Alfred") },
                        selected = selectedTab == MainTab.HOME,
                        onClick = { selectedTab = MainTab.HOME },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AlfredBlueLight,
                            selectedTextColor = AlfredBlueLight,
                            indicatorColor = AlfredBlue.copy(alpha = 0.2f),
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant
                        )
                    )
                    if (jennyEnabled) {
                        NavigationBarItem(
                            icon = {
                                Text("\uD83D\uDC9C", style = MaterialTheme.typography.titleMedium)
                            },
                            label = { Text("Jenny") },
                            selected = selectedTab == MainTab.JENNY,
                            onClick = { selectedTab = MainTab.JENNY },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = JennyPurpleLight,
                                selectedTextColor = JennyPurpleLight,
                                indicatorColor = JennyPurple.copy(alpha = 0.2f),
                                unselectedIconColor = OnSurfaceVariant,
                                unselectedTextColor = OnSurfaceVariant
                            )
                        )
                    }
                    if (notesEnabled) {
                        NavigationBarItem(
                            icon = {
                                Text("📝", style = MaterialTheme.typography.titleMedium)
                            },
                            label = { Text("Note") },
                            selected = selectedTab == MainTab.NOTES,
                            onClick = { selectedTab = MainTab.NOTES },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF66BB6A),
                                selectedTextColor = Color(0xFF66BB6A),
                                indicatorColor = Color(0xFF66BB6A).copy(alpha = 0.2f),
                                unselectedIconColor = OnSurfaceVariant,
                                unselectedTextColor = OnSurfaceVariant
                            )
                        )
                    }
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
                                unselectedTextColor = OnSurfaceVariant
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
                            unselectedTextColor = OnSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                MainTab.HOME       -> HomeScreen(onOpenSettings = onOpenSettings)
                MainTab.JENNY      -> JennyScreen()
                MainTab.NOTES      -> NotesScreen()
                MainTab.SMART_HOME -> SmartHomeScreen(onOpenSettings = onOpenSettings)
                MainTab.SETTINGS   -> { /* handled via onOpenSettings */ }
            }
        }
    }
}
