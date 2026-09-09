# Lost Treasures Android client

This directory is the Kotlin and Jetpack Compose Android client for Lost
Treasures. It connects directly to Firebase Authentication, Cloud Firestore,
and Firebase Storage; no Spring Boot server is required.

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
| `treasure` | Placeholder for future treasure gameplay |
| `Rooms` | Create/join a two-person room, room details, and room chat |
| `Map` | Placeholder for future map support |
| `friend` | Friend lookup, requests, friend list, and private chat |
| `profile` | Profile view/edit and sign out |

For end-user instructions, Firebase setup, the data model, and detailed
acceptance criteria, see the repository [README](../README.md) and
[REQUIREMENTS.md](../REQUIREMENTS.md).
