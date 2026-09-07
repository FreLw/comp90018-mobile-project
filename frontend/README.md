# Android frontend

This Kotlin + Jetpack Compose app connects directly to Firebase Authentication
and Cloud Firestore. It does not require a local or hosted Spring Boot server.

## Firebase setup

1. Add an Android app with package name `com.comp90018.app` in Firebase.
2. Put `google-services.json` at `frontend/app/google-services.json`.
3. Enable **Authentication > Sign-in method > Email/Password**.
4. Create Cloud Firestore and deploy the repository's rules:

   ```powershell
   firebase deploy --only firestore
   ```

## Run it

Open this `frontend` directory in Android Studio, sync Gradle, and run the app
on an emulator or physical device with internet access. Registration and login
use Firebase Authentication; the signed-in profile is created at and observed
from `users/{uid}` in Cloud Firestore.
