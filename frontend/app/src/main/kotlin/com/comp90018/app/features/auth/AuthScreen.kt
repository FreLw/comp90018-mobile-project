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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicGold
import com.comp90018.app.data.auth.AuthRepository
import com.comp90018.app.ui.components.AppTextField

@Composable
fun AuthScreen(repository: AuthRepository) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LOST TREASURES", style = MaterialTheme.typography.labelLarge, color = RelicGold)
                Text(if (state.loginMode) "Return to the hunt" else "Begin your hunt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(if (state.loginMode) "Sign in to continue your campus adventure." else "Your explorer profile will be created automatically.", color = Muted)
                AppTextField("Email", state.email, viewModel::updateEmail, keyboardType = KeyboardType.Email)
                AppTextField("Password (at least 6 characters)", state.password, viewModel::updatePassword, password = true, keyboardType = KeyboardType.Password)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { viewModel.submit(validateCredentials(state.email, state.password)) },
                    modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp), enabled = !state.submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (state.submitting) "Please wait..." else if (state.loginMode) "Sign in" else "Create account") }
                TextButton(onClick = viewModel::toggleMode, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.loginMode) "No account? Create one" else "Already have an account? Sign in", color = Brand)
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
