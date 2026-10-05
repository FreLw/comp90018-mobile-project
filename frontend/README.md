# Lost Treasures Android client

This directory is the Kotlin and Jetpack Compose Android client for Lost
Treasures. It connects directly to Firebase Authentication, Cloud Firestore,
and Firebase Storage; no Spring Boot server is required.

## Code structure

The app separates UI state from Firebase operations:

- `features/` contains Compose screens, sizeable `Content` sections, and
  ViewModels grouped by user-facing feature.
- `data/` contains domain models, repository interfaces, Firebase adapters,
  and small Firebase service objects grouped by data domain.
- `sensors/` wraps Android hardware APIs behind testable interfaces.
- `contextengine/` combines sensor values and evaluates challenge rules.
- `ui/components/` contains reusable UI with no feature ownership.
- The ViewModels own snapshot-listener cleanup and expose `StateFlow` UI state.

The dependency direction is `Screen → ViewModel → Repository → Firebase
adapter/service`. Feature screens do not directly invoke Firebase services or
Firestore/Storage APIs. See [ARCHITECTURE.md](ARCHITECTURE.md) for the complete
package tree, naming convention, data flow, and presentation talking points.

## Run in Android Studio

1. Complete the Firebase setup in the repository [README](../README.md),
   including placing `google-services.json` in `app/google-services.json` and
   deploying the Firestore and Storage rules.
2. Open this `frontend` directory in Android Studio.
3. Configure Android Studio to use JDK 21 and install Android SDK Platform 37.
4. Add a Google Maps API key to your local Gradle user properties file. Do not
   commit the key to this repository. Alternatively, copy
   `local.properties.sample` to `local.properties` and set the key there.

   ```properties
   # ~/.gradle/gradle.properties
   MAPS_API_KEY=your_google_maps_api_key
   ```

   The key needs **Maps SDK for Android** enabled. If the key is restricted to
   Android apps, use package name `com.comp90018.app` and the SHA-1 fingerprint
   of the machine that builds the debug APK.
5. Wait for Gradle sync to complete, choose an Android API 26+ emulator or
   physical device with internet access, and select **Run**.

To build a debug APK from PowerShell instead:

```powershell
.\gradlew.bat assembleDebug
```

## Navigation

| Bottom tab | Current behaviour |
| --- | --- |
| `Treasure` | Hunt route and discovered/undiscovered treasure details |
| `Rooms` | Team-room chat and cooperative treasure hunts |
| `Map` | Proximity discovery, compass hunt and treasure challenges |
| `Friends` | Chats, contacts, requests, profiles and collectible stickers |
| `Profile` | Profile/settings editing, About and sign out |

For end-user instructions, Firebase setup, the data model, and detailed
acceptance criteria, see the repository [README](../README.md) and
[REQUIREMENTS.md](../REQUIREMENTS.md).
