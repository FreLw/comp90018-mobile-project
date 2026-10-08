package com.comp90018.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/** Explicit Debug opt-in; uses a separate project and auth key from production. */
internal object LocalFirebaseTestEnvironment {
    private var connected = false

    fun connect(context: Context) {
        check(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        if (connected) return
        val original = FirebaseApp.getInstance()
        val options = FirebaseOptions.Builder(original.options)
            .setProjectId("demo-lost-treasures")
            .setApplicationId("1:123456789:android:0000000000000000000000")
            .setApiKey("fake-api-key")
            .setStorageBucket("demo-lost-treasures.appspot.com")
            .build()
        original.delete()
        val app = requireNotNull(FirebaseApp.initializeApp(context, options))
        // adb reverse forwards these device loopback ports to the host.
        FirebaseAuth.getInstance(app).useEmulator("127.0.0.1", 9099)
        FirebaseFirestore.getInstance(app).useEmulator("127.0.0.1", 8080)
        FirebaseStorage.getInstance(app).useEmulator("127.0.0.1", 9199)
        connected = true
    }
}
