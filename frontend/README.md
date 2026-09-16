# Lost Treasures Android client

This directory is the Kotlin and Jetpack Compose Android client for Lost
Treasures. It connects directly to Firebase Authentication, Cloud Firestore,
and Firebase Storage; no Spring Boot server is required.

## Code structure

The app separates UI state from Firebase operations:

- `features/` contains Compose screens and ViewModels.
- `data/` contains repository interfaces plus Firebase implementations for
  authentication, profiles, social features, direct chat, and team rooms.
- The ViewModels own snapshot-listener cleanup and expose `StateFlow` UI state.

Firebase service calls are contained in data-layer adapters; feature screens do
not directly invoke Firebase services or Firestore/Storage APIs.

## Run in Android Studio

1. Complete the Firebase setup in the repository [README](../README.md),
   including placing `google-services.json` in `app/google-services.json` and
   deploying the Firestore and Storage rules.
2. Open this `frontend` directory in Android Studio.
3. Configure Android Studio to use JDK 21 and install Android SDK Platform 37.
4. Wait for Gradle sync to complete, choose an Android API 26+ emulator or
   physical device with internet access, and select **Run**.

To build a debug APK from PowerShell instead:

```powershell
.\gradlew.bat assembleDebug
```

## Navigation

| Bottom tab | Current behaviour |
| --- | --- |
| `Profile` | Profile view/edit, local display preferences, and sign out |
| `Social` | Friend lookup, private chat, team rooms, and room chat |
| `Map` | Offline campus map prototype with selectable relic markers and the complete demo search flow |
| `Hunt` | Current active treasure and its hunt progress |
| `Collection` | Previously found relics and their distance from the player |

The map currently uses a local illustrated placeholder instead of a map SDK. The
searching state simulates sensor readiness for two seconds before the relic can
be collected; it is intentionally structured so GPS/orientation state can
replace the delay later.

For end-user instructions, Firebase setup, the data model, and detailed
acceptance criteria, see the repository [README](../README.md) and
[REQUIREMENTS.md](../REQUIREMENTS.md).
