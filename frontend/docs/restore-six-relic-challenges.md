# Six treasure challenges restoration

Branch: `jiayi/fix/restore-six-relic-challenges`. Started from clean, fetched and fast-forward-checked `main` at `41d52ba`. Existing commits are preserved. No push, merge, rebase or PR was performed.

## Root cause

Map buttons, treasure cards and team-owner arrival routed configured treasures into `TreasureCompassGate`, which presented the same Trail / Bearing / Balance conditions. Detail routing also excluded the local hunt types instead of consistently entering `TreasureChallengeRoute`. The treasure-specific engine, evaluator and screen already existed but were bypassed. System Garden had separately inherited Old Quad's excavation configuration, and the camera panel was limited to Union Lawn. Debug builds also forced simulated sensors whenever the challenge route was used.

## Corrected behavior

| Treasure | Requirements | GPS radius | Hold |
| --- | --- | --- | --- |
| Union Lawn | Valid GPS, configured compass alignment, successful CameraX photo | 25m | Existing 800ms alignment |
| Wilson Hall | Valid GPS, heading, stationary/stable device, rotational stillness | 20m | 3s continuously |
| Old Quad | Valid GPS, horizontal, stationary/stable device, rotational stillness; no heading | 18m | 3s continuously |
| South Lawn (solo) | Valid GPS, configured viewing direction and stability conditions | 15m | Existing 1.2s |
| System Garden | Valid GPS and successful CameraX photo; no heading or motion/level requirements | 22m | No sensor hold |
| Grainger Museum | Valid GPS, microphone access, sound at/above configured -30dBFS threshold; no heading or stability | 18m | Existing 1s continuously |

All normal entry points use the configured challenge route; pending room members retain their quiz. South Lawn's existing shared fragment hunt and claim workflow still takes precedence in team mode. Treasure identity must match its configured type. Missing/disabled/incomplete configurations show an unavailable screen, and legacy Garden excavation configuration is rejected rather than bypassing the photo requirement.

`UnionPhotoPanel` now serves both photo tasks. Opening a camera, granting permission, clicking the shutter, empty/malformed URI results, errors and unsolicited callbacks cannot complete a challenge. Completion requires an active capture and a nonempty local `content://` URI from the successful CameraX callback. Photos remain in the device MediaStore; no uploads or new Firebase collections were introduced. Debug simulation cannot manufacture a completed photo. Existing readiness latching through shutter movement is preserved.

Each task displays its own instructions and evaluator conditions. Camera controls appear for photo tasks and microphone permission controls for the sound task. After a confirmed discovery save, photo tasks open the normal treasure reveal directly. The reusable historical-image implementation remains intact but is not required.

Physical challenge completion and discovery persistence remain separate. Save failure permits retry and blocks reveal. Success haptics still use the existing confirmed-save/confirmed-team-claim adapters; the independent 20m proximity haptic threshold is unchanged.

## Firestore handoff — manual update required

Do not run an import or write automatically. A teammate should update only these fields in `treasures/system_garden_glasshouse.challenge`, using the corrected `database/treasures.json` as reference:

```json
{
  "photoActionRequired": true,
  "requiresStationary": false,
  "requiresStability": false,
  "requiresRotationStill": false,
  "requiresHorizontal": false,
  "requiredHeadingDegrees": null,
  "holdDurationMs": 0,
  "requiresSound": false
}
```

Keep its type `SYSTEM_GARDEN_GLASSHOUSE`, ID, enabled status, GPS coordinate and 22m radius. Preserve all other treasure radii and configured headings. Audit that Union/Wilson/South have non-null required headings and that all six challenge maps match their intended rules. This change did not access or modify the live catalogue; until Garden is migrated, its old live configuration will be blocked in this app version.

## Validation

- `./gradlew :app:testDebugUnitTest`: passed, 212 tests, zero failures/errors (including the final Garden parser migration test).
- `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`: passed.
- Relevant `:app:connectedDebugAndroidTest` selection: passed, 7 tests on `Medium_Phone` Android 17 (API 37) emulator: `TreasureChallengeUiTest`, `HapticSessionBindingTest`, `RoomClaimHapticUiTest`.
- `git diff --check`: passed. Static inspection of `database/treasures.json` confirms unchanged GPS radii and corrected Garden photo flags.
- An intermediate instrumentation build caught an invalid Compose assertion import; corrected in `a37778c` and all subsequent builds/tests passed.
- The headless emulator started for this verification was closed after the test run.

Added coverage includes six distinct routes and radii, malformed/mismatched configuration, Garden photo-only rules, heading gating, capture error/URI rejection/retry, microphone unavailability and interrupted sound hold, member quiz compatibility, confirmed-save haptics and challenge-specific Compose UI. The full unit suite includes the existing GPS, sensor, fragment hunt, room claim and haptic regressions.

Physical CameraX capture, real-world GPS/compass calibration, actual acoustic threshold and physical vibration have not been verified. Emulator UI tests use fake callbacks for persistence and do not prove live multi-device Firestore behavior.

## Teammate review and integration risks

- Review and apply the manual Garden Firestore update before using the corrected challenge in a live catalogue.
- Field-calibrate GPS, magnetic headings, stability and sound threshold/duration; existing coordinates/headings remain pending calibration.
- Test real-device CameraX capture and permission denial/retry, microphone denial/grant, save failure/retry, treasure reveal and vibration timing.
- Review two-device owner challenge/member quiz completion and South Lawn fragment hunting/final claims. The preserved multiplayer South Lawn workflow uses fragments instead of the solo viewing-angle task.
- Review stricter configuration validation: previously weakened or heading-less challenge documents now remain unavailable.

## Modified files

- `database/treasures.json`
- `frontend/app/src/androidTest/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeUiTest.kt`
- `frontend/app/src/debug/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationFactory.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/contextengine/challenge/ChallengeConfigValidation.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/contextengine/challenge/RelicChallengeConfigs.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/data/treasure/FirebaseTreasureRepository.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/PostChallengeRevealSession.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/TreasureChallengeRouting.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeScreen.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeViewModel.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/contextengine/challenge/ChallengeRuleEvaluatorTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/data/treasure/ChallengeCalibrationParsingTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/map/PostChallengeRevealSessionTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/map/TreasureChallengeRoutingTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeHapticSaveRegressionTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeViewModelTest.kt`
- `frontend/docs/restore-six-relic-challenges.md` (this report)

## Commits

```text
9b3a7ae fix(challenges): make System Garden a GPS-gated photo task
1e865a0 fix(map): route hunt entries through treasure-specific challenges
b03565f fix(catalogue): reject incomplete treasure challenge rules
6b2bdff fix(camera): reuse photo panel and require successful local capture
463b5df feat(challenges): display task-specific instructions and sensor conditions
9e7be4f test(challenges): cover Garden photo rules and reject absent headings
1b25144 fix(routing): validate treasure identity and preserve member quiz selection
5b32766 test(challenges): cover six routes capture retries and microphone interruption
8bc7481 test(integration): verify challenge UI and confirmed-save haptics
a37778c fix(tests): use Compose node member assertion for absent controls
49b5aae fix(reveal): open treasure directly after simplified photo discovery
```

The final catalogue-test and documentation milestone is the subsequent commit containing this report; use `git log 41d52ba..HEAD` for its hash.
