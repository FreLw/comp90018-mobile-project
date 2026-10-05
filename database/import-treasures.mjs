import { readFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = fileURLToPath(new URL(".", import.meta.url));
const argumentValue = (name) => {
  const index = process.argv.indexOf(name);
  return index === -1 ? undefined : process.argv[index + 1];
};

const serviceAccountPath = argumentValue("--key");
const targetProjectId = argumentValue("--target");
const useFirebaseCli = process.argv.includes("--firebase-cli");

if (!targetProjectId || (!serviceAccountPath && !useFirebaseCli) || (serviceAccountPath && useFirebaseCli)) {
  console.error(
    "Usage: node import-treasures.mjs (--key <service-account.json> | --firebase-cli) --target <new-project-id>",
  );
  process.exit(1);
}

const snapshot = JSON.parse(
  await readFile(resolve(scriptDirectory, "treasures.json"), "utf8"),
);

if (snapshot.collection !== "treasures" || !Array.isArray(snapshot.documents)) {
  throw new Error("database/treasures.json is not a treasures catalogue snapshot.");
}

if (serviceAccountPath) {
  const { cert, getApps, initializeApp } = await import("firebase-admin/app");
  const { getFirestore } = await import("firebase-admin/firestore");
  const serviceAccount = JSON.parse(
    await readFile(resolve(process.cwd(), serviceAccountPath), "utf8"),
  );

  if (serviceAccount.project_id !== targetProjectId) {
    throw new Error(
      `Safety check failed: the key belongs to '${serviceAccount.project_id}', not '${targetProjectId}'.`,
    );
  }

  if (!getApps().length) {
    initializeApp({ credential: cert(serviceAccount), projectId: targetProjectId });
  }
  const batch = getFirestore().batch();
  for (const document of snapshot.documents) {
    if (!document.documentId || !document.data) throw new Error("Invalid treasure snapshot document.");
    batch.set(getFirestore().collection("treasures").doc(document.documentId), document.data);
  }
  await batch.commit();
} else {
  const firebaseCliConfig = JSON.parse(
    await readFile(join(process.env.USERPROFILE, ".config", "configstore", "firebase-tools.json"), "utf8"),
  );
  const accessToken = firebaseCliConfig.tokens?.access_token;
  if (!accessToken) throw new Error("No Firebase CLI access token found. Run firebase login first.");

  const toFirestoreValue = (value) => {
    if (value === null) return { nullValue: null };
    if (typeof value === "string") return { stringValue: value };
    if (typeof value === "boolean") return { booleanValue: value };
    if (typeof value === "number" && Number.isFinite(value)) return { doubleValue: value };
    if (Array.isArray(value)) return { arrayValue: { values: value.map(toFirestoreValue) } };
    if (typeof value === "object") {
      return { mapValue: { fields: Object.fromEntries(Object.entries(value).map(([key, item]) => [key, toFirestoreValue(item)])) } };
    }
    throw new Error(`Unsupported JSON value: ${String(value)}`);
  };
  const writes = snapshot.documents.map((document) => {
    if (!document.documentId || !document.data) throw new Error("Invalid treasure snapshot document.");
    return {
      update: {
        name: `projects/${targetProjectId}/databases/(default)/documents/treasures/${encodeURIComponent(document.documentId)}`,
        fields: Object.fromEntries(Object.entries(document.data).map(([key, value]) => [key, toFirestoreValue(value)])),
      },
    };
  });
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${targetProjectId}/databases/(default)/documents:commit`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({ writes }),
  });
  if (!response.ok) throw new Error(`Firestore import failed (${response.status}): ${await response.text()}`);
}
console.log(
  `Imported ${snapshot.documents.length} treasures from '${snapshot.projectId}' to '${targetProjectId}'.`,
);
