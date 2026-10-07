"""Add editable compass lamp thresholds to the Android app's Firestore treasures."""

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import runpy
import shutil
import subprocess
import sys

# Some macOS Python installs lack bundled certificates; use the system CA store.
if sys.platform == "darwin" and Path("/etc/ssl/cert.pem").is_file():
    os.environ.setdefault("SSL_CERT_FILE", "/etc/ssl/cert.pem")

ROOT = Path(__file__).resolve().parent.parent
helpers = runpy.run_path(str(ROOT / "database/update-challenge-configs.py"))
request_json = helpers["request_json"]
decode = helpers["decode"]
DEFAULTS = {
    "huntReadyRadiusMeters": 10,
    "compassAlignmentToleranceDegrees": 15,
    "horizontalToleranceDegrees": 12,
}


def access_token():
    node = shutil.which("node")
    if not node:
        raise RuntimeError("Node.js is required to reuse the Firebase CLI login.")
    cli = shutil.which("firebase.cmd") or shutil.which("firebase")
    candidates = []
    if cli:
        resolved = Path(cli).resolve()
        candidates += [resolved.parent.parent / "lib/auth.js",
                       resolved.parent / "node_modules/firebase-tools/lib/auth.js"]
    candidates += list((Path.home() / ".npm/_npx").glob("*/node_modules/firebase-tools/lib/auth.js"))
    module = next((path for path in candidates if path.is_file()), None)
    if module is None:
        raise RuntimeError("Firebase CLI is required. Run npx firebase-tools login first.")
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
    result = subprocess.run([node, "-e", helper, str(module)],
                            capture_output=True, text=True, timeout=60)
    if result.returncode:
        raise RuntimeError("Firebase login failed. Run npx firebase-tools login --reauth and retry.")
    try:
        token = json.loads(result.stdout)["access_token"]
        if not isinstance(token, str) or not token:
            raise ValueError()
        return token
    except (ValueError, KeyError):
        raise RuntimeError("Could not read Firebase CLI access token.") from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    app_project = json.loads((ROOT / "frontend/app/google-services.json").read_text())["project_info"]["project_id"]
    if args.project != app_project:
        raise RuntimeError("Project does not match Android app configuration.")
    catalogue = json.loads((ROOT / "database/treasures.json").read_text())
    token = access_token()
    base = f"https://firestore.googleapis.com/v1/projects/{args.project}/databases/(default)/documents"
    documents, writes = [], []
    print(f"Project: {args.project} | {'APPLY' if args.apply else 'PREVIEW'}")
    for entry in catalogue["documents"]:
        document_id = entry["documentId"]
        document = request_json(base + "/treasures/" + document_id, token)
        if document.get("name") != f"projects/{args.project}/databases/(default)/documents/treasures/{document_id}":
            raise RuntimeError("Unexpected document returned for " + document_id)
        documents.append(document)
        fields = document.get("fields", {})
        missing = {key: value for key, value in DEFAULTS.items() if key not in fields}
        print(document_id + ": " + (json.dumps(missing) if missing else "Already configured"))
        if missing:
            writes.append({
                "update": {"name": document["name"], "fields": {
                    key: {"integerValue": str(value)} for key, value in missing.items()
                }},
                "updateMask": {"fieldPaths": list(missing)},
                "currentDocument": {"updateTime": document["updateTime"]},
            })
    if not args.apply:
        print("Preview complete. Add --apply to initialize missing fields.")
        return
    if writes:
        backup_dir = ROOT / "tmp/compass-gate-backups"
        backup_dir.mkdir(parents=True, exist_ok=True)
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        backup = backup_dir / f"{args.project}-{stamp}.json"
        backup.write_text(json.dumps({"project": args.project, "documents": documents}, indent=2))
        request_json(base + ":commit", token, {"writes": writes})
    for original in documents:
        document_id = original["name"].rsplit("/", 1)[-1]
        actual = request_json(base + "/treasures/" + document_id, token)
        for key, default in DEFAULTS.items():
            expected = original.get("fields", {}).get(key, {"integerValue": str(default)})
            if decode(actual["fields"][key]) != decode(expected):
                raise RuntimeError("Verification failed: " + document_id + "." + key)
    print(f"Updated {len(writes)} documents; verified all {len(documents)} compass configurations.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.SubprocessError, ValueError, KeyError) as error:
        print("Error: " + str(error), file=sys.stderr)
        sys.exit(1)
