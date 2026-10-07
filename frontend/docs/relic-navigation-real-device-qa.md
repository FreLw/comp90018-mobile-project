# Relic Navigation: Android real-device QA

Run the Debug app from Android Studio on a physical Android phone. Record device,
Android version, app revision, font/display scale, GPS accuracy, and results below.
These checks are pending manual execution; JVM tests/builds do not verify sensors or maps on a device.
Use an undiscovered campus relic and a separate discovered relic. Leave debug distance/heading
simulation off for the real GPS and compass checks. Debug sliders can supplement boundary tests;
changed distance values produce distinct synthetic fixes, repeating the same value does not.

## A. Treasure entry

- [ ] Open an undiscovered relic. Primary action says **Follow the Resonance**, with distance only when available.
- [ ] Tap it: Navigation opens for that relic, including when already nearby; no challenge starts automatically.
- [ ] Exit, then choose **View on map**: normal Map opens for inspection, without Navigation or a Hunt request.
- [ ] Open a discovered relic: existing lore/collection actions remain; no navigation/hunt primary action appears.

## B. Location permission and readiness

- [ ] Fresh install: enter Navigation before a usable fix; see position-acquisition guidance, no valid distance/direction or Begin Hunt.
- [ ] Grant precise location using the existing permission flow; wait for a valid fix. Distance, resonance and Guiding Thread become available.
- [ ] Deny/revoke location permission: see **Location access is required**; no actionable guidance or Begin Hunt.
- [ ] Disable location/provider or lose reception: see unavailable/acquiring/improving-signal text as applicable; any display fallback must not grant arrival.
- [ ] Restore permission/provider and obtain good fixes: guidance recovers; arrival requires fresh qualifying evidence.
- [ ] In Debug, enable distance simulation with denied GPS: synthetic guidance works. Disable it: actual readiness returns and evidence resets.
- [ ] With a test catalogue/fixture, leave the target unresolved while loading: normal app controls remain usable. After a settled missing target/error, navigation cancels; no hidden-bottom-bar dead end.
- [ ] Remove the active target from a test catalogue: Navigation exits, its overlays and sensor session are released. Do not alter production catalogue data for this check.

## C. Distance stages

Check displayed semantic labels with reliable position data; GPS drift may require repeated observations.

| Distance | Expected stage |
| --- | --- |
| >150 m (including 150.1) | DORMANT / Outside resonance |
| >80 m to 150 m inclusive | FAINT; exactly 150 is FAINT |
| >30 m to 80 m inclusive | DRAWN; exactly 80 is DRAWN |
| >10 m to 30 m inclusive | STRONG; exactly 30 is STRONG |
| <=10 m before usable evidence | STRONG; distance alone cannot confirm arrival |
| First reliable <=10 m fix | CONFIRMING |
| Second newer reliable <=10 m fix | ARRIVED |

- [ ] Check 150.1, 150, 100, 80.1, 80, 50, 30, 15 and 10 m with debug simulation as supplemental boundary checks.
- [ ] Resonance progress is 0 at >=150, 0.5 at 80, and 1 at <=10 m. Full progress alone never shows Begin Hunt.

## D. Arrival confirmation

- [ ] First actionable fix <=10 m, with finite nonnegative distance and accuracy <=10 m (or unavailable accuracy), shows **Confirming the resonance…**.
- [ ] Rotate/pan/recompose or repeat the same synthetic value/timestamp: it does not count as the second fix.
- [ ] A second newer qualifying fix confirms arrival; see **Resonance complete** and the one-time sweep/seal transition.
- [ ] Begin Hunt appears only after confirmed arrival and the transition finishes.
- [ ] Move to 12 m, then 15 m with reliable fixes: retain ARRIVED, full resonance and Begin Hunt.
- [ ] Move beyond 15 m: arrival and Begin Hunt disappear. Re-entry requires two new qualifying <=10 m fixes.
- [ ] Lose actionability or receive inaccurate evidence while confirming/arrived: evidence clears immediately; recovery requires two fresh fixes.
- [ ] Switch real/debug distance sources, target, or leave/re-enter: previous evidence does not carry over.

## E. Compass

- [ ] With a known target bearing, rotate left/right: hints show the correct shortest turn and degree error.
- [ ] Face the target: **Trail aligned** includes +/-12 degrees. Just outside that tolerance, the corresponding turn hint returns.
- [ ] Cross 0/360 degrees both ways: 350 to 10 gives right 20 degrees; 10 to 350 gives left 20 degrees, without a long-way jump.
- [ ] Repeat at multiple campus orientations; compare with a geographic-north reference when practical. Production magnetic heading includes local declination; simulated heading is already true north.
- [ ] During unavailable/warming/unreliable heading, show **Finding direction…**, rather than treating missing data as valid north.
- [ ] Heading simulation overrides actual heading; disabling it restores real heading. Alignment alone never enables Hunt.

## F. Display and lifecycle

- [ ] Portrait: map, header, controls and card are usable.
- [ ] Rotate the display if the app/device supports it: direction remains consistent with the display, controls stay accessible.
- [ ] Home/background/resume, and screen lock/unlock: orientation stops while inactive and restarts on resume with fresh output.
- [ ] Repeat pause/resume several times; use Android Studio profiler/debugger to verify no duplicate listener registrations or retained Activity/View from the orientation session.
- [ ] Exit/re-enter Navigation for the same and different relics: each starts fresh evidence/animations; no old target/thread or leaked session remains.

## G. Guiding Thread and camera

- [ ] Observe >150 m, faint, drawn and strong ranges: thread emphasis/glints follow resonance without thick bands or expanding pulses.
- [ ] Compare aligned/misaligned headings: motion is fastest when aligned and slower away from the target.
- [ ] Pan manually: camera stays where moved; glint animation does not recenter it or produce jitter.
- [ ] Tap Recenter: expected target-focused camera behavior resumes; a subsequent manual pan still works.
- [ ] Observe a stationary device for several animation cycles: no marker reconstruction/flicker or camera movement solely from glints.
- [ ] Try zoom in/out: standalone Navigation intentionally retains disabled zoom gestures. For multiple SDK zoom levels, use a debug-only map test harness/debugger; do not enable production gestures for QA.
- [ ] At supported test zoom levels, check line continuity and distinct, restrained diamond glints.
- [ ] Exit Navigation, remove target, or make position non-actionable: base thread, gold line and glints disappear. Normal Map/Hunt overlays remain intact.

## H. Responsive UI

- [ ] Narrow phone with a long relic/location name and a three-digit distance: text wraps; Stop retains a usable touch target.
- [ ] Larger Android font/display scale: card can scroll within its height limit; header and controls do not overflow horizontally.
- [ ] Check **Turn right · 128°**, Rosette, CONFIRMING and ARRIVED text: all remain readable.
- [ ] Begin Hunt occupies its own full-width row with a normal touch target; scroll to it when necessary.
- [ ] Active navigation text uses Resonance/Guiding Thread; no Energy/Battery labels appear.
- [ ] Drag Debug simulation controls out of the card's way if they overlap during testing.

## I. System Back

- [ ] Android system Back from Navigation returns to the prior normal app destination, without closing the Activity.
- [ ] Visible header Back and Stop do the same; bottom navigation returns.
- [ ] Re-enter after each exit: no retained arrival readiness or navigation overlay remains.

## J. Hunt handoff

- [ ] Arrive, wait for transition, then tap Begin Hunt.
- [ ] With Android Studio breakpoints, verify `onStartHunting(relic.id)` -> AppShell request ID + Map destination -> MapScreen consumption -> `openHunt()` -> `huntEntry()`.
- [ ] Verify existing Compass/Quiz and applicable Team/Room challenge routing still works; Navigation does not start competing challenge logic.
- [ ] Confirm actual challenge conditions still control completion/collection, not resonance, alignment or navigation arrival.
- [ ] Normal Map camera, teammate markers, fragments and team-room hunt triggering behave as before.

Record pass/fail and logs/screenshots for each unchecked item. A successful build is not a completed device QA run.

## Visual-feedback follow-up (pending device execution)

- [ ] Compare 120, 80, 32 and 17 m at the same map position/zoom: Rosette arcs are about 21%, 50%, 84% and 95%, with 2/5/6/7 active gold petals respectively.
- [ ] Check 150/80/30/10 m: the dormant instrument/thread is quiet; increasing resonance visibly strengthens the gold arc/line and diamonds.
- [ ] Hold simulated distance and heading constant for at least 10 seconds: all three diamonds continuously travel from relic to explorer without requiring a slider change, tap or GPS update.
- [ ] At 17 m, turn away: gold remains strong while glints slow down. At 120 m, align: gold remains faint while glints move faster.
- [ ] Pan the map and observe stationary-device animation: no camera jitter/recentering, relic-marker flicker or Hunt overlay reconstruction. Recenter still works explicitly.
- [ ] Repeat leave/re-enter, background/resume, target loss and permission loss: no orphan thread/glints; guidance and animation recover when available.
- [ ] Confirm existing CONFIRMING/ARRIVED sweep and Begin Hunt behavior, without any direction-based arrival gate.

## Split-panel follow-up (pending device execution)

- [ ] Below the understated app bar, target panel contains only relic name, location, distance and Stop. Normal height is approximately 84 dp; long names wrap to two lines.
- [ ] Bottom panel contains only the 64 dp Rosette, resonance label, stage/readiness and direction. Normal height is approximately 88–115 dp, with no repeated target information.
- [ ] CONFIRMING has no Begin Hunt; confirmed arrival reveals a separate full-width Begin Hunt row after the existing transition.
- [ ] Test a narrow phone, a long treasure/location name and increased font scale. Both panels can scroll if screen-height limits are reached; Stop and Begin Hunt remain reachable.
- [ ] Debug controls initially sit below the measured top panels, including after font-size changes. Drag them if necessary on a short display.
- [ ] Pan/Recenter and compare all distance stages: more of the central map is visible; Rosette strength, continuous glints and Hunt handoff remain unchanged.
