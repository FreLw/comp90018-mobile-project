# Treasure proximity haptics

Implemented from clean, latest main `bf594a2` on `jiayi/feature/proximity-haptics`.

## Behavior and architecture

Raw LocationOutput → independent proximity policy → nearest eligible target → session event controller → safe hardware driver → Android Vibrator.

Light: one 100ms pulse, amplitude 60 when supported, at distance <=20m. This threshold is independent of hunt radii. Requires a precise, available, valid, non-mock fix with a valid coordinate, finite nonnegative accuracy <= the existing LocationConfig maximum (50m), and monotonic age <= the existing 10-second stale timeout. Rejects future/missing timestamps, missing accuracy, last-known/display fallbacks and map simulation. This is a feedback filter, not a change to GPS or challenge validation.

Only the active hunt/challenge target or nearest undiscovered candidate is evaluated. Nearby IDs remain remembered across map/detail navigation and Activity configuration changes. The session is Activity-retained and keyed by explorer; restarting the Activity/process creates a new session. Ordinary heading/motion updates and opening/closing details do not own feedback triggers.

Success: 150ms at amplitude 220, 100ms silence, then 200ms at amplitude 255. Without amplitude control, use device-default amplitude. Solo paths wait for the existing collection save callback. Team individual tasks never emit success: final feedback waits for both collection save and the existing team claim transaction confirmation. Already discovered solo collections do not emit new unlock feedback. Success is deduplicated per treasure within the session. Failed persistence/claim does not consume success eligibility, so a successful retry can notify. Disabled/background successes are consumed silently rather than replayed on resume.

Haptics settings are applied from AppShell. Only RESUMED foreground playback is allowed. Pause/stop, disposal and disabled settings cancel active vibration. Missing hardware and driver exceptions are safe no-ops. No dependencies were added.

## Shared files changed and teammate review

| File (repository-relative) | Required change | Coordination |
|---|---|---|
| frontend/app/src/main/AndroidManifest.xml | Add VIBRATE permission | Inform Android integration owner |
| frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt | Explorer-keyed session ViewModel, preferences/lifecycle, map driver and team claim callback wiring | Review with shell/navigation owner |
| frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt | Replace old proximity feedback; remove detail and sensor-readiness vibration; decorate existing saves; await team claim confirmation | Review with map and challenge owners |
| frontend/app/src/main/kotlin/com/comp90018/app/features/rooms/TeamRoomChatViewModel.kt | Add confirmation method forwarding existing repository transaction result; preserve original no-argument API and busy guard | Review with team-hunt owner |
| frontend/app/src/main/kotlin/com/comp90018/app/sensors/location/AndroidLocationSensor.kt | Forward Android mock-provider flag only | Review with location owner |
| frontend/app/src/main/kotlin/com/comp90018/app/sensors/location/LocationOutput.kt | Add defaulted isMock metadata | Inform location-output consumers |

Context Engine, challenge criteria, compass/motion algorithms, hunt radii, GPS permission/accuracy algorithms, Firebase schemas/services and reveal animations are unchanged. Collection UI callbacks retain their original timing. Team claim errors still reside in existing Room state. RoomsScreen's original no-argument claim API is preserved. Unrelated bottom-navigation haptics are preserved.

## New files and responsibilities

All production files below are in `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/`:

| File | Responsibility |
|---|---|
| TreasureHapticProximity.kt | Pure 20m boundary and raw fix validation |
| TreasureHapticEvent.kt | Typed Nearby/Unlocked events and driver interface |
| TreasureHapticController.kt | Per-treasure session deduplication and playback gates |
| TreasureHapticTarget.kt | Pure active/nearest eligible treasure selection |
| TreasureHapticPattern.kt | Hardware-independent light/success waveform definitions |
| SafeTreasureHapticDriver.kt | Testable unavailable-hardware and exception handling |
| AndroidTreasureHapticDriver.kt | Android Vibrator adapter |
| TreasureHapticSessionViewModel.kt | Activity-retained driver/controller and cancellation |
| TreasureHapticSave.kt | Decorate existing save/claim callbacks without changing persistence |

Tests in `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/`:

| File | Coverage |
|---|---|
| TreasureHapticProximityTest.kt | 20.1/20/19.9m; malformed, stale, inaccurate, mock, missing fixes |
| TreasureHapticControllerTest.kt | Oscillation, treasure switching, deduplication, failure/readiness |
| TreasureHapticTargetTest.kt | Active vs nearest, discovered IDs, missing target, display fallback |
| TreasureHapticSaveTest.kt | Async save, retry, cancellation, duplicate callbacks, team claim failure/success |
| TreasureHapticLifecycleTest.kt | Settings, foreground/resume, session re-entry, waveform amplitudes |
| SafeTreasureHapticDriverTest.kt | Missing hardware, default amplitudes, cancellation, platform exceptions |

`frontend/app/src/test/kotlin/com/comp90018/app/features/rooms/TeamHuntClaimConfirmationTest.kt` verifies the existing Room busy guard, transaction arguments, callback forwarding and error-state updates. This document is `frontend/docs/proximity-haptics.md`.

## Executed validation

On 2026-10-06, from frontend:

```
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Baseline unit tests passed before implementation. Final full suite: 186 tests, 0 failures, 0 errors, 0 skipped; 16 new tests. Final debug build: BUILD SUCCESSFUL. APK: frontend/app/build/outputs/apk/debug/app-debug.apk (generated, not committed).

Initial sandbox cache access required approved execution; one initial invocation used the wrong working-directory path and was corrected. No failing test assertions or build errors remained. JVM tests simulate lifecycle gates and hardware boundaries; they do not prove actual Compose navigation, device vibration strength or physical GPS behavior. No physical-device or instrumentation test was executed.

## Physical-device checklist (not executed)

- Enable Haptics, allow precise location, move from >20m to <=20m near an undiscovered target: one light pulse.
- Stay inside, oscillate across the boundary, reopen details, navigate away/back and rotate Activity: no duplicate for that treasure.
- Switch to another eligible treasure: one pulse for the new treasure.
- Disable Haptics: no new proximity/success pulse; re-enable and test a new target.
- Use stale/inaccurate/missing/mock location and debug display simulation: no proximity pulse.
- Start Hunt or satisfy only part of a challenge: no success pulse.
- Complete a solo challenge: one distinct double pulse after save succeeds, with existing reveal and collection behavior.
- Fail/cancel saving and retry: no failure/cancellation pulse, one pulse after successful retry.
- In a two-person hunt, finish only one person's task: no success pulse. Finish all requirements, save collection and confirm claim: one double pulse. Denied/failed claim: no success pulse.
- Background during a save/claim, then resume: no queued success feedback; verify stale fix cannot produce proximity feedback.
- Verify no-hardware and no-amplitude-control devices remain stable; compare the light/strong sensations on real hardware.
- Confirm ordinary compass/motion updates, map/detail navigation, challenge criteria, animations, collection updates and Room workflows still behave as before.

## Integration concerns

Review the additional team callback signature at MapScreen with teammates before merging; AppShell is updated and RoomsScreen's original ViewModel API remains compatible. The controller deduplicates success by treasure ID for the session, including repeated team hunts of that treasure. Team claim completion initiated exclusively from RoomsScreen keeps existing behavior; this feature's success integration is the map treasure-hunting flow. Haptic intensity varies by device; manually calibrate if necessary. Session termination is Activity/process lifetime rather than a persisted lifetime across application restarts.

## Local commit evidence

The following chronological commits include modified files for contribution review. Documentation's own commit is available in `git log --reverse bf594a2..HEAD`.

### 382bfc9 feat: define independent 20m haptic proximity policy

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticProximity.kt`

### 8d6f8ad feat: introduce typed treasure haptic events

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticEvent.kt`

### 81ac529 feat: deduplicate treasure haptics within exploration sessions

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticController.kt`

### 91dc721 test: cover 20m boundaries and invalid GPS readings

- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticProximityTest.kt`

### c364dde test: verify treasure switching and success event deduplication

- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticControllerTest.kt`

### d1a8a20 feat: implement supported light and success vibration patterns

- `frontend/app/src/main/AndroidManifest.xml`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/AndroidTreasureHapticDriver.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticPattern.kt`

### 7197bd2 fix: exclude mock GPS from real proximity haptic events

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticProximity.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/sensors/location/AndroidLocationSensor.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/sensors/location/LocationOutput.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticProximityTest.kt`

### 1f3e3cc feat: select active or nearest eligible haptic target

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticTarget.kt`

### f063091 test: verify haptic target eligibility and display fallback isolation

- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticTargetTest.kt`

### d57b265 feat: integrate session haptics with map GPS and foreground settings

- `frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModel.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`

### 675c1af fix: remove duplicate detail and sensor-readiness haptics

- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`

### 9288078 feat: emit success haptics only after discovery save confirmation

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSave.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`

### 4ea93ae test: cover asynchronous unlock failure retry and cancellation

- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticSaveTest.kt`

### c4a2607 test: verify haptic settings lifecycle and unsupported hardware

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/AndroidTreasureHapticDriver.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/SafeTreasureHapticDriver.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/SafeTreasureHapticDriverTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticLifecycleTest.kt`

### 24d9c9e fix: wait for confirmed team treasure claim before success haptics

- `frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSave.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/rooms/TeamRoomChatViewModel.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticSaveTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/rooms/TeamHuntClaimConfirmationTest.kt`
