# Lost Treasures

Developed by **Tutorial 01, Group 1** for **COMP90018** at the University of Melbourne.

Lost Treasures is an Android campus treasure-hunt app built for the University
of Melbourne. Players explore campus locations, follow map and compass guidance,
complete sensor challenges, collect historical relics, and cooperate through
team rooms. Social features include profiles, friends, direct messages, team
chat, and collected treasure stickers.

## Quick start

### Step 1: Environment Setup

Open the `frontend` folder in Android Studio and install **Android SDK Platform 37**.
The app uses **Java 21** source/target compatibility. This checkout selects
**JDK 25** for the Gradle daemon in `frontend/gradle/gradle-daemon-jvm.properties`;
use that Gradle JVM configuration in Android Studio.

### Step 2: Configuration

Place the supplied Firebase configuration file at
`frontend/app/google-services.json` if it is not already present.

Android Studio normally creates `frontend/local.properties` with the Android SDK
path for your computer, for example `C:/Users/<username>/AppData/Local/Android/Sdk`
on Windows. Keep the generated `sdk.dir` entry and add the Maps API key supplied
in the project report:

```properties
MAPS_API_KEY=PASTE_THE_MAPS_API_KEY_FROM_THE_PROJECT_REPORT
```

Alternatively, copy `frontend/local.properties.sample` to
`frontend/local.properties`, set your own SDK path, and replace the key placeholder.
If `MAPS_API_KEY` is also set in your user-level `~/.gradle/gradle.properties`, that
Gradle property takes precedence. Keep `local.properties` outside version control.

For an Android-restricted Maps key, the application ID `com.comp90018.app` and
the build's signing certificate SHA-1 must be authorised. Open Android Studio's
Terminal in the `frontend` folder and run:

**Windows**

```powershell
.\gradlew.bat :app:signingReport
```

**macOS / Linux**

```bash
./gradlew :app:signingReport
```

Provide the **debug SHA-1** to the project team so it can be added to the Maps
key's allowed Android applications. The key must have **Maps SDK for Android** enabled.

### Step 3: Build and Run

Complete Gradle sync, select the **app** run configuration and **debug** build
variant, and run on an Android API 26+ device or emulator with Google Play services.
Ensure internet access and grant **precise location** permission when prompted.
Grant camera or microphone access when the corresponding challenge requests it.

## Implemented features

### Campus treasure hunt

- Google Maps displays the treasure catalogue, the player's location, and
  distance and bearing guidance for the selected relic.
- **Follow the Resonance** opens navigation from an undiscovered Treasure entry,
  with a resonance gauge, animated Guiding Thread, direction hints, and confirmed
  arrival before **Begin Hunt** becomes available.
- Guided hunts lead through a compass gate to location-specific challenges at
  Union Lawn, Wilson Hall, Old Quad, South Lawn, System Garden, and the Grainger Museum.
- Historical stories, artwork, discovery state, and a personal treasure collection
  are backed by Firestore.
- Cooperative hunts combine host challenges, member quizzes, shared progress,
  and individual claims, including South Lawn's four-fragment hunt.
- Optional proximity and collection haptics follow the player's settings.
  Success feedback waits for confirmed saving and, for team hunts, the member's
  claim transaction.

### Sensors and context engine

- Fused Android location updates with permission handling, accuracy and staleness
  filtering, proximity hysteresis, and stale-signal recovery.
- Device heading, target direction, rotation, motion, and stability processing.
- Camera and microphone input for challenge-specific interactions.
- A deterministic context engine combining location and sensor states to evaluate
  challenge conditions and produce instructions.
- Debug-only sensor simulation and calibration controls for repeatable development.

Location-based actions require a current usable reading. Denied permissions,
stale readings, and display-only fallback coordinates cannot unlock a hunt in
normal play. Explicit debug simulation provides synthetic inputs for testing.

### Accounts and collaboration

- Email/password registration, sign-in, and sign-out through Firebase Authentication.
- Explorer profiles with username, gender, bio, and an optional profile photo.
- Exact username search, friend requests, friend management, and profiles.
- Real-time direct messages, including image and treasure-sticker sharing.
- Public-room browsing in the Room plaza, joining by room ID, room settings,
  owner member management, and real-time team chat.
- Foreground location sharing during active team hunts: current-session teammate
  markers update about every 10 seconds and expire when readings or heartbeats
  become stale. Sharing stops when backgrounded or the hunt ends.

## Challenge design

Each relic has a Firestore-backed challenge configuration. The context engine
checks location and the inputs required by that particular task:

| Campus location | Solo challenge |
| --- | --- |
| Union Lawn | Align with the configured direction, then capture a photo |
| Wilson Hall | Face the configured direction and hold still with stable sensors |
| Old Quad | Hold the phone level, stationary, and stable for the excavation |
| South Lawn | Hold the configured viewing angle with stable sensors |
| System Garden | Capture a photo after GPS proximity is confirmed |
| Grainger Museum | Sustain sound above the configured microphone threshold |

Navigation arrival, the pre-task compass gate, and the relic challenge have
separate rules. Navigation requires two distinct reliable fixes within 10 m;
confirmed arrival is retained up to 15 m while readings remain reliable. Arrival
allows the player to begin a hunt; collection still requires challenge completion
and a successful save.

For regular team hunts, the host completes the physical challenge and other
members complete location-gated quizzes from `memberQuiz.questions`. South Lawn
uses the shared four-fragment configuration in `fragmentHunt`. Missing or invalid
quiz or fragment configuration blocks the corresponding task. Quiz answers are
checked on the client and are readable by signed-in catalogue users.

The six configurations are included in
[`database/treasures.json`](database/treasures.json). All six calibration statuses
are `pending` in the checked-in snapshot; thresholds and bearings still require
physical-device validation at the campus locations. The JSON snapshot and patch
scripts are development tools; the running app reads the live Firestore catalogue.

## Technology

| Area | Implementation |
| --- | --- |
| Android client | Kotlin, Jetpack Compose, Material 3 |
| Architecture | Feature-oriented MVVM with repository interfaces |
| Build | Gradle 9.7.1, Android Gradle Plugin 9.3.3, Compose compiler plugin 2.3.21 |
| Java configuration | Gradle daemon JDK 25; app Java source/target compatibility 21 |
| Maps and location | Google Maps SDK for Android, Google Play services location |
| Device input | Android motion and orientation sensors, CameraX, microphone |
| Authentication | Firebase Authentication |
| Data and live updates | Cloud Firestore snapshot listeners |
| Images | Firebase Storage |
| Access control | Firestore and Storage Security Rules |

The app uses `minSdk 26`, `compileSdk 37`, `targetSdk 37`, and application ID
`com.comp90018.app`. Build versions come from the checked-in Gradle configuration.

## Architecture

Lost Treasures is a single-module Compose app with this primary dependency direction:

```text
Compose Screen -> ViewModel -> Repository -> Firebase adapter/service
```

ViewModels expose immutable UI state and own real-time subscription lifecycles.
Firebase calls stay behind repository implementations, and Android hardware APIs
are wrapped in testable sensor classes. The `contextengine` package combines their
structured outputs and evaluates challenge rules independently of the UI.

| Package | Responsibility |
| --- | --- |
| `features/` | Auth, chat, friends, map, navigation, profile, rooms, treasure, challenges, and haptics |
| `data/` | Domain models, repositories, Firebase adapters, and services |
| `sensors/` | Location, orientation, motion, stability, and sound abstractions |
| `contextengine/` | Context aggregation and deterministic challenge evaluation |
| `diagnostics/` | Shared sensor and environment diagnostics |
| `ui/components/` | Reusable presentation components |

See [`frontend/ARCHITECTURE.md`](frontend/ARCHITECTURE.md) for the package tree,
data flow, naming conventions, and feature ownership.

## Repository layout

```text
frontend/                 Android Studio / Gradle project
  app/                    App source, resources, and tests
  ARCHITECTURE.md         Detailed Android architecture guide
  docs/                   UI guides, feature notes, and device QA checklists
database/                 Treasure snapshot, importer, and configuration patch scripts
firebase/                 Firebase rules and indexes
  firestore.rules         Firestore authorization and validation rules
  firestore.indexes.json  Firestore indexes
  storage.rules           Firebase Storage rules
tests/firestore-rules/    Firestore Emulator security rules tests
firebase.json             Firebase deployment and emulator configuration
.firebaserc               Firebase project selection
.gitignore                Git ignore rules
```

The root `docs/` directory is ignored by Git and may contain local course documents;
it is not included in a fresh clone. Shared implementation guides are tracked
under `frontend/docs/`.

## Local setup

### Prerequisites

- Android Studio with Android SDK Platform 37 and a JDK 25 Gradle daemon, as
  selected by `frontend/gradle/gradle-daemon-jvm.properties`.
- An Android API 26+ emulator with Google Play services or a physical device
  with internet access. Use physical devices for sensor and campus validation.
- A Firebase project with the Firebase CLI installed.
- A Google Cloud project with **Maps SDK for Android** enabled.
- Node.js and npm for database tools and Firestore rules tests; Python 3.8+
  for the targeted configuration patch scripts.

### Provisioning a new Firebase project

1. Choose the Firebase project you intend to use and register an Android app
   with application ID `com.comp90018.app`.
2. Download `google-services.json` and place it at
   `frontend/app/google-services.json`, replacing the supplied configuration
   only if you are switching projects.
3. Enable Email/Password in Firebase Authentication.
4. Create the default Cloud Firestore database and enable Firebase Storage.
5. Authenticate the Firebase CLI with `firebase login`. From the repository
   root, deploy the rules and indexes to the same project as the app configuration:

   ```bash
   firebase deploy --only firestore,storage --project YOUR_FIREBASE_PROJECT_ID
   ```

### Treasure catalogue

The app reads relic definitions from the Firestore `treasures` collection.
For an initial import into a new project, use a service account belonging to
that target project:

```bash
cd database
npm install
node import-treasures.mjs --key /path/to/service-account.json --target YOUR_FIREBASE_PROJECT_ID
```

On Windows, the importer also supports `--firebase-cli` instead of `--key`,
using an existing Firebase CLI login from `%USERPROFILE%/.config/configstore/`.
Replace the service-account path with the location of your local file and never
commit credentials.

The importer replaces the six complete treasure documents, including existing
fields. For updates to an existing catalogue, use the targeted Python scripts
described in [`database/README.md`](database/README.md) for challenge rules,
compass thresholds, member quizzes, and South Lawn fragments. They preview
changes by default; `--apply` writes the changes with document update-time checks
and local backups.

## Build and test

Open `frontend` in Android Studio, wait for Gradle sync, select a device, and
choose **Run**. In PowerShell, from the repository root:

```powershell
cd frontend
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`.
The debug APK is generated at `frontend/app/build/outputs/apk/debug/app-debug.apk`.

Run Compose instrumentation tests with a connected emulator or Android device
from `frontend`:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Run Firestore Security Rules tests from the repository root in a separate terminal:

```powershell
cd tests/firestore-rules
npm ci
npm run test:emulator
```

The script launches the local Firestore Emulator using `demo-lost-treasures`.
It exercises profile and collection ownership, catalogue access, room join and
management rules, team completion and claims, messages, and shared-location
session and timestamp validation. See
[`tests/firestore-rules/README.md`](tests/firestore-rules/README.md).

Unit tests cover location calculations, invalid/stale location policy, direction,
motion and sound processing, treasure parsing, map and hunt state transitions,
navigation arrival, haptic save/claim behaviour, challenge rules, and
ViewModel/repository behaviour. Camera, microphone, compass, GPS, permission,
poor-signal, and on-site threshold behaviour additionally require physical-device
checks. Build success does not establish that these manual checks have passed.

## Main user flow

1. Register or sign in and complete the explorer profile.
2. Open an undiscovered relic in **Treasure** and choose **Follow the Resonance**,
   or select a relic directly in **Map**.
3. Follow navigation guidance, confirm arrival, and choose **Begin Hunt**; the
   Map hunt entry then applies the relevant compass and task requirements.
4. Complete the relic's sensor challenge. After saving succeeds, reveal its
   history and artwork.
5. Review collected and undiscovered relics in **Treasure**.
6. In **Rooms**, join or create a team. The host selects and starts a hunt;
   members complete their assigned tasks or collect South Lawn fragments.
   Each member claims the treasure after shared completion.
7. Use **Friends** for direct chat and treasure-sticker sharing, and **Profile**
   to edit explorer details and preferences.

## Firestore data model

| Path | Purpose |
| --- | --- |
| `users/{uid}` | Explorer profile and settings |
| `users/{uid}/friends/{friendUid}` | Accepted friendship and unread state |
| `users/{uid}/treasureCollection/{treasureId}` | The user's collected relics |
| `friendRequests/{requestId}` | Incoming and outgoing friend requests |
| `rooms/{roomId}` | Direct-chat room metadata |
| `rooms/{roomId}/messages/{messageId}` | Private direct-chat messages |
| `teamMemberships/{uid}` | The user's current team-room membership |
| `teamRooms/{roomId}` | Team membership and cooperative hunt state |
| `teamRooms/{roomId}/messages/{messageId}` | Team-room messages |
| `teamRooms/{roomId}/locations/{uid}` | Active hunt-session positions and server heartbeats |
| `treasures/{treasureId}` | Relic metadata, challenge rules, compass thresholds, member quizzes, and fragments |

## Known limitations

- Challenge thresholds and bearings are development values and still require
  on-site physical-device calibration.
- Hardware behaviour varies by device, sensor quality, magnetic interference,
  GPS conditions, and Android permission choice.
- Core online features require Firebase connectivity; map rendering requires a
  valid Maps API key and network access.
- Debug simulation tools are excluded from release builds and are not evidence
  of physical-device validation.
- Quizzes and sensor challenges are evaluated on the client. Security Rules
  enforce data access and team-state constraints, but do not independently
  verify quiz answers or physical sensor evidence.

## Further documentation

| Guide | Contents |
| --- | --- |
| [Android client](frontend/README.md) | Android Studio setup and client navigation |
| [Architecture](frontend/ARCHITECTURE.md) | Packages, data flow, and feature ownership |
| [UI directory guide](frontend/docs/ui-directory-guide.md) | Screen locations and visual assets |
| [UI code guide](frontend/docs/ui-code-guide.md) | UI maintenance and source responsibilities |
| [Database tools](database/README.md) | Catalogue snapshot and targeted configuration updates |
| [Firestore rules tests](tests/firestore-rules/README.md) | Emulator setup and rules coverage |
| [Team location sharing](frontend/docs/team-hunt-location-sharing.md) | Sharing lifecycle and two-device checks |
| [Proximity haptics](frontend/docs/proximity-haptics.md) | Feedback policy and physical-device checklist |
| [Relic navigation QA](frontend/docs/relic-navigation-real-device-qa.md) | Arrival, compass, lifecycle, and map checks |
