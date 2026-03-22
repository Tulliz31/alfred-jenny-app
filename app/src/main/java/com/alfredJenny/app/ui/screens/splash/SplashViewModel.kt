package com.alfredJenny.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alfredJenny.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Home : SplashDestination()
    object Login : SplashDestination()
    object Onboarding : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination

    init {
        viewModelScope.launch {
            delay(1500)
            val prefs = preferencesRepository.userPreferences.first()
            _destination.value = when {
                // Already logged in: skip everything
                prefs.jwtToken.isNotBlank()  -> SplashDestination.Home
                // Onboarding not finished yet (fresh install)
                !prefs.onboardingCompleted   -> SplashDestination.Onboarding
                // Logged out but backend is configured: go to login
                else                         -> SplashDestination.Login
            }
        }
    }
}
