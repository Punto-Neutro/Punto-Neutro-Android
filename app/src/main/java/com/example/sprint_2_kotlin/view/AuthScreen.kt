package com.example.sprint_2_kotlin.view

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.extensions.isNotNull
import com.example.sprint_2_kotlin.R
import com.example.sprint_2_kotlin.model.auth.BiometricAuthManager
import com.example.sprint_2_kotlin.model.data.Category
import com.example.sprint_2_kotlin.model.data.Country
import com.example.sprint_2_kotlin.viewmodel.AuthViewModel
import utils.getTranslatedCategoryName
import utils.getTranslatedCountryName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var succesMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val biometricManager = remember {
        BiometricAuthManager(context as FragmentActivity)
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    val countries by viewModel.countries.collectAsState()


    // Navigation check - auto navigate if session exists
    LaunchedEffect(state.isSuccess, state.isCheckingSession) {
        if (state.isSuccess && !state.isCheckingSession) {
            println("DEBUG AuthScreen: isSuccess = true, calling onLoginSuccess()")
            onLoginSuccess()
        }
    }

    // Show loading while checking existing session
    if (state.isCheckingSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.Login_in_automatically),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //  Logo y Header
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF1A1A1A)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Punto Neutro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.Fighting_misinformation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.A_collaborative_platform_where___),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(20.dp))

                //  periodico imagen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.periodico),
                        contentDescription = "News Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(16.dp))

                //  Features Lista
                FeatureItem(stringResource(R.string.Source_credibility))
                Spacer(Modifier.height(6.dp))
                FeatureItem(stringResource(R.string.Realtime_collaborative_verification))
                Spacer(Modifier.height(6.dp))
                FeatureItem(stringResource(R.string.Network_of_verified_users))

                Spacer(Modifier.height(24.dp))

                //  "Access your account" section
                Text(
                    text = stringResource(R.string.Access_your_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.Enter_your_credentials_to_continue),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )

                Spacer(Modifier.height(20.dp))

                // los Tabs: Sign In / Register
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        text = stringResource(R.string.Sign_in),
                        selected = isLoginMode,
                        onClick = {
                            isLoginMode = true
                            errorMessage = ""
                            succesMessage = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = stringResource(R.string.Register),
                        selected = !isLoginMode,
                        onClick = {
                            isLoginMode = false
                            errorMessage = ""
                            succesMessage = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // el Email Input
                Text(
                    text = stringResource(R.string.Email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = { Text(stringResource(R.string.You_example_com), color = Color(0xFFBBBBBB)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1A1A1A),
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )

                Spacer(Modifier.height(16.dp))

                // el Password Input
                Text(
                    text = stringResource(R.string.Password),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(6.dp))



                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = { Text("••••••••", color = Color(0xFFBBBBBB)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF888888)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1A1A1A),
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )

                Spacer(Modifier.height(16.dp))

                if(!isLoginMode){

                    Text(
                        text = stringResource(R.string.Country),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Start)
                    )


                    Spacer(Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value =  selectedCountry?.let { getTranslatedCountryName(it.country_name) } ?: stringResource(R.string.Select_Your_Country) ,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            countries.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(getTranslatedCountryName(country.country_name)) },
                                    onClick = {
                                        selectedCountry = country
                                        expanded = false
                                        Log.d(TAG, "Selected country = ${selectedCountry?.country_name}")
                                        Log.d(TAG, "Selected country id= ${selectedCountry?.id}")

                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))



                // Main Action Button negro
                Button(
                    onClick = {
                        errorMessage = ""
                        succesMessage = ""
                        if (isLoginMode) {
                            viewModel.login()
                        } else {
                            // Add a check to ensure a country is selected
                            if (selectedCountry?.id != null) { // Check for a valid ID, not 0
                                viewModel.register(selectedCountry?.id ?: 0)
                                succesMessage = "Register successful verify email before logging in"
                                Log.d(TAG, "Selected country = ${selectedCountry?.id}")
                            } else {
                                Log.d(TAG, "Selected country = ${selectedCountry?.id}")
                                errorMessage = "Please select a country." // Show an error

                            }
                        }
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A),
                        contentColor = Color.White
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) stringResource(R.string.Sign_in) else stringResource(R.string.Register),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                //  Fingerprint boton
                if (isLoginMode && biometricManager.isBiometricAvailable()) {
                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            errorMessage = ""
                            biometricManager.authenticate(
                                onSuccess = { viewModel.loginWithBiometric() },
                                onError = { error -> errorMessage = error }
                            )
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1A1A1A)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.Sign_in) + " " + stringResource(R.string.With_fingerprint),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 🔗 Forgot Password (SOLO PARA login mode)
                if (isLoginMode) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { /* TODO: Implement forgot password */ }) {
                        // Inside AuthScreen.kt
                        Text(text = stringResource(R.string.Forgot_Password),
                            modifier = Modifier
                                .clickable { onForgotPassword() }, // Add navigation
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF000000)
                        )
                    }
                }

                //  Error message display (local errors from biometric)
                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (succesMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF90EE90)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = "Success",
                                tint = Color(0xFF00913F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = succesMessage,
                                color = Color(0xFF00913F),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// Feature Item Component
@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Check",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF555555),
            fontSize = 13.sp
        )
    }
}

// Tab Button Component
@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFE8E8E8) else Color.Transparent,
            contentColor = if (selected) Color(0xFF1A1A1A) else Color(0xFF888888)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}