package com.alfredJenny.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.data.repository.PreferencesRepository
import com.alfredJenny.app.ui.navigation.AppNavigation
import com.alfredJenny.app.ui.theme.AlfredJennyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by preferencesRepository.userPreferences.collectAsState(initial = null)
            AlfredJennyTheme(useLightTheme = prefs?.lightTheme ?: false) {
                AppNavigation(authRepository = authRepository)
            }
        }
    }
}
