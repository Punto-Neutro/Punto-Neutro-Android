package com.example.sprint_2_kotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sprint_2_kotlin.model.data.AppDatabase
import com.example.sprint_2_kotlin.model.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(
            application,
            AppDatabase.getDatabase(application).CommentDao(),
            AppDatabase.getDatabase(application).newsItemDao(),
            AppDatabase.getDatabase(application).PQRSDao(),
            AppDatabase.getDatabase(application).PQRS_typesDao()
    )

    private val _uiState = MutableStateFlow(ForgotState())
    val uiState: StateFlow<ForgotState> = _uiState

    fun onEmailChange(email: String) { _uiState.value = _uiState.value.copy(email = email) }
    fun onPasswordChange(pw: String) { _uiState.value = _uiState.value.copy(newPassword = pw) }

    fun sendResetEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.sendResetPasswordEmail(_uiState.value.email)
            _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    emailSent = success
            )
        }
    }

    fun resetPassword() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.updatePassword(_uiState.value.newPassword)
            _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isResetSuccessful = success
            )
        }
    }

    data class ForgotState(
            val email: String = "",
            val newPassword: String = "",
            val isLoading: Boolean = false,
            val emailSent: Boolean = false,
            val isResetSuccessful: Boolean = false
    )
}

