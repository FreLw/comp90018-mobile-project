# Shared treasure database

Live Firestore snapshot: `treasures.json`, refreshed from `mobile-melbourne` / `(default)` on 2026-10-09 (Australia/Sydney). The export timestamp is stored in UTC as `exportedAt`; each document's cloud update time is recorded in `sourceUpdateTimes`.

Contains only the six allowlisted treasure documents. No credentials or user/chat/room data are included.

All fields were read from the six cloud documents without changing Firebase. All six `calibrationStatus` values remain `pending` for later real-device/on-site testing. This is a point-in-time snapshot, not automatic ongoing synchronization.

The current cloud documents do not contain `prototypeImageUrl`; local artwork is selected through `artworkKey`. Cloud field values are preserved in the snapshot rather than replaced with local estimates.

To preview the four challenge configuration fixes for the Android app's current project:

```powershell
python database/update-challenge-configs.py --project mobile-melbourne
```

To apply those changes after reviewing the preview:

```powershell
python database/update-challenge-configs.py --project mobile-melbourne --apply
```

Run from the repository root with Python 3.8+, Node.js, and a globally installed Firebase CLI. The script reuses the existing Firebase CLI login; if needed, run `firebase.cmd login --reauth`. It updates only the three required headings and the System Garden task fields, retains the existing calibration status, backs up the four documents under ignored `tmp/challenge-config-backups/`, and verifies the saved fields. Each write uses field masks and the document's last update time so unrelated fields are preserved and concurrent edits abort the batch. The heading values are development estimates awaiting on-site calibration.

`import-treasures.mjs` replaces entire treasure documents; use the Python patch script for this targeted repair.

See [challenge-config-changes.md](challenge-config-changes.md) for the Chinese change record, field values, execution status, and verification steps.

## Compass lamp thresholds

Each `treasures/{treasureId}` document uses two numeric fields editable in the Firebase Console:

| Field | Default | Meaning | Valid range |
| --- | --- | --- | --- |
| `huntReadyRadiusMeters` | 10 | Maximum distance for The Trail lamp, in metres | Greater than 0 |
| `compassAlignmentToleranceDegrees` | 15 | Maximum heading error for The Bearing lamp, in degrees | 0–180 |

The Android app observes these fields live, including an open compass screen. Missing, nonnumeric, or invalid values fall back independently to the defaults. Legacy `horizontalToleranceDegrees` values are ignored by the compass page. These two fields control the compass gate; the subsequent physical task still uses its own `challenge` rules.

Initialize missing fields using the existing Firebase CLI login:

```sh
python3 database/update-compass-gates.py --project mobile-melbourne
python3 database/update-compass-gates.py --project mobile-melbourne --apply
```

This patch preserves existing values and all unrelated fields, checks document update times, saves a backup under ignored `tmp/compass-gate-backups/`, and reads back all six documents to verify the saved configuration.
