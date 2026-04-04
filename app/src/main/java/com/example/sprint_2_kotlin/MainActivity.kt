package com.example.sprint_2_kotlin

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sprint_2_kotlin.ui.theme.Sprint2KotlinTheme
import com.example.sprint_2_kotlin.view.*
import com.example.sprint_2_kotlin.viewmodel.HomeViewModel
import com.example.sprint_2_kotlin.viewmodel.BookmarkViewModel
import com.example.sprint_2_kotlin.model.data.ThemePreferences
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatDelegate


import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sprint_2_kotlin.viewmodel.AuthViewModel
import com.example.sprint_2_kotlin.viewmodel.NewsFeedViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest


class MainActivity : AppCompatActivity() {

    private val connectivityViewModel: HomeViewModel by viewModels()

    // BookmarkViewModel
    private val bookmarkViewModel: BookmarkViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[BookmarkViewModel::class.java]
    }

    private val prefs by lazy {
        applicationContext.getSharedPreferences("my_preferences", MODE_PRIVATE)
    }

    val newsFeedViewModel: NewsFeedViewModel by viewModels()
    val authViewModel: AuthViewModel by viewModels()




    private val client = createSupabaseClient(
        supabaseUrl = "https://fyotaxqfpgbkyefapzya.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ5b3RheHFmcGdia3llZmFwenlhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE4MDMxNDIsImV4cCI6MjA4NzM3OTE0Mn0.Hvb--I3VLCkkhXAbMUaUC-O2SKbV9JyyUjFc3dmmxjU"
    ) {
        install(Postgrest)
        install(Auth)
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        val savedLocales = AppCompatDelegate.getApplicationLocales()
        AppCompatDelegate.setApplicationLocales(savedLocales)

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
                            startDestination = "auth",
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
                                route = "auth",
                                enterTransition = {
                                    fadeIn(animationSpec = tween(600))
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(300)) +
                                            scaleOut(targetScale = 0.95f, animationSpec = tween(300))
                                }
                            ) {
                                AuthScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = {
                                        navController.navigate("news_feed") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    },
                                    onForgotPassword = {
                                        navController.navigate("forgot_password")
                                    }
                                )
                            }

                            composable(route = "news_feed") {
                                NewsFeedScreen(
                                    viewModel = newsFeedViewModel,
                                    preferences = prefs,
                                    isDarkMode = isDarkMode,
                                    onNewsItemClick = { newsItemId ->
                                        navController.navigate("news_item_detail/$newsItemId")
                                    },
                                    onNavigateToGuide = {
                                        navController.navigate("guide")
                                    },
                                    onNavigateToProfile = {
                                        navController.navigate("profile")
                                    }
                                )
                            }

                            composable(
                                route = "news_item_detail/{newsItemId}",
                                arguments = listOf(navArgument("newsItemId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val newsItemId = backStackEntry.arguments?.getInt("newsItemId") ?: 0
                                val userProfileId = backStackEntry.arguments?.getInt("userProfileId") ?: 0

                                NewsItemDetailScreen(
                                    isDarkMode = isDarkMode,
                                    newsItemId = newsItemId,
                                    userProfileId = userProfileId,
                                    bookmarkViewModel = bookmarkViewModel,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onShareClick = { url, title ->
                                        // Navigate to QR share screen with URL and title
                                        navController.navigate("qr_share/${java.net.URLEncoder.encode(url, "UTF-8")}/${java.net.URLEncoder.encode(title, "UTF-8")}")
                                    }
                                )
                            }

                            composable(route = "guide") {
                                GuideScreen(
                                    isDarkMode = isDarkMode,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(route = "profile") {
                                ProfileScreen(
                                    authViewModel = authViewModel,
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = { newValue ->
                                        coroutineScope.launch {
                                            ThemePreferences.setDarkMode(this@MainActivity, newValue)
                                        }
                                    },
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateToHome = {
                                        navController.navigate("news_feed") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateToGuide = {
                                        navController.navigate("guide")
                                    },
                                    onNavigateToReadHistory = {
                                        navController.navigate("read_history")
                                    },
                                    onNavigateToNewsDetail = { newsItemId ->
                                        navController.navigate("news_item_detail/$newsItemId")
                                    },
                                    onNavigateToBookmarks = {
                                        navController.navigate("bookmarks")
                                    },
                                    bookmarkViewModel = bookmarkViewModel
                                )
                            }

                            // Ruta para Read History Screen
                            composable(route = "read_history") {
                                ReadHistoryScreen(
                                    isDarkMode = isDarkMode,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onNewsItemClick = { newsItemId ->
                                        navController.navigate("news_item_detail/$newsItemId")
                                    }
                                )
                            }

                            // Ruta para Bookmarks Screen
                            composable(route = "bookmarks") {
                                BookmarksScreen(
                                    preferences = prefs,
                                    isDarkMode = isDarkMode,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onNewsItemClick = { newsItemId ->
                                        navController.navigate("news_item_detail/$newsItemId")
                                    },
                                    viewModel = bookmarkViewModel
                                )
                            }

                            // Inside NavHost in MainActivity.kt
                            composable(route = "forgot_password") {
                                ForgotPasswordView( // Pass the dark mode state
                                    onBackToLogin = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(
                                route = "qr_share/{url}/{title}",
                                arguments = listOf(
                                    navArgument("url") { type = NavType.StringType },
                                    navArgument("title") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""

                                val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
                                val title = java.net.URLDecoder.decode(encodedTitle, "UTF-8")

                                QRShareScreen(
                                    isDarkMode = isDarkMode,
                                    newsItemUrl = url,
                                    newsItemTitle = title,
                                    onBackClick = {
                                        navController.popBackStack()
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