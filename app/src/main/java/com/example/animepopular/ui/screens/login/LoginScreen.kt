package com.example.animepopular.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.LoginState
import com.example.animepopular.viewmodel.ProfileViewModel

@Composable
fun LoginScreen(
    viewModel: ProfileViewModel,
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit,
    language: String = "en"
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    var username      by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val focusManager  = LocalFocusManager.current

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success   -> onLoginSuccess()
            is LoginState.GuestMode -> onSkip()
            else -> {}
        }
    }

    fun validate(): Boolean {
        var valid = true
        usernameError = if (username.isBlank()) {
            valid = false
            if (language == "id") "Username tidak boleh kosong" else "Username is required"
        } else null
        passwordError = when {
            password.isBlank() -> { valid = false
                if (language == "id") "Password tidak boleh kosong" else "Password is required" }
            password.length < 6 -> { valid = false
                if (language == "id") "Password minimal 6 karakter" else "Min. 6 characters" }
            else -> null
        }
        return valid
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(ColorPrimary, BackgroundDark, BackgroundDark)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Surface(shape = CircleShape, color = AccentColor.copy(0.15f), modifier = Modifier.size(96.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🐼", fontSize = 48.sp) }
            }
            Spacer(Modifier.height(16.dp))
            Text("PandaMind", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                if (language == "id") "Jelajahi manga favoritmu" else "Explore your favorite manga",
                color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            AnimatedVisibility(visible = loginState is LoginState.Error,
                enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
                val msg = (loginState as? LoginState.Error)?.message ?: ""
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = username, onValueChange = { username = it; usernameError = null; if (loginState is LoginState.Error) viewModel.resetLoginState() },
                label = { Text("Username", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextSecondary) },
                isError = usernameError != null,
                supportingText = { usernameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true, shape = RoundedCornerShape(14.dp), colors = loginTfColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; passwordError = null; if (loginState is LoginState.Error) viewModel.resetLoginState() },
                label = { Text("Password", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = TextSecondary)
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = { passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true, shape = RoundedCornerShape(14.dp), colors = loginTfColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); if (validate()) viewModel.login(username.trim(), password) }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { focusManager.clearFocus(); if (validate()) viewModel.login(username.trim(), password) },
                enabled = loginState !is LoginState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(50.dp), modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                AnimatedContent(targetState = loginState is LoginState.Loading, label = "login_btn") { isLoading ->
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(if (language == "id") "Masuk..." else "Signing in...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Login, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (language == "id") "Masuk" else "Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Divider(Modifier.weight(1f), color = DividerColor)
                Text(if (language == "id") "  atau  " else "  or  ", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Divider(Modifier.weight(1f), color = DividerColor)
            }
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.loginAsGuest() }, enabled = loginState !is LoginState.Loading,
                shape = RoundedCornerShape(50.dp), border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.PersonOutline, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (language == "id") "Lanjutkan sebagai Tamu" else "Continue as Guest",
                    color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun loginTfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentColor, unfocusedBorderColor = DividerColor,
    focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor,
    cursorColor = AccentColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedLabelColor = AccentColor, unfocusedLabelColor = TextSecondary,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorContainerColor = MaterialTheme.colorScheme.error.copy(0.05f)
)