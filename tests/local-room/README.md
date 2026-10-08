# Local Room Acceptance Test

This uses only the local demo Firebase project. It does not deploy rules or change production data.
Node.js and the existing Firebase CLI/JDK prerequisites are required.

From `tests/firestore-rules`, keep this command running in a terminal:

```sh
npx --yes firebase-tools@15.32.1 emulators:start --only auth,firestore,storage --project demo-lost-treasures --config ../../firebase.json
```

In another terminal at the repository root:

```sh
node tests/local-room/seed.mjs
```

In `frontend/local.properties`, temporarily add:

```properties
USE_FIREBASE_EMULATORS=true
```

This project explicitly reads the flag from `local.properties`. Alternatively, run:

```sh
./gradlew :app:installDebug -PUSE_FIREBASE_EMULATORS=true
```

Before launching the app, forward the Firebase ports (repeat after restarting the device):

```sh
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
adb reverse tcp:9199 tcp:9199
```

Use an Android emulator on this computer. The Debug flag connects Firebase to device
loopback through these forwards; it is false by default and always false for release builds.
Google Maps still requires
the existing Maps key and an Internet connection.

Sign in as `player1@example.com` (password `RoomTest123!`), open Rooms and start the seeded
South Lawn hunt. Check the fragment ownership labels. Debug collection can collect only
your assigned or shared fragments. Sign out and repeat with players 2, 3 and 4 on the
same emulator; you do not need four emulators at once.

After players 1 and 2 collect, the room must still be incomplete. After players 3 and 4
collect, everyone may claim. The room must remain active until all four have claimed.
Another player's fragment must have a disabled collection button, regardless of distance.

Repeat with three players by terminating the hunt, removing one member, and starting again.
The fourth fragment is shared. For two players, each receives two fragments.
The owner must terminate an assigned fragment hunt before any membership changes.

Restarting the local emulators discards local data unless explicitly exported. Rerunning
the seed script resets the test room, so do not run it during an acceptance test.
Remove the local Gradle flag and rebuild normally when returning to production Firebase.
