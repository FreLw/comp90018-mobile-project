package com.comp90018.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.data.auth.FirebaseAuthRepository
import com.comp90018.app.features.auth.AuthScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
            LocalFirebaseTestEnvironment.connect(this)
        }
        setContent { Comp90018App() }
    }
}

/** Application entry point: it only selects the signed-out or signed-in flow. */
@Composable
private fun Comp90018App() {
    val auth = remember { FirebaseAuth.getInstance() }
    val authRepository = remember(auth) { FirebaseAuthRepository(auth) }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val session: AuthSessionViewModel = viewModel()
    var user by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth, session) {
        val listener = FirebaseAuth.AuthStateListener {
            session.updateUser(it.currentUser?.uid)
            user = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            if (user == null) AuthScreen(authRepository)
            else {
                val signedInUser = requireNotNull(user)
                key(signedInUser.uid) {
                    CompositionLocalProvider(LocalViewModelStoreOwner provides session.ownerFor(signedInUser.uid)) {
                        AppShell(signedInUser, firestore, onLogout = {
                            // Detach authenticated listeners before Firebase revokes their access.
                            session.updateUser(null)
                            auth.signOut()
                        })
                    }
                }
            }
        }
    }
}
