"""Preview or patch four treasure challenges using the existing Firebase CLI login."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import subprocess
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parent.parent
PROJECT = "mobile-melbourne"
PATCHES = {
    "union_lawn_lost_lake": ("UNION_LAWN_PHOTO", {"requiredHeadingDegrees": 288}),
    "wilson_hall_rosette": ("WILSON_HALL_OBSERVATION", {"requiredHeadingDegrees": 197}),
    "south_lawn_atlas": ("SOUTH_LAWN_VIEWING_ANGLE", {"requiredHeadingDegrees": 185}),
    "system_garden_glasshouse": ("SYSTEM_GARDEN_GLASSHOUSE", {
        "requiredHeadingDegrees": None,
        "photoActionRequired": True,
        "requiresStationary": False,
        "requiresStability": False,
        "requiresRotationStill": False,
        "requiresHorizontal": False,
        "holdDurationMs": 0,
    }),
}


def access_token():
    # Reuse the CLI's refresh logic; never print or save its credentials.
    firebase = shutil.which("firebase.cmd") or shutil.which("firebase")
    node = shutil.which("node")
    if not firebase or not node:
        raise RuntimeError("Node.js and Firebase CLI are required on PATH.")
    auth_module = Path(firebase).resolve().parent / "node_modules/firebase-tools/lib/auth.js"
    if not auth_module.is_file():
        raise RuntimeError("Cannot locate the globally installed Firebase CLI auth module.")
    helper = """
const { getAccessToken, getGlobalDefaultAccount } = require(process.argv[1]);
(async () => {
  const account = getGlobalDefaultAccount();
  if (!account?.tokens?.refresh_token) throw new Error('Login required');
  const token = await getAccessToken(account.tokens.refresh_token,
    ['https://www.googleapis.com/auth/cloud-platform']);
  process.stdout.write(JSON.stringify({access_token: token.access_token}));
})().catch(() => process.exitCode = 1);
"""
    result = subprocess.run([node, "-e", helper, str(auth_module)],
                            capture_output=True, text=True, timeout=60)
    if result.returncode:
        raise RuntimeError("Firebase login failed. Run firebase.cmd login --reauth and retry.")
    try:
        token = json.loads(result.stdout)["access_token"]
        if not isinstance(token, str) or not token:
            raise ValueError()
        return token
    except (ValueError, KeyError):
        raise RuntimeError("Could not read the Firebase CLI access token.") from None


def request_json(url, token, payload=None):
    request = Request(url, data=None if payload is None else json.dumps(payload).encode("utf-8"),
                      headers={"Authorization": "Bearer " + token,
                               "Content-Type": "application/json"})
    try:
        with urlopen(request, timeout=30) as response:
            return json.load(response)
    except HTTPError as error:
        raise RuntimeError("Firestore HTTP {}. Check login, project access, and whether a document changed; rerun the preview.".format(error.code)) from None
    except URLError:
        raise RuntimeError("Unable to reach Firestore. Check your network connection.") from None


def encode(value):
    if value is None:
        return {"nullValue": None}
    if isinstance(value, bool):
        return {"booleanValue": value}
    return {"integerValue": str(value)}


def decode(value):
    for key in ("nullValue", "booleanValue", "stringValue", "doubleValue"):
        if key in value:
            return value[key]
    if "integerValue" in value:
        return int(value["integerValue"])
    raise RuntimeError("Unexpected Firestore value type in challenge configuration.")


def plan(document, document_id):
    expected_type, patch = PATCHES[document_id]
    challenge = document.get("fields", {}).get("challenge", {}).get("mapValue", {}).get("fields", {})
    if decode(challenge.get("type", {"nullValue": None})) != expected_type:
        raise RuntimeError("Unexpected challenge type for " + document_id)
    changes = {}
    for field, expected in patch.items():
        before = decode(challenge[field]) if field in challenge else None
        if field not in challenge or type(before) is not type(expected) or before != expected:
            changes[field] = expected
            print("  challenge.{}: {} -> {}".format(field, json.dumps(before), json.dumps(expected)))
    if not changes:
        print("  Already up to date.")
        return None
    if not document.get("updateTime"):
        raise RuntimeError("Missing document update time for " + document_id)
    return {
        "update": {"name": document["name"], "fields": {
            "challenge": {"mapValue": {"fields": {field: encode(value) for field, value in changes.items()}}},
        }},
        "updateMask": {"fieldPaths": ["challenge." + field for field in changes]},
        "currentDocument": {"updateTime": document["updateTime"]},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, choices=[PROJECT])
    parser.add_argument("--apply", action="store_true", help="Write changes; otherwise only preview.")
    args = parser.parse_args()
    app_project = json.loads((ROOT / "frontend/app/google-services.json").read_text(encoding="utf-8"))["project_info"]["project_id"]
    if app_project != args.project:
        raise RuntimeError("Project does not match the Android app configuration.")
    token = access_token()
    base = "https://firestore.googleapis.com/v1/projects/{}/databases/(default)/documents".format(args.project)
    documents, writes = [], []
    print("Project: {} | {}".format(args.project, "APPLY" if args.apply else "PREVIEW (no writes)"))
    print("Heading estimates remain pending on-site calibration.")
    for document_id in PATCHES:
        document = request_json(base + "/treasures/" + document_id, token)
        expected_name = "projects/{}/databases/(default)/documents/treasures/{}".format(args.project, document_id)
        if document.get("name") != expected_name:
            raise RuntimeError("Unexpected document returned for " + document_id)
        documents.append(document)
        print(document_id)
        write = plan(document, document_id)
        if write:
            writes.append(write)
    if not args.apply:
        print("Preview complete. Add --apply to update these fields.")
        return
    if not writes:
        print("Nothing to update.")
        return
    backup_dir = ROOT / "tmp/challenge-config-backups"
    backup_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    backup = backup_dir / (args.project + "-" + stamp + ".json")
    backup.write_text(json.dumps({"project": args.project, "documents": documents}, indent=2), encoding="utf-8")
    print("Backup: " + str(backup))
    request_json(base + ":commit", token, {"writes": writes})
    for document_id in PATCHES:
        document = request_json(base + "/treasures/" + document_id, token)
        for field, expected in PATCHES[document_id][1].items():
            actual = decode(document["fields"]["challenge"]["mapValue"]["fields"][field])
            if actual != expected or (isinstance(expected, bool) and not isinstance(actual, bool)):
                raise RuntimeError("Write completed, but verification failed for {}.{}".format(document_id, field))
    print("Updated {} documents; all four challenge configurations verified.".format(len(writes)))


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.SubprocessError, ValueError, KeyError) as error:
        print("Error: " + str(error), file=sys.stderr)
        sys.exit(1)
