"""Initialize South Lawn fragmentHunt only, using the existing Firebase CLI login."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import runpy
import sys

ROOT = Path(__file__).resolve().parent.parent
helpers = runpy.run_path(str(ROOT / "database/update-challenge-configs.py"))


def encode(value):
    if isinstance(value, str):
        return {"stringValue": value}
    if isinstance(value, (int, float)):
        return {"doubleValue": value}
    if isinstance(value, list):
        return {"arrayValue": {"values": [encode(item) for item in value]}}
    if isinstance(value, dict):
        return {"mapValue": {"fields": {key: encode(item) for key, item in value.items()}}}
    raise ValueError("Unsupported fragment configuration value")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    app_project = json.loads((ROOT / "frontend/app/google-services.json").read_text(encoding="utf-8"))["project_info"]["project_id"]
    if args.project != app_project:
        raise RuntimeError("Project does not match Android app configuration")
    catalogue = json.loads((ROOT / "database/treasures.json").read_text(encoding="utf-8"))
    config = next(entry["data"]["fragmentHunt"] for entry in catalogue["documents"] if entry["documentId"] == "south_lawn_atlas")
    request = helpers["request_json"]
    token = helpers["access_token"]()
    base = f"https://firestore.googleapis.com/v1/projects/{args.project}/databases/(default)/documents"
    url = base + "/treasures/south_lawn_atlas"
    original = request(url, token)
    expected_name = f"projects/{args.project}/databases/(default)/documents/treasures/south_lawn_atlas"
    if original.get("name") != expected_name:
        raise RuntimeError("Unexpected document returned")
    if "fragmentHunt" in original.get("fields", {}):
        print("fragmentHunt already exists; preserving the live configuration.")
        return
    print(f"Project: {args.project}; initialize south_lawn_atlas.fragmentHunt:")
    print(json.dumps(config, indent=2))
    if not args.apply:
        print("Preview complete. Add --apply to initialize this field.")
        return
    backup_dir = ROOT / "tmp/fragment-hunt-backups"
    backup_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    (backup_dir / f"{args.project}-{stamp}.json").write_text(json.dumps(original, indent=2), encoding="utf-8")
    encoded = encode(config)
    request(base + ":commit", token, {"writes": [{
        "update": {"name": expected_name, "fields": {"fragmentHunt": encoded}},
        "updateMask": {"fieldPaths": ["fragmentHunt"]},
        "currentDocument": {"updateTime": original["updateTime"]},
    }]})
    actual = request(url, token)
    if actual.get("fields", {}).get("fragmentHunt") != encoded:
        raise RuntimeError("Saved fragment configuration did not match")
    for key, value in original.get("fields", {}).items():
        if actual["fields"].get(key) != value:
            raise RuntimeError("Unrelated field changed: " + key)
    print("Initialized fragmentHunt; verified all fragments and unchanged unrelated fields.")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("Error: " + str(error), file=sys.stderr)
        sys.exit(1)
