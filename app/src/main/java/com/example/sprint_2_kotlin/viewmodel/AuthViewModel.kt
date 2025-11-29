package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.network.NetworkStatusTracker
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AuthViewModel - Handles authentication logic
 *
 * CHANGE: Now extends AndroidViewModel to get Application context
 * This is needed because Repository now requires context for Room Database
 */
class AuthViewModel(
    application: Application // OJO CAMBIO: ahora recibe Application
) : AndroidViewModel(application) { // OJOO CAMBIO: extiende AndroidViewModel

    // IMPORTANTE CAMBIO: Repository ahora recibe context
    private val dao = AppDatabase.getDatabase(application).CommentDao()
    private val daonews = AppDatabase.getDatabase(application).newsItemDao()
    // CAMBIO: Repository ahora recibe context
    private val repository = Repository(application.applicationContext, dao,daonews)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState



    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.signIn(_uiState.value.email, _uiState.value.password)
            println("DEBUG: Login result = $success")
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = success)
        }
    }

    fun register() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.signUp(_uiState.value.email, _uiState.value.password)
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = success)
        }
    }

    fun loginWithBiometric() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Check if user session exists
            val isLoggedIn = repository.isUserLoggedIn()
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = isLoggedIn)
        }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)






