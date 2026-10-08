# Lost Treasures

Lost Treasures is an Android campus treasure-hunt app built for the University
of Melbourne. Players explore real campus locations, follow map and compass
guidance, complete context-aware sensor challenges, collect historical relics,
and cooperate through team rooms. Social features support profiles, friends,
direct messages, team chat, and sharing collected treasure stickers.

## Implemented features

### Campus treasure hunt

- A Google Maps view of the treasure catalogue and the user's current location.
- Distance, proximity, and target-bearing calculations for the selected relic.
- Guided hunts that progress from map discovery to compass navigation and a
  location-specific challenge.
- Six campus relic challenges at Union Lawn, Wilson Hall, Old Quad, South Lawn,
  System Garden, and the Grainger Museum.
- Historical stories, artwork, discovery state, and a personal treasure
  collection stored in Firestore.
- Cooperative team hunts, including the four-fragment South Lawn hunt.

### Sensors and context engine

- Fused Android location updates with runtime permission handling, accuracy and
  staleness filtering, proximity hysteresis, and stale-signal recovery.
- Device heading, target direction, rotation, motion, and stability processing.
- Camera and microphone input for challenge-specific interactions.
- A deterministic context engine that combines location and sensor states to
  evaluate challenge conditions and produce user instructions.
- Debug-only sensor simulation and calibration tools for repeatable development.

Location-based actions require a current usable reading. Denied permissions,
stale readings, and fallback coordinates cannot unlock a hunt.

### Accounts and collaboration

- Email/password registration, sign-in, and sign-out with Firebase Authentication.
- Explorer profiles with username, gender, bio, and an optional profile photo.
- Exact username search, friend requests, friend management, and profiles.
- Real-time direct messages, including image and treasure-sticker sharing.
- Team-room creation and joining, real-time team chat, member details, and
  shared hunt progress.

## Challenge design

Each relic has a Firestore-backed challenge configuration. Depending on the
location, the context engine can require combinations of these conditions:

| Input | Example use |
| --- | --- |
| Location | Verify that the player is inside the relic's configured radius |
| Direction | Guide the player and check alignment with a target bearing |
| Motion and stability | Require the player to stop, hold still, or keep the phone level |
| Rotation and attitude | Check a viewing angle or observation position |
| Camera | Capture the Union Lawn challenge photo |
| Microphone | Detect the sound required by the Grainger Museum challenge |

The six catalogue configurations are included in
[`database/treasures.json`](database/treasures.json). Their calibration status
is currently `pending`; thresholds and bearings must be validated on physical
devices at the corresponding campus locations before final demonstration.

## Technology

| Area | Implementation |
| --- | --- |
| Android client | Kotlin, Jetpack Compose, Material 3 |
| Architecture | Feature-oriented MVVM with repository interfaces |
| Build | Gradle 9.7.1, Android Gradle Plugin 9.3.2, JDK 21 |
| Maps and location | Google Maps SDK for Android, Google Play services location |
| Device input | Android location and motion sensors, CameraX, microphone |
| Authentication | Firebase Authentication |
| Data and live updates | Cloud Firestore snapshot listeners |
| Images | Firebase Storage |
| Access control | Firestore and Storage Security Rules |

The app uses `minSdk 26`, `targetSdk 37`, and application ID
`com.comp90018.app`.

## Architecture

Lost Treasures is a single-module Compose app with this primary dependency
direction:

```text
Compose Screen -> ViewModel -> Repository -> Firebase adapter/service
```

ViewModels expose immutable UI state and own real-time subscription lifecycles.
Firebase calls stay behind repository implementations, and Android hardware
APIs are wrapped in testable sensor classes. The `contextengine` package
combines their structured outputs and evaluates challenge rules independently
of the UI.

| Package | Responsibility |
| --- | --- |
| `features/` | Auth, chat, friends, map, profile, rooms, treasure, and challenge UI/ViewModels |
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
database/                 Shared treasure catalogue and import utility
firebase/                 Firebase rules and indexes
  firestore.rules         Firestore authorization and validation rules
  firestore.indexes.json  Firestore indexes
  storage.rules           Firebase Storage rules
docs/                     Project documents
  REQUIREMENTS.md         Requirements and acceptance criteria
  Mobile A1.pdf           Assignment specification
tests/                    Firestore security rules tests
firebase.json             Firebase deployment and emulator configuration
.firebaserc                Firebase project selection
.gitignore                Git ignore rules
```

## Local setup

### Prerequisites

- Android Studio with JDK 21 and Android SDK Platform 37.
- An Android API 26+ emulator or physical device with internet access.
- A Firebase project with the Firebase CLI installed.
- A Google Cloud project with **Maps SDK for Android** enabled.
- Node.js if the treasure catalogue needs to be imported.

### Firebase

1. Register an Android app with application ID `com.comp90018.app`.
2. Download `google-services.json` and place it at
   `frontend/app/google-services.json`. This local file must not be committed.
3. Enable Email/Password in Firebase Authentication.
4. Create the default Cloud Firestore database and enable Firebase Storage.
5. From the repository root, deploy the rules and indexes:

   ```bash
   firebase deploy --only firestore,storage
   ```

### Google Maps

Store the Maps API key outside version control. The recommended location is the
user-level Gradle properties file:

```properties
# ~/.gradle/gradle.properties
MAPS_API_KEY=your_google_maps_api_key
```

Alternatively, copy `frontend/local.properties.sample` to
`frontend/local.properties` and set `MAPS_API_KEY` there. For an Android-
restricted key, configure application ID `com.comp90018.app` and the SHA-1
fingerprint of the signing certificate used to build the APK.

### Treasure catalogue

The app reads relic definitions from the Firestore `treasures` collection. To
import the checked-in six-relic catalogue using an authenticated Firebase CLI:

```bash
cd database
npm install
node import-treasures.mjs --firebase-cli --target YOUR_FIREBASE_PROJECT_ID
```

The importer also accepts a service account with `--key`; see the usage check in
[`database/import-treasures.mjs`](database/import-treasures.mjs). Never commit a
service-account file.

## Build and test

Open `frontend` in Android Studio, wait for Gradle sync, select a device, and
choose **Run**. From a terminal:

```bash
cd frontend
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

On Windows, replace `./gradlew` with `gradlew.bat`.

Unit tests cover pure location calculations, invalid/stale location policy,
direction processing, motion and sound processing, treasure parsing, map and
hunt state transitions, challenge rules, and ViewModel/repository behaviour.
Camera, microphone, compass, GPS, permission, poor-signal, and on-site threshold
behaviour should additionally be checked on physical Android devices.

## Main user flow

1. Register or sign in and complete the explorer profile.
2. Open **Map**, select a relic, and start its hunt.
3. Follow distance and compass guidance until a reliable location reading is
   inside the configured radius.
4. Complete the sensor challenge and reveal the relic's history and artwork.
5. Review collected and undiscovered relics in **Treasure**.
6. Use **Rooms** for team chat and a cooperative hunt, or **Friends** for direct
   chat and treasure-sticker sharing.

## Firestore data model

| Path | Purpose |
| --- | --- |
| `users/{uid}` | Explorer profile and settings |
| `users/{uid}/friends/{friendUid}` | Accepted friendship and unread state |
| `users/{uid}/treasureCollection/{treasureId}` | The user's collected relics |
| `friendRequests/{requestId}` | Incoming and outgoing friend requests |
| `rooms/{roomId}/messages/{messageId}` | Private direct-chat messages |
| `teamMemberships/{uid}` | The user's current team-room membership |
| `teamRooms/{roomId}` | Team membership and cooperative hunt state |
| `teamRooms/{roomId}/messages/{messageId}` | Team-room messages |
| `treasures/{treasureId}` | Shared relic metadata and challenge configuration |

## Known limitations

- Challenge thresholds and bearings are map-assisted development values and
  still require on-site physical-device calibration.
- Hardware behaviour varies by device, sensor quality, magnetic interference,
  GPS conditions, and Android permission choice.
- Core online features require Firebase connectivity; map rendering requires a
  valid Maps API key and network access.
- Debug simulation tools are excluded from release builds and are not evidence
  of physical-device validation.

See [`REQUIREMENTS.md`](docs/REQUIREMENTS.md) for detailed acceptance criteria and
[`frontend/README.md`](frontend/README.md) for the Android client quick start.
