# Android client

This directory is the Kotlin + Jetpack Compose Android client for Lost
Treasures. It connects directly to Firebase Authentication, Cloud Firestore,
and Firebase Storage; there is no local or hosted Spring Boot server.

## Open and run

1. Complete the Firebase setup in the repository [README](../README.md),
   including placing `google-services.json` in `app/google-services.json` and
   deploying Firestore and Storage rules.
2. Open this `frontend` directory in Android Studio.
3. Wait for Gradle sync to complete.
4. Choose an Android emulator or physical device with internet access.
5. Select **Run**.

If Android Studio reports a Gradle cache issue, use **File → Sync Project with
Gradle Files**. For emulator install failures, inspect the `INSTALL_FAILED...`
line in the Run output; a test-only emulator can be reset from Device Manager
with **Wipe Data**.

## App navigation

| Bottom tab | Current behaviour |
| --- | --- |
| `treasure` | Placeholder for future treasure gameplay |
| `Rooms` | Create/join a two-person room and room chat |
| `Map` | Placeholder for future map support |
| `friend` | Friend search, requests, friend list, and private chat |
| `profile` | Profile view/edit and sign out |

For end-user instructions, data model, requirements, and acceptance criteria,
see the repository [README](../README.md) and
[REQUIREMENTS.md](../REQUIREMENTS.md).
