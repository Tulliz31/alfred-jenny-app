package com.alfredJenny.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alfredJenny.app.ui.screens.home.HomeScreen
import com.alfredJenny.app.ui.screens.jenny.JennyScreen
import com.alfredJenny.app.ui.screens.login.LoginScreen
import com.alfredJenny.app.ui.screens.login.LoginViewModel
import com.alfredJenny.app.ui.screens.settings.SettingsScreen

object Routes {
    const val LOGIN    = "login"
    const val HOME     = "home"
    const val SETTINGS = "settings"
    const val JENNY    = "jenny"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Use the LoginViewModel to detect a saved session and skip the login screen.
    val loginVm: LoginViewModel = hiltViewModel()
    val loginState by loginVm.uiState.collectAsStateWithLifecycle()

    // Auto-navigate to Home if a JWT is already stored.
    LaunchedEffect(loginState.autoLogin) {
        if (loginState.autoLogin) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                viewModel = loginVm
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.JENNY) {
            JennyScreen()
        }
    }
}
