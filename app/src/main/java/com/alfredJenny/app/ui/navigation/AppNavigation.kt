package com.alfredJenny.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.ui.screens.avatar.AvatarImportScreen
import com.alfredJenny.app.ui.screens.login.LoginScreen
import com.alfredJenny.app.ui.screens.login.LoginViewModel
import com.alfredJenny.app.ui.screens.main.MainScreen
import com.alfredJenny.app.ui.screens.settings.SettingsScreen

object Routes {
    const val LOGIN         = "login"
    const val HOME          = "home"
    const val SETTINGS      = "settings"
    const val JENNY         = "jenny"
    const val AVATAR_IMPORT = "avatar_import"
}

@Composable
fun AppNavigation(
    authRepository: AuthRepository
) {
    val navController = rememberNavController()

    val loginVm: LoginViewModel = hiltViewModel()
    val loginState by loginVm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(loginState.autoLogin) {
        if (loginState.autoLogin) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    fun handleLogout() {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
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
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAvatarImport = { navController.navigate(Routes.AVATAR_IMPORT) },
                onLogout = {
                    handleLogout()
                }
            )
        }
        composable(Routes.AVATAR_IMPORT) {
            AvatarImportScreen(onBack = { navController.popBackStack() })
        }
    }
}
