# Firestore security rules tests

These tests exercise `firestore.rules` against the local Firestore Emulator.
They use the demo project ID `demo-lost-treasures`, so the test run cannot
access the production Firebase project.

From this directory, install dependencies and run the suite:

```bash
npm install
npm run test:emulator
```

The suite verifies profile ownership, catalogue read/write access, treasure
collection ownership and catalogue references, team-room message access, and
shared hunt location access, ownership, session isolation, and timestamp validation.
Expected permission-denied log entries are produced by negative assertions and
do not indicate test failures.

Android Compose instrumentation tests live under
`frontend/app/src/androidTest`. Run them with a connected emulator or device:

```bash
cd ../../frontend
./gradlew connectedDebugAndroidTest
```
