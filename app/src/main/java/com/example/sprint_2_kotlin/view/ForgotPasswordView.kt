package com.example.sprint_2_kotlin.view

import android.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sprint_2_kotlin.viewmodel.ForgotPasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordView(
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // Define fixed colors to ignore the theme's dark mode
    val fixedWhite = Color.White
    val fixedBlack = Color(0xFF1A1A1A)
    val fixedGrey = Color(0xFF666666)



    Scaffold(
        containerColor = fixedWhite,
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.emailSent) {
                // PHASE 1: Enter Email
                Text("Enter your email to receive a reset link", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.sendResetEmail() },
                    enabled = state.email.isNotEmpty() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = fixedBlack,
                        contentColor = fixedWhite
                    )
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Text("Send Reset Link")
                }
            } else if (!state.isResetSuccessful) {
                // PHASE 2: Update Password
                Text(
                    "Check your email! Click the link in the message, then return here to set your new password.",
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.resetPassword() },
                    enabled = state.newPassword.length >= 6 && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Password")
                }
            } else {
                // PHASE 3: Success
                Text("Password updated successfully!", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBackToLogin) { Text("Go to Login") }
            }
        }
    }
}

