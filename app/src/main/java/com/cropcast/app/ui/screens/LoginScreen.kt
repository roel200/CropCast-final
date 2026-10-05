package com.cropcast.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.R
import com.cropcast.app.ui.localization.localize
import com.cropcast.app.ui.localization.tr
import com.cropcast.app.ui.theme.CropGreen
import com.cropcast.app.ui.theme.DeepGreen

@Composable
fun LoginScreen(
    isLoading: Boolean,
    isDemo: Boolean,
    message: String?,
    language: String,
    darkMode: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onContinueAsGuest: () -> Unit,
    onGoogleLogin: (suspend () -> String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onMessageShown: () -> Unit
) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val localizedMessage = message?.let { localize(it, language) }
    val mismatch = registering && confirm.isNotEmpty() && confirm != password

    LaunchedEffect(localizedMessage) {
        localizedMessage?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    fun submit() {
        focusManager.clearFocus()
        if (registering) {
            if (confirm != password) return
            onRegister(name, email, password)
        } else {
            onLogin(email, password)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Filipino", "English").forEach { option ->
                    FilterChip(
                        selected = language.equals(option, ignoreCase = true),
                        onClick = { onLanguageChange(option) },
                        label = { Text(option) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                IconButton(onClick = { onDarkModeChange(!darkMode) }) {
                    Icon(
                        if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        tr(if (darkMode) "Switch to light mode" else "Switch to dark mode"),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.cropcast_logo),
                contentDescription = tr("CropCast logo"),
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(18.dp))
            Text(
                tr("Welcome to CropCast"),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                tr(if (registering) "Create your account to start monitoring your farm." else "Sign in to monitor your farm and sensor data."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (registering) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Full name")) },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Email address")) },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Password")) },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    tr(if (passwordVisible) "Hide password" else "Show password")
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (registering) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                    if (registering) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Confirm password")) },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isLoading,
                            isError = mismatch,
                            supportingText = if (mismatch) {
                                { Text(tr("Passwords do not match")) }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() })
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(tr(if (registering) "Create account" else "Log in"), fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                        Text(
                            tr("or"),
                            Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            onGoogleLogin { com.cropcast.app.data.GoogleSignIn.getIdToken(context) }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isLoading && !isDemo
                    ) {
                        GoogleBadge()
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tr("Continue with Google"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!registering) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onContinueAsGuest,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !isLoading
                        ) {
                            Text(tr(if (isDemo) "Continue in demo mode" else "Continue as guest"), color = CropGreen)
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    registering = !registering
                    confirm = ""
                },
                enabled = !isLoading,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    tr(if (registering) "Already have an account? Log in" else "New here? Create an account"),
                    color = CropGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isDemo) {
                Text(
                    tr("Firebase is not configured. Log in starts a local demo session; cloud verification and synchronization are unavailable."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** Google "G" in its brand blue; the real logo is not in the Material icon set. */
@Composable
private fun GoogleBadge() {
    Box(
        Modifier.size(24.dp).background(Color.White, CircleShape).border(1.dp, Color(0xFFDADCE0), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}
