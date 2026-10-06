# Treasure proximity haptics

Implemented from clean, latest main `bf594a2` on `jiayi/feature/proximity-haptics`.

## Behavior and architecture

Raw LocationOutput → independent proximity policy → nearest eligible target → session event controller → safe hardware driver → Android Vibrator.

Light: one 100ms pulse, amplitude 60 when supported, at distance <=20m. This threshold is independent of hunt radii. Requires a precise, available, valid, non-mock fix with a valid coordinate, finite nonnegative accuracy <= the existing LocationConfig maximum (50m), and monotonic age <= the existing 10-second stale timeout. Rejects future/missing timestamps, missing accuracy, last-known/display fallbacks and map simulation. This is a feedback filter, not a change to GPS or challenge validation.

Only the active hunt/challenge target or nearest undiscovered candidate is evaluated. Nearby IDs remain remembered across map/detail navigation and Activity configuration changes. The session is Activity-retained and keyed by explorer. Rotation preserves it; sign-out permanently closes its controller and pending attempts. Re-entry creates a fresh session, and callbacks captured before logout remain inert. Ordinary heading/motion updates and opening/closing details do not own feedback triggers.

Success: 150ms at amplitude 220, 100ms silence, then 200ms at amplitude 255. Without amplitude control, use device-default amplitude. Solo paths wait for the existing collection save callback. Team individual tasks never emit success: final feedback waits for both collection save and the existing team claim transaction confirmation. Already discovered solo collections do not emit new unlock feedback. Solo success is deduplicated by discovery identity. Both Map and Rooms use one confirmed team-claim coordinator, deduplicating by room, observed hunt generation, user and treasure. A new observed team hunt can notify again for the same treasure, while proximity remains once per treasure per exploration session. Abandoning a Map challenge invalidates its feedback attempts without cancelling persistence or collection callbacks. Failed persistence/claim does not consume success eligibility, so a successful retry can notify. Disabled/background successes are consumed silently rather than replayed on resume.

Automatic treasure haptics remain disabled until settings load successfully and explicitly enable Haptics. Unrelated navigation feedback retains its existing preference behavior. Haptics settings are applied from AppShell. Only RESUMED foreground playback is allowed. Pause/stop, disposal and disabled settings cancel active vibration. Missing hardware and driver exceptions are safe no-ops. A monotonic 450ms success window suppresses proximity playback without consuming nearby eligibility; the next valid GPS update can notify after that window. No timer, queue or dependencies were added.

## Shared files changed and teammate review

| File (repository-relative) | Required change | Coordination |
|---|---|---|
| frontend/app/src/main/AndroidManifest.xml | Add VIBRATE permission | Inform Android integration owner |
| frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt | Explorer-keyed session ViewModel, preferences/lifecycle, map driver and team claim callback wiring | Review with shell/navigation owner |
| frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt | Replace old proximity feedback; remove detail and sensor-readiness vibration; decorate existing saves; await team claim confirmation | Review with map and challenge owners |
| frontend/app/src/main/kotlin/com/comp90018/app/features/rooms/TeamRoomChatViewModel.kt | Add confirmation method forwarding existing repository transaction result; preserve original no-argument API and busy guard | Review with team-hunt owner |
| frontend/app/src/main/kotlin/com/comp90018/app/sensors/location/AndroidLocationSensor.kt | Preserve mock provenance, including stale outputs; no filtering change | Review with location owner |
| frontend/app/src/main/kotlin/com/comp90018/app/features/map/LocationActionPolicy.kt | Preserve source mock metadata and flag simulated derived coordinates | Review with location/map owners |
| frontend/app/src/main/kotlin/com/comp90018/app/features/rooms/RoomsScreen.kt | Route claim button through shared coordinator; retain default no-argument API; expose header internally for UI regression test | Review with Rooms owner |
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
| TreasureHapticAttempt.kt | Identity token for an asynchronous feedback attempt |
| ConfirmedTeamClaimHaptics.kt | Shared confirmed Map/Rooms claims and observed hunt generations |
| TreasureHapticSessionBinding.kt | Production Compose settings and Activity lifecycle binding |

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

Final compatibility-fix verification on 2026-10-06:

```
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest
```

204 JVM tests, 0 failures/errors/skipped, across 37 suites. Debug APK and AndroidTest APK compiled successfully. All 8 instrumentation tests passed on the headless Medium_Phone Android emulator: existing authentication tests, the actual production lifecycle/settings binding, and the real Rooms claim button with a delayed repository substitute. Final combined Gradle invocation: BUILD SUCCESSFUL.

New JVM integration tests use the actual collection and team ViewModels and save/claim adapters with delayed repository substitutes. They cover failure/retry, Map/Rooms deduplication, abandoned and obsolete attempts, logout, rotation, repeat team hunts, and success/proximity ordering. LocationActionPolicy tests cover derived source metadata. These do not verify live Firebase transactions, physical GPS, full online map navigation, or vibration sensation. Physical-device testing remains pending. An early compilation attempt overlapped source edits; it was rerun successfully after editing finished. No failing assertions or build errors remain.

Generated APKs and reports are not committed. JVM results are under `app/build/test-results/testDebugUnitTest`; instrumentation results are under `app/build/outputs/androidTest-results/connected/debug`.

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

Review the shared AppShell/Map/Rooms claim wiring and lifecycle binding with navigation and multiplayer owners before merging. The existing no-argument claim API, Firebase repositories/schema and challenge rules are unchanged. Team completion identity uses observed room transitions because the schema has no durable hunt-run ID. If the subscription misses an entire reset and restart of the same treasure, feedback may be conservatively suppressed; a durable run ID would require a separate teammate/schema decision. Do not bypass production mock-location rejection to test emulator GPS. Physical-device intensity, real GPS and live Firebase/multiplayer end-to-end behavior remain unverified.

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

## Compatibility-fix commit evidence

The original 16 commits remain intact. Follow-up commits completed before this final regression/documentation milestone:

### 1c9ceeb fix: propagate mock location metadata through derived outputs

- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/LocationActionPolicy.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/sensors/location/AndroidLocationSensor.kt`

### f3eb438 test: verify derived location source metadata

- `frontend/app/src/test/kotlin/com/comp90018/app/features/map/LocationActionPolicyTest.kt`

### 96e1ddc fix: suppress automatic haptics until settings are loaded

- `frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModel.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModelTest.kt`

### cb83ce9 fix: reset haptic session on sign-out

- `frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticController.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModel.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModelTest.kt`

### 16d2504 feat: invalidate obsolete asynchronous haptic attempts

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticAttempt.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticController.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSave.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`

### 9341ac8 test: cover delayed save and session invalidation

- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/HapticCollectionIntegrationTest.kt`

### 6de8a10 feat: unify confirmed team claim haptic events

- `frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/ConfirmedTeamClaimHaptics.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticController.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSave.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModel.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/rooms/RoomsScreen.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TreasureHapticSaveTest.kt`

### 696a479 test: cover map and rooms claim entry points and activity lifecycle

- `frontend/app/src/androidTest/kotlin/com/comp90018/app/features/haptics/HapticSessionBindingTest.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/AppShell.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionBinding.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/haptics/TeamClaimHapticIntegrationTest.kt`

### 26ad66d fix: prioritize success vibration over proximity feedback

- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticController.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticPattern.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModel.kt`

The final milestone adds repeated-hunt and priority regression tests, the Rooms claim-button instrumentation test, and this updated evidence. Its hash is available in Git history.
