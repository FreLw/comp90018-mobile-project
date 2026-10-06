# Shared treasure database

Verified runtime export: treasures.json.

Contains only the six allowlisted treasure documents. No credentials or user/chat/room data are included.

Map-assisted simulated GPS/heading values are stored for Union Lawn, Wilson Hall and South Lawn Atlas. calibrationStatus remains pending for later real-device/on-site testing.

prototypeImageUrl is intentionally removed; local artwork is selected through artworkKey.

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
