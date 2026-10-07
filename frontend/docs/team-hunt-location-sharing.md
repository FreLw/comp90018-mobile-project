# Team hunt location sharing

Location sharing runs only while an authenticated member's room reports an active
`hunting` task with a non-empty `huntSessionId` and at least two members. Choosing
a task does not enable it; the owner must successfully start the hunt first.

Each foreground explorer publishes their current, permission-granted GPS fix
every 10 seconds to `teamRooms/{roomId}/locations/{uid}`. Coordinates used by the
debug distance slider and campus display fallback are never published. The app
stops sharing and clears its position when backgrounded, when GPS becomes
unusable, when leaving the room, or when the hunt ends.

Markers require server-confirmed heartbeats for both the viewer and the owner.
Only current members in the same hunt session appear. Both the heartbeat and
GPS observation must be no older than 30 seconds. Unexpected disconnection or
process termination therefore hides a peer within approximately 31 seconds
(30-second expiry plus the one-second refresh cadence). This is heartbeat-based
presence, rather than an instantaneous network-disconnect signal.

The existing local marker represents the viewer. Shared host markers are orange;
shared teammate markers are blue. Tap a shared marker to see the member name and
role. They appear in both main-map perspectives and the South Lawn fragment and
claim-waiting maps.

## Firebase setup

Deploy the updated rules to the project used by `app/google-services.json`:

```powershell
# From the repository root; this changes the live project's Firestore rules.
firebase deploy --only firestore:rules --project mobile-melbourne
```

No additional Firestore indexes, backend functions, or background-location
permission are required. Rules restrict reads to current hunt participants,
require the current session, validate coordinate ranges and timestamps, and
allow each explorer to delete their own position after leaving.

## Verification

Automated checks:

```powershell
# From frontend
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
# From tests/firestore-rules
npm run test:emulator
```

Two-device acceptance checks (use separate accounts with location permission):

1. Join the same room and select a treasure. No shared markers appear yet.
2. Have the owner start the hunt. Open Map on both devices and wait for the
   first server-confirmed heartbeat. Each device shows the other explorer.
3. Move one device. Its shared marker updates approximately every 10 seconds.
4. Check God view, Hunt view, and the South Lawn fragment map.
5. Background either app. The other app hides the peer after cleanup reaches the
   server. Kill its process or disable networking instead: the marker expires
   within approximately 31 seconds of its last heartbeat.
6. Resume/reconnect with a fresh GPS fix. The marker returns automatically.
7. Terminate the hunt, leave/dismiss the room, or remove a member. Shared markers
   disappear. Start a new hunt: old session positions do not reappear.
8. Deny/revoke location permission or let the GPS fix expire. That explorer's
   shared position disappears; display-only fallback locations are not shared.

Physical two-device acceptance checks must be performed separately from the
unit tests and local Firebase Emulator tests.
