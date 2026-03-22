package com.alfredJenny.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alfredJenny.app.data.repository.AuthRepository
import com.alfredJenny.app.ui.screens.avatar.AvatarManagerScreen
import com.alfredJenny.app.ui.screens.jenny.JennyAIScreen
import com.alfredJenny.app.ui.screens.settings.AIProviderConfigScreen
import com.alfredJenny.app.ui.screens.settings.VoiceBrowserScreen
import com.alfredJenny.app.ui.screens.login.LoginScreen
import com.alfredJenny.app.ui.screens.login.LoginViewModel
import com.alfredJenny.app.ui.screens.main.MainScreen
import com.alfredJenny.app.ui.screens.onboarding.OnboardingScreen
import com.alfredJenny.app.ui.screens.settings.SettingsScreen
import com.alfredJenny.app.ui.screens.splash.SplashDestination
import com.alfredJenny.app.ui.screens.splash.SplashScreen
import com.alfredJenny.app.ui.screens.splash.SplashViewModel

object Routes {
    const val SPLASH              = "splash"
    const val ONBOARDING          = "onboarding"
    const val LOGIN               = "login"
    const val HOME                = "home"
    const val SETTINGS            = "settings"
    const val JENNY               = "jenny"
    const val AVATAR_IMPORT       = "avatar_manager?mode=alfred"  // legacy alias
    const val AVATAR_ALFRED       = "avatar_manager?mode=alfred"
    const val AVATAR_JENNY        = "avatar_manager?mode=jenny"
    const val JENNY_AI            = "jenny_ai"
    const val AI_PROVIDER_CONFIG  = "ai_provider/{companionId}"
    const val VOICE_BROWSER       = "voice_browser/{companionId}"
}

// ── Transition presets ────────────────────────────────────────────────────────

private val enterSlide  get() = slideInHorizontally(tween(280)) { it / 5 } + fadeIn(tween(280))
private val exitSlide   get() = slideOutHorizontally(tween(280)) { -it / 5 } + fadeOut(tween(280))
private val popEnter    get() = slideInHorizontally(tween(280)) { -it / 5 } + fadeIn(tween(280))
private val popExit     get() = slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(280))

@Composable
fun AppNavigation(
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val loginVm: LoginViewModel = hiltViewModel()

    fun navigateToHome() {
        navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
    }

    fun navigateToLogin() {
        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {

        // ── Splash ───────────────────────────────────────────────────────────
        composable(
            route = Routes.SPLASH,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition  = { fadeOut(tween(400)) },
        ) {
            val splashVm: SplashViewModel = hiltViewModel()
            SplashScreen(
                onNavigate = { dest ->
                    when (dest) {
                        SplashDestination.Home       -> navigateToHome()
                        SplashDestination.Login      -> navigateToLogin()
                        SplashDestination.Onboarding ->
                            navController.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
                    }
                },
                viewModel = splashVm,
            )
        }

        // ── Onboarding ───────────────────────────────────────────────────────
        composable(
            route = Routes.ONBOARDING,
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
        ) {
            OnboardingScreen(onComplete = { navigateToHome() })
        }

        // ── Login ────────────────────────────────────────────────────────────
        composable(
            route = Routes.LOGIN,
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
        ) {
            LoginScreen(
                onLoginSuccess = { navigateToHome() },
                viewModel = loginVm,
            )
        }

        // ── Home (main tabs) ──────────────────────────────────────────────────
        composable(
            route = Routes.HOME,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition  = { fadeOut(tween(200)) },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit },
        ) {
            MainScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(
            route = Routes.SETTINGS,
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit },
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAvatarImport   = { navController.navigate(Routes.AVATAR_ALFRED) },
                onOpenJennyAvatar    = { navController.navigate(Routes.AVATAR_JENNY) },
                onOpenJennyAI        = { navController.navigate("ai_provider/jenny") },
                onOpenAlfredAI       = { navController.navigate("ai_provider/alfred") },
                onOpenAlfredVoice    = { navController.navigate("voice_browser/alfred") },
                onOpenJennyVoice     = { navController.navigate("voice_browser/jenny") },
                onLogout = { navigateToLogin() }
            )
        }

        // ── Jenny AI config (legacy) ──────────────────────────────────────────
        composable(
            route = Routes.JENNY_AI,
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit },
        ) {
            JennyAIScreen(onBack = { navController.popBackStack() })
        }

        // ── AI Provider config (alfred / jenny) ───────────────────────────────
        composable(
            route = Routes.AI_PROVIDER_CONFIG,
            arguments = listOf(navArgument("companionId") {
                type = NavType.StringType
                defaultValue = "alfred"
            }),
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit },
        ) {
            AIProviderConfigScreen(onBack = { navController.popBackStack() })
        }

        // ── Voice browser ────────────────────────────────────────────────────
        composable(
            route = Routes.VOICE_BROWSER,
            arguments = listOf(navArgument("companionId") {
                type = NavType.StringType
                defaultValue = "alfred"
            }),
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit },
        ) {
            VoiceBrowserScreen(onBack = { navController.popBackStack() })
        }

        // ── Avatar manager ────────────────────────────────────────────────────
        composable(
            route = "avatar_manager?mode={mode}",
            arguments = listOf(navArgument("mode") {
                type = NavType.StringType
                defaultValue = "alfred"
            }),
            enterTransition = { enterSlide },
            exitTransition  = { exitSlide },
            popEnterTransition = { popEnter },
            popExitTransition  = { popExit },
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "alfred"
            AvatarManagerScreen(mode = mode, onBack = { navController.popBackStack() })
        }
    }
}
