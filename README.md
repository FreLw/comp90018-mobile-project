# COMP90018 mobile project

## Architecture

```text
Android app
  -> Firebase Authentication for email/password login
  -> Cloud Firestore for profiles and application data
  -> Firestore Security Rules for authorization and validation
```

The project has no Spring Boot, MySQL, or Cloud Functions backend. The Android
app talks directly to Firebase and can remain on the Spark plan for the current
Authentication and Firestore features.

## Structure

```text
frontend/                 Kotlin + Jetpack Compose Android app
firebase.json             Firebase deployment/emulator configuration
firestore.rules           Firestore authorization and validation rules
firestore.indexes.json    Firestore index definitions
```

## Setup

1. Register Android package `com.comp90018.app` in Firebase.
2. Put the downloaded file at `frontend/app/google-services.json`.
3. Enable **Authentication > Sign-in method > Email/Password**.
4. Create the default Cloud Firestore database.
5. Deploy the Firestore rules and indexes:

   ```powershell
   firebase deploy --only firestore
   ```

6. Open the `frontend` directory in Android Studio and run the app.

Registration creates both the Firebase Authentication account and its
`users/{uid}` Firestore profile. All collections other than `users` are denied
until feature-specific security rules are added.
