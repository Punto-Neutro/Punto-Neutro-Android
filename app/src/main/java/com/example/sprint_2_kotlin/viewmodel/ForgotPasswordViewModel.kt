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


    // State update functions (The ones that were missing!)
    fun onOtpChange(value: String) { _uiState.value = _uiState.value.copy(otpToken = value) }



    fun sendOtp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val exists = repository.sendResetPasswordEmail(_uiState.value.email) // Your existing check
            if (exists == 0) {
                _uiState.value = _uiState.value.copy(isLoading = false, phase = 2)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Email not found")
            }
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val success = repository.verifyResetToken(_uiState.value.email, _uiState.value.otpToken)
            if (success) {
                _uiState.value = _uiState.value.copy(isLoading = false, phase = 3)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Invalid or expired token")
            }
        }
    }

    fun resetPassword() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = repository.updatePassword(_uiState.value.newPassword)
            if (success) {
                _uiState.value = _uiState.value.copy(isLoading = false, phase = 4) // Success
            }
        }
    }

    data class ForgotState(
        val email: String = "",
        val newPassword: String = "",
        val isLoading: Boolean = false,
        val emailSent: Boolean = false,
        val isResetSuccessful: Boolean = false,
        val errorMessage: String? = null, // Add this to show to the user
        val phase: Int = 1, //
        val otpToken: String = ""
    )
}

