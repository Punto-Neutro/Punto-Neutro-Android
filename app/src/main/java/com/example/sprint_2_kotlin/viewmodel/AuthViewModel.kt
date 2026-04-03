package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.auth.SessionManager
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.model.data.Country
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AuthViewModel - Handles authentication logic with persistent session
 *
 * Now includes SessionManager to persist login credentials and auto-login
 */
class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Database DAOs
    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val daonews = AppDatabase.getDatabase(application).newsItemDao()

    private val daopqrs = AppDatabase.getDatabase(application).PQRSDao()
    private val daopqrstypes = AppDatabase.getDatabase(application).PQRS_typesDao()

    private val readHistoryDao = AppDatabase.getDatabase(application).readHistoryDao()



    // Repository for authentication
    private val repository = Repository(application.applicationContext, dao, daonews, daopqrs, daopqrstypes)

    // Session Manager for persistent login
    private val sessionManager = SessionManager(application.applicationContext)

    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries

    init {
        // Check if user is already logged in when ViewModel is created
        checkExistingSession()
        loadCountries(true)
    }

    /**
     * Check if there's an existing valid session and auto-login
     */
    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                val isValid = sessionManager.isSessionValid()
                if (isValid) {
                    // Get stored credentials
                    val credentials = sessionManager.getStoredCredentials()
                    if (credentials != null) {
                        val (email, password) = credentials

                        // Attempt auto-login with stored credentials
                        _uiState.value = _uiState.value.copy(
                            email = email,
                            password = password,
                            isCheckingSession = true
                        )

                        val loginSuccess = repository.signIn(email, password)

                        if (loginSuccess) {
                            println("DEBUG: Auto-login successful for $email")
                            _uiState.value = _uiState.value.copy(
                                isSuccess = true,
                                isCheckingSession = false
                            )
                        } else {
                            // Credentials invalid, clear session
                            println("DEBUG: Auto-login failed, clearing session")
                            sessionManager.clearSession()
                            _uiState.value = _uiState.value.copy(
                                email = "",
                                password = "",
                                isCheckingSession = false
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(isCheckingSession = false)
                    }
                } else {
                    // Session expired or doesn't exist
                    sessionManager.clearSession()
                    _uiState.value = _uiState.value.copy(isCheckingSession = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("DEBUG: Error checking session: ${e.message}")
                _uiState.value = _uiState.value.copy(isCheckingSession = false)
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value,errorMessage = "")
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value,errorMessage = "")
    }

    // Add this function inside AuthViewModel
    fun onCountryChange(value: Int) {
        _uiState.value = _uiState.value.copy(country = value,errorMessage = "")
    }

    // In AuthViewModel.kt

    fun register(countryId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")

            // Save the country in state so we have it ready for the first login
            _uiState.value = _uiState.value.copy(country = countryId)

            // Call the simplified signup (Auth only)
            val success = repository.signUp(_uiState.value.email, _uiState.value.password)

            if (success) {
                Log.d(TAG, "Auth user created. Verification email sent.")
                // We set isSuccess to false because they are NOT logged in yet
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,

                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false)
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")

            readHistoryDao.deleteAllHistory()

            // Use the new sync function
            val success = repository.signInAndSyncProfile(
                email = _uiState.value.email,
                password = _uiState.value.password,
                countryId = _uiState.value.country // Uses the country selected during reg
            )

            if (success) {
                val userId = 1 // Replace with actual logic if needed
                sessionManager.saveSession(
                    email = _uiState.value.email,
                    password = _uiState.value.password,
                    userId = userId
                )
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = "Invalid credentials")
                // Optional: Set an error message if email isn't verified
            }
        }
    }
    fun loginWithBiometric() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Get stored credentials
            val credentials = sessionManager.getStoredCredentials()

            if (credentials != null) {
                val (email, password) = credentials

                // Attempt login with stored credentials
                val success = repository.signIn(email, password)

                if (success) {
                    println("DEBUG: Biometric login successful for $email")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        email = email,
                        password = password
                    )
                } else {
                    // Credentials no longer valid, clear session
                    println("DEBUG: Biometric login failed, credentials invalid")
                    sessionManager.clearSession()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false
                    )
                }
            } else {
                // No stored credentials
                println("DEBUG: No stored credentials for biometric login")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false
                )
            }
        }
    }

    /**
     * Logout user and clear session
     */
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            println("DEBUG: User logged out, session cleared")
            _uiState.value = AuthUiState() // Reset to initial state
        }
    }

    /**
     * Get stored user email from session
     */
    fun getUserEmail(): kotlinx.coroutines.flow.Flow<String?> {
        return sessionManager.userEmail
    }

    fun loadCountries(forcedRefresh: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading countries...")
                val categoriesList = repository.getCountries(forcedRefresh )
                _countries.value = categoriesList
                Log.d(TAG, "Countries loaded: ${categoriesList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading countries", e)
                _countries.value = emptyList()
            }
        }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String = "",
    val country: Int = 0,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isCheckingSession: Boolean = true // Track if we're checking existing session
)