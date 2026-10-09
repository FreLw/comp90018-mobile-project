"""Initialize missing memberQuiz fields using the existing Firebase CLI login."""

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
    if isinstance(value, int) and not isinstance(value, bool):
        return {"integerValue": str(value)}
    if isinstance(value, list):
        return {"arrayValue": {"values": [encode(item) for item in value]}}
    if isinstance(value, dict):
        return {"mapValue": {"fields": {key: encode(item) for key, item in value.items()}}}
    raise ValueError("Unsupported quiz value")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    app_project = json.loads((ROOT / "frontend/app/google-services.json").read_text(encoding="utf-8"))["project_info"]["project_id"]
    if args.project != app_project:
        raise RuntimeError("Project does not match Android app configuration")
    catalogue = json.loads((ROOT / "database/treasures.json").read_text(encoding="utf-8"))
    configs = {doc["documentId"]: doc["data"]["memberQuiz"] for doc in catalogue["documents"] if "memberQuiz" in doc["data"]}
    if not configs:
        raise RuntimeError("No quiz configurations found")
    for config in configs.values():
        questions = config.get("questions")
        if not isinstance(questions, list) or not questions:
            raise RuntimeError("Quiz must contain questions")
        for question in questions:
            options = question.get("options")
            answer = question.get("correctAnswerIndex")
            if (not isinstance(question.get("prompt"), str) or not question["prompt"].strip()
                    or not isinstance(options, list) or not 2 <= len(options) <= 26
                    or any(not isinstance(option, str) or not option.strip() for option in options)
                    or type(answer) is not int or not 0 <= answer < len(options)):
                raise RuntimeError("Invalid quiz question")
    request = helpers["request_json"]
    token = helpers["access_token"]()
    base = f"https://firestore.googleapis.com/v1/projects/{args.project}/databases/(default)/documents"
    originals, writes = {}, []
    for document_id, config in configs.items():
        original = request(base + "/treasures/" + document_id, token)
        name = f"projects/{args.project}/databases/(default)/documents/treasures/{document_id}"
        if original.get("name") != name or not original.get("updateTime"):
            raise RuntimeError("Unexpected document returned: " + document_id)
        if "memberQuiz" in original.get("fields", {}):
            print(document_id + ": preserving existing live memberQuiz")
            continue
        originals[document_id] = original
        writes.append({
            "update": {"name": name, "fields": {"memberQuiz": encode(config)}},
            "updateMask": {"fieldPaths": ["memberQuiz"]},
            "currentDocument": {"updateTime": original["updateTime"]},
        })
        print(f"{document_id}: initialize {len(config['questions'])} questions")
    if not writes:
        print("No missing quizzes to initialize.")
        return
    if not args.apply:
        print(f"Preview: {len(writes)} documents in {args.project}. Add --apply to upload.")
        return
    backup_dir = ROOT / "tmp/member-quiz-backups"
    backup_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    (backup_dir / f"{args.project}-{stamp}.json").write_text(json.dumps(originals, indent=2), encoding="utf-8")
    request(base + ":commit", token, {"writes": writes})
    for document_id, original in originals.items():
        actual = request(base + "/treasures/" + document_id, token)
        if actual.get("fields", {}).get("memberQuiz") != encode(configs[document_id]):
            raise RuntimeError("Saved quiz did not match: " + document_id)
        unrelated = {key: value for key, value in actual["fields"].items() if key != "memberQuiz"}
        if unrelated != original.get("fields", {}):
            raise RuntimeError("Unrelated fields changed: " + document_id)
    print(f"Uploaded and verified {len(writes)} quizzes; all unrelated fields unchanged.")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("Error: " + str(error), file=sys.stderr)
        sys.exit(1)
