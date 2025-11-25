package com.example.sprint_2_kotlin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sprint_2_kotlin.ui.theme.Sprint2KotlinTheme
import com.example.sprint_2_kotlin.view.AuthScreen
import com.example.sprint_2_kotlin.view.GuideScreen
import com.example.sprint_2_kotlin.view.NewsFeedScreen
import com.example.sprint_2_kotlin.view.NewsItemDetailScreen
import com.example.sprint_2_kotlin.view.ProfileScreen
import com.example.sprint_2_kotlin.view.ReadHistoryScreen  // ✅ AGREGADO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.sprint_2_kotlin.view.ConnectivityBanner
import com.example.sprint_2_kotlin.viewmodel.HomeViewModel
import com.example.sprint_2_kotlin.model.data.ThemePreferences
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val connectivityViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // DARK MODE STATE - Lee la preferencia guardada
            val isDarkMode by ThemePreferences.isDarkMode(this).collectAsState(initial = false)
            val coroutineScope = rememberCoroutineScope()

            Sprint2KotlinTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val isConnected by connectivityViewModel.isConnected.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Column(modifier = Modifier.fillMaxSize()) {

                        //  CONNECTIVITY BANNER
                        ConnectivityBanner(isConnected = isConnected)

                        NavHost(
                            navController = navController,
                            startDestination = Screen.Auth.route,
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(400)
                                ) + fadeIn(animationSpec = tween(400))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(400)
                                ) + fadeOut(animationSpec = tween(400))
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(400)
                                ) + fadeIn(animationSpec = tween(400))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(400)
                                ) + fadeOut(animationSpec = tween(400))
                            }
                        ) {
                            composable(
                                route = Screen.Auth.route,
                                enterTransition = {
                                    fadeIn(animationSpec = tween(600))
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(300)) +
                                            scaleOut(targetScale = 0.95f, animationSpec = tween(300))
                                }
                            ) {
                                AuthScreen(
                                    onLoginSuccess = {
                                        navController.navigate(Screen.NewsFeed.route) {
                                            popUpTo(Screen.Auth.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(
                                route = Screen.NewsFeed.route
                            ) {
                                NewsFeedScreen(
                                    isDarkMode = isDarkMode,
                                    onNewsItemClick = { newsItemId ->
                                        navController.navigate(
                                            Screen.NewsItemDetail.createRoute(newsItemId)
                                        )
                                    },
                                    onNavigateToGuide = {
                                        navController.navigate(Screen.Guide.route)
                                    },
                                    onNavigateToProfile = {
                                        navController.navigate(Screen.Profile.route)
                                    }
                                )
                            }

                            composable(
                                route = Screen.NewsItemDetail.route,
                                arguments = listOf(navArgument("newsItemId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val newsItemId = backStackEntry.arguments?.getInt("newsItemId") ?: 0
                                val userProfileId = backStackEntry.arguments?.getInt("userProfileId") ?: 0

                                NewsItemDetailScreen(
                                    isDarkMode = isDarkMode,
                                    newsItemId = newsItemId,
                                    userProfileId = userProfileId,
                                    onBackClick = {
                                        navController.navigate(Screen.NewsFeed.route) {
                                            popUpTo(Screen.NewsFeed.route) { inclusive = true } }
                                    }
                                )
                            }

                            composable(route = Screen.Guide.route) {
                                GuideScreen(
                                    isDarkMode = isDarkMode,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(route = Screen.Profile.route) {
                                ProfileScreen(
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = { newValue ->
                                        coroutineScope.launch {
                                            ThemePreferences.setDarkMode(this@MainActivity, newValue)
                                        }
                                    },
                                    onLogout = {
                                        navController.navigate(Screen.Auth.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateToHome = {
                                        navController.navigate(Screen.NewsFeed.route) {
                                            popUpTo(Screen.NewsFeed.route) { inclusive = true }
                                        }
                                    },
                                    onNavigateToGuide = {
                                        navController.navigate(Screen.Guide.route)
                                    },
                                    onNavigateToReadHistory = {  //  update!!
                                        navController.navigate(Screen.ReadHistory.route)
                                    }
                                )
                            }

                            // update!! : Ruta para Read History Screen
                            composable(route = Screen.ReadHistory.route) {
                                ReadHistoryScreen(
                                    isDarkMode = isDarkMode,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onNewsItemClick = { newsItemId ->
                                        navController.navigate(
                                            Screen.NewsItemDetail.createRoute(newsItemId)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricManager = BiometricManager.from(this)

        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onFailure()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock with Fingerprint")
            .setSubtitle("Authenticate to access your News Feed")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}