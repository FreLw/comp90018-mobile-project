package com.comp90018.app.features.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.FirebaseAuthService
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicGold
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthScreen(auth: FirebaseAuth) {
    var loginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LOST TREASURES", style = MaterialTheme.typography.labelLarge, color = RelicGold)
                Text(if (loginMode) "Return to the hunt" else "Begin your hunt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(if (loginMode) "Sign in to continue your campus adventure." else "Your explorer profile will be created automatically.", color = Muted)
                AppTextField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                AppTextField("Password (at least 6 characters)", password, { password = it }, password = true, keyboardType = KeyboardType.Password)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        error = validateCredentials(email, password)
                        if (error != null) return@Button
                        submitting = true
                        val complete: (String?) -> Unit = { message -> submitting = false; error = message }
                        if (loginMode) FirebaseAuthService.login(auth, email, password, complete) else FirebaseAuthService.register(auth, email, password, complete)
                    },
                    modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp), enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (submitting) "Please wait…" else if (loginMode) "Sign in" else "Create account") }
                TextButton(onClick = { loginMode = !loginMode; error = null }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (loginMode) "No account? Create one" else "Already have an account? Sign in", color = Brand)
                }
            }
        }
    }
}

private fun validateCredentials(email: String, password: String): String? = when {
    email.isBlank() || password.isBlank() -> "Enter your email and password"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
    password.length < 6 -> "Password must be at least 6 characters"
    else -> null
}
