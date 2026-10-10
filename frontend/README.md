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

For UI editing locations, source responsibilities, and visual assets, see the
[UI directory and maintenance guide](docs/ui-directory-guide.md) and the
[Frontend and UI code guide](docs/ui-code-guide.md).
For the complete current file inventory and architecture assessment, see the
[UI architecture review](docs/ui-architecture-review.md).

## Run in Android Studio

1. Complete the Firebase setup in the repository [README](../README.md),
   including placing `google-services.json` in `app/google-services.json` and
   deploying the Firestore and Storage rules.
2. Open this `frontend` directory in Android Studio.
3. Install Android SDK Platform 37. This checkout pins the Gradle daemon to
   JDK 25 in `gradle/gradle-daemon-jvm.properties`; use that daemon JVM
   configuration in Android Studio (the installed `jbr-25` works). The app's
   Java source/target compatibility remains 21.
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
5. Wait for Gradle sync to complete, select the `app` run configuration and
   the `debug` build variant, choose an Android API 26+ emulator or physical
   device with internet access, and select **Run**.

### Emulator setup on Apple silicon

If `Pixel 10 API 37.1` already appears in the device selector, reuse it.
To create a device, open **View > Tool Windows > Device Manager**, click
**+ > Create Virtual Device**, and select a Pixel phone profile and a
**Google Play** or **Google APIs** ARM64 (`arm64-v8a`) system image.
API 37 matches the project's target SDK. Google-enabled images provide the
Play services used by Maps and device location.

For the photograph tasks, configure the back camera as **VirtualScene** in
the device's advanced settings. For a campus GPS fix, open the running
emulator's **Extended Controls > Location** and send latitude `-37.7986`,
longitude `144.9602`, the centre used by this app's map. Allow location or
camera access in the app when using the corresponding feature.

If **Run** is grey and Android Studio says Gradle files have changed, click
**Sync Project with Gradle Files** (or **Sync Now**) and wait for sync to
finish. Confirm that `app`, `debug`, and the running emulator are selected,
then press **Run**. The **Assemble** action builds an APK; **Run** also
installs it and opens the app.

See Android's [virtual-device setup guide](https://developer.android.com/studio/run/managing-avds)
and [Gradle JDK settings](https://developer.android.com/build/jdks) for the
corresponding Android Studio controls.

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
[REQUIREMENTS.md](../docs/REQUIREMENTS.md).
