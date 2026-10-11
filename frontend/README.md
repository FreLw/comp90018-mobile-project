# Lost Treasures Android client

This directory is the Kotlin and Jetpack Compose Android client for Lost
Treasures. It connects directly to Firebase Authentication, Cloud Firestore,
and Firebase Storage. Open this directory as the Android Studio project.

## Build configuration

| Setting | Checked-in value |
| --- | --- |
| Application ID / namespace | `com.comp90018.app` |
| Android SDK | Minimum 26; compile and target 37 |
| Gradle wrapper | 9.7.1 |
| Android Gradle Plugin | 9.3.3 |
| Compose compiler plugin | 2.3.21 |
| Gradle daemon JVM | JDK 25, selected by `gradle/gradle-daemon-jvm.properties` |
| App Java source/target compatibility | 21 |

## Code structure

The app separates UI state from Firebase operations:

- `features/` contains Compose screens, sizeable `Content` sections, and
  ViewModels grouped by user-facing feature.
- `features/navigation/` owns relic resonance guidance, direction hints, and
  arrival confirmation. `navigation/` owns the app's bottom tabs.
- `features/treasurechallenge/` owns the six physical challenges and their
  completion/save flow; `features/haptics/` owns proximity and confirmed-success
  feedback.
- `data/` contains domain models, repository interfaces, Firebase adapters,
  and small Firebase service objects grouped by data domain.
- `sensors/` wraps Android hardware APIs behind testable interfaces.
- `contextengine/` combines sensor values and evaluates challenge rules.
- `diagnostics/` reports sensor and environment errors through shared UI.
- `ui/components/` contains reusable UI with no feature ownership.
- The ViewModels own snapshot-listener cleanup and expose `StateFlow` UI state.

The dependency direction is `Screen → ViewModel → Repository → Firebase
adapter/service`. Feature screens do not directly invoke Firebase services or
Firestore/Storage APIs. See [ARCHITECTURE.md](ARCHITECTURE.md) for the complete
package tree, naming convention, data flow, and presentation talking points.

For UI editing locations, source responsibilities, and visual assets, see the
[UI directory and maintenance guide](docs/ui-directory-guide.md) and the
[Frontend and UI code guide](docs/ui-code-guide.md).

## Run in Android Studio

1. Complete the Firebase setup in the repository [README](../README.md),
   including placing `google-services.json` in `app/google-services.json` and
   deploying the Firestore and Storage rules.
2. Open this `frontend` directory in Android Studio.
3. Install Android SDK Platform 37. This checkout pins the Gradle daemon to
   JDK 25 in `gradle/gradle-daemon-jvm.properties`; use that daemon JVM
   configuration in Android Studio with an available JDK 25. The app's
   Java source/target compatibility remains 21.
4. Add a Google Maps API key to your local Gradle user properties file. Do not
   commit the key to this repository. Alternatively, copy
   `local.properties.sample` to `local.properties` and set the key there.

   ```properties
   # ~/.gradle/gradle.properties
   MAPS_API_KEY=your_google_maps_api_key
   ```

   Set `sdk.dir` in `local.properties` to your Android SDK location if Android
   Studio has not configured it. A Gradle `MAPS_API_KEY` property takes precedence
   over the value in `local.properties`.

   The key needs **Maps SDK for Android** enabled. If the key is restricted to
   Android apps, use package name `com.comp90018.app` and the SHA-1 fingerprint
   of the signing certificate used to build the APK.
5. Wait for Gradle sync to complete, select the `app` run configuration and
   the `debug` build variant, choose an Android API 26+ emulator or physical
   device with internet access, and select **Run**. The emulator needs Google
   Play services for Maps and fused location.

The live Firestore `treasures` collection must contain valid challenge
configurations. Regular team destinations also require `memberQuiz.questions`;
South Lawn multiplayer requires `fragmentHunt`. The app does not automatically
import `../database/treasures.json`. See the [database guide](../database/README.md)
for imports and targeted configuration updates.

### Emulator setup on Apple silicon

Reuse an existing compatible virtual device, or open Android Studio's Device
Manager, click
**+ > Create Virtual Device**, and select a Pixel phone profile and a
**Google Play** or **Google APIs** ARM64 (`arm64-v8a`) system image.
API 37 matches the project's target SDK. Google-enabled images provide the
Play services used by Maps and device location.

For the photograph tasks, configure the back camera as **VirtualScene** in
the device's advanced settings. For a campus GPS fix, open the running
emulator's **Extended Controls > Location** and send latitude `-37.7986`,
longitude `144.9602`, the centre used by this app's map. Allow location or
camera access in the app when using the corresponding feature.

These coordinates place the emulator near campus; each relic and fragment has
its own target coordinate. Emulator interaction checks do not validate physical
GPS, compass calibration, microphone response, or vibration sensation.

If **Run** is grey and Android Studio says Gradle files have changed, click
**Sync Project with Gradle Files** (or **Sync Now**) and wait for sync to
finish. Confirm that `app`, `debug`, and the running emulator are selected,
then press **Run**. The **Assemble** action builds an APK; **Run** also
installs it and opens the app.

See Android's [virtual-device setup guide](https://developer.android.com/studio/run/managing-avds)
and [Gradle JDK settings](https://developer.android.com/build/jdks) for the
corresponding Android Studio controls.

## Build and test from a terminal

Run these commands from `frontend` in PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`.
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
JVM test reports are generated at `app/build/reports/tests/testDebugUnitTest/`.

With an emulator or physical Android device connected, run the Compose
instrumentation tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

To check release compilation:

```powershell
.\gradlew.bat :app:assembleRelease
```

Unit tests cover sensor processing, location policy, navigation and hunt state,
catalogue parsing, challenge evaluation, ViewModels, and confirmed save/claim
haptics. Instrumentation tests cover authentication, photo tasks, navigation
panels, map/compass UI, profile saving, and lifecycle/claim feedback bindings.

Firestore Security Rules tests are a separate Node.js suite. From `frontend`,
run them in another terminal:

```powershell
cd ../tests/firestore-rules
npm ci
npm run test:emulator
```

The suite uses the local Firestore Emulator with `demo-lost-treasures`. See the
[rules test guide](../tests/firestore-rules/README.md) for details.

## Navigation

| Bottom tab | Current behaviour |
| --- | --- |
| `Treasure` | Collection and relic details; Follow the Resonance or View on map for undiscovered relics |
| `Rooms` | Public-room plaza, room-ID joining, team chat, settings, member management, and cooperative hunts |
| `Map` | Catalogue exploration, compass gate, relic challenges, team markers, quizzes, and South Lawn fragments |
| `Friends` | Chats, contacts, requests, profiles and collectible stickers |
| `Profile` | Profile/settings editing, About and sign out |

**Follow the Resonance** opens a dedicated navigation screen and hides the bottom
tabs until the player exits. **View on map** opens normal Map inspection.
Navigation confirms arrival using two distinct reliable fixes within 10 m, with
a 15 m exit radius. **Begin Hunt** hands off to Map's hunt routing; arrival alone
does not complete a challenge or collect a relic.

During regular team hunts, the host performs the physical challenge and other
members perform location-gated quizzes. South Lawn uses four shared fragments.
The team must satisfy the shared requirements before members claim individually.
Teammate positions are shared only during active hunts while foregrounded, using
real usable GPS readings; debug distance controls are not published.

## Permissions and development checks

- Grant location access for distance, navigation, and location-gated tasks.
  Stale/unusable readings and display fallback coordinates do not satisfy normal
  hunt requirements.
- Union Lawn and System Garden use CameraX and require camera access; Grainger
  Museum requires microphone access. A permission grant alone does not complete
  the task.
- Automatic treasure haptics require the loaded Profile setting to enable
  Haptics and the app to be foregrounded. Success feedback follows confirmed
  persistence and team claims rather than sensor readiness alone.
- Ordinary Debug runs use actual sensor input for physical challenges until
  simulation is explicitly enabled. Debug task previews use local fixtures and
  complete locally without saving collection or room progress. Preview values
  can differ from the live catalogue.
- Release builds omit the debug task fixtures and challenge simulator. Use
  physical devices for on-site calibration and two-device team acceptance checks.

## Further documentation

| Guide | Contents |
| --- | --- |
| [Project README](../README.md) | User flow, Firebase setup, challenge summary, data model, and limitations |
| [Architecture](ARCHITECTURE.md) | Repository boundaries, state flow, and feature ownership |
| [UI directory guide](docs/ui-directory-guide.md) | Screen locations and visual assets |
| [UI code guide](docs/ui-code-guide.md) | UI source responsibilities and maintenance |
| [Navigation device QA](docs/relic-navigation-real-device-qa.md) | Arrival, compass, lifecycle, and map checks |
| [Team location sharing](docs/team-hunt-location-sharing.md) | Sharing lifecycle and two-device verification |
| [Proximity haptics](docs/proximity-haptics.md) | Feedback policy and physical-device checklist |

The repository-root `docs/` directory is ignored by Git. Shared client guides
are maintained in this directory's tracked `docs/` folder.
