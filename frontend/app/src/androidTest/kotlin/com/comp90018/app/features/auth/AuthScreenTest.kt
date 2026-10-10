package com.comp90018.app.features.auth

/*
 * Checks authentication form presentation, input handling, and validation with a fake repository.
 * Run these device/Compose checks when changing the corresponding interface or interaction contract.
 */

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.comp90018.app.RelicColorScheme
import com.comp90018.app.RelicTypography
import com.comp90018.app.data.auth.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun signedOutScreenShowsLoginControls() {
        composeRule.setContent { TestTheme { AuthScreen(FakeAuthRepository()) } }

        composeRule.onNodeWithText("Return to the hunt").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password (at least 6 characters)").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in").assertIsDisplayed()
    }

    @Test
    fun emptyLoginShowsValidationErrorWithoutCallingRepository() {
        val repository = FakeAuthRepository()
        composeRule.setContent { TestTheme { AuthScreen(repository) } }

        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.onNodeWithText("Enter your email and password").assertIsDisplayed()
        assertNull(repository.loginRequest)
    }

    @Test
    fun registrationSubmitsEnteredCredentials() {
        val repository = FakeAuthRepository()
        composeRule.setContent { TestTheme { AuthScreen(repository) } }

        composeRule.onNodeWithText("No account? Create one").performClick()
        composeRule.onNodeWithText("Email").performTextInput("explorer@example.com")
        composeRule.onNodeWithText("Password (at least 6 characters)").performTextInput("secret12")
        composeRule.onNodeWithText("Create account").performClick()

        composeRule.runOnIdle {
            assertEquals("explorer@example.com" to "secret12", repository.registerRequest)
        }
    }
}

@androidx.compose.runtime.Composable
private fun TestTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography, content = content)
}

private class FakeAuthRepository : AuthRepository {
    var loginRequest: Pair<String, String>? = null
    var registerRequest: Pair<String, String>? = null

    override fun login(email: String, password: String, onComplete: (String?) -> Unit) {
        loginRequest = email to password
        onComplete(null)
    }

    override fun register(email: String, password: String, onComplete: (String?) -> Unit) {
        registerRequest = email to password
        onComplete(null)
    }
}
