package com.example.sprint_2_kotlin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sprint_2_kotlin.viewmodel.ForgotPasswordViewModel

// ... imports
import androidx.compose.ui.res.stringResource
import com.example.sprint_2_kotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordView(onBackToLogin: () -> Unit, viewModel: ForgotPasswordViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val fixedBlack = Color(0xFF1A1A1A)
    val primaryBlue = Color(0xFF1976D2)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(state.phase) {
                            1 -> stringResource(R.string.forgot_password_title)
                            2 -> stringResource(R.string.verify_code_title)
                            3 -> stringResource(R.string.new_password_title)
                            else -> stringResource(R.string.success_title)
                        },
                        color = fixedBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.phase == 1 || state.phase == 4) {
                            onBackToLogin()
                        } else {
                            onBackToLogin() // Or implement back-phase logic
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.Close),
                            tint = fixedBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            when (state.phase) {
                1 -> { // EMAIL PHASE
                    Text(
                        text = stringResource(R.string.enter_email_instruction),
                        color = fixedBlack,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text(stringResource(R.string.Email)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = fixedBlack,
                            unfocusedTextColor = fixedBlack
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.sendOtp() },
                        enabled = !state.isLoading && state.email.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = fixedBlack)
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text(stringResource(R.string.send_code_button))
                    }
                }
                2 -> { // OTP PHASE
                    Text(
                        text = stringResource(R.string.enter_code_instruction, state.email),
                        color = fixedBlack,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.otpToken,
                        onValueChange = { if (it.length <= 8) viewModel.onOtpChange(it) },
                        label = { Text(stringResource(R.string.verification_code_label)) },
                        placeholder = { Text("12345678") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = fixedBlack,
                            unfocusedTextColor = fixedBlack
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.verifyOtp() },
                        enabled = !state.isLoading && state.otpToken.length == 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text(stringResource(R.string.verify_code_button))
                    }
                }
                3 -> { // PASSWORD PHASE
                    Text(
                        text = stringResource(R.string.set_new_password_instruction),
                        color = fixedBlack
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text(stringResource(R.string.new_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = fixedBlack,
                            unfocusedTextColor = fixedBlack
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetPassword() },
                        enabled = !state.isLoading && state.newPassword.length >= 6,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = fixedBlack)
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text(stringResource(R.string.update_password_button))
                    }
                }
                4 -> { // SUCCESS
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.password_updated_success),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = fixedBlack
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = fixedBlack)
                    ) {
                        Text(stringResource(R.string.return_to_login))
                    }
                }
            }
        }
    }
}