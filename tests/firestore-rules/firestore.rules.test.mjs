import { readFile } from "node:fs/promises";
import { after, afterEach, before, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";

const PROJECT_ID = "demo-lost-treasures";
let testEnvironment;

before(async () => {
  const rules = await readFile(new URL("../../firestore.rules", import.meta.url), "utf8");
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules },
  });
});

afterEach(async () => {
  await testEnvironment.clearFirestore();
});

after(async () => {
  await testEnvironment.cleanup();
});

const authenticatedDb = (uid) =>
  testEnvironment.authenticatedContext(uid, { email: `${uid}@example.com` }).firestore();
const unauthenticatedDb = () => testEnvironment.unauthenticatedContext().firestore();

async function seed(path, data) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

function validProfile(uid) {
  return {
    uid,
    email: `${uid}@example.com`,
    username: `${uid}_user`,
    displayName: uid,
    gender: "unspecified",
    bio: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

describe("profiles", () => {
  test("an owner can create their profile but cannot create another user's profile", async () => {
    const alice = authenticatedDb("alice");

    await assertSucceeds(setDoc(doc(alice, "users/alice"), validProfile("alice")));
    await assertFails(setDoc(doc(alice, "users/bob"), validProfile("bob")));
  });
});

describe("treasure catalogue and collection", () => {
  test("signed-in users can read catalogue entries but clients cannot write them", async () => {
    await seed("treasures/relic-one", { title: "Relic One" });
    const alice = authenticatedDb("alice");

    await assertSucceeds(getDoc(doc(alice, "treasures/relic-one")));
    await assertFails(getDoc(doc(unauthenticatedDb(), "treasures/relic-one")));
    await assertFails(setDoc(doc(alice, "treasures/relic-two"), { title: "Relic Two" }));
  });

  test("only the owner can record an existing treasure as discovered", async () => {
    await seed("treasures/relic-one", { title: "Relic One" });
    const discovery = {
      ownerUid: "alice",
      treasureId: "relic-one",
      status: "discovered",
      discoveredAt: serverTimestamp(),
    };

    await assertSucceeds(
      setDoc(doc(authenticatedDb("alice"), "users/alice/treasureCollection/relic-one"), discovery),
    );
    await assertFails(
      setDoc(doc(authenticatedDb("bob"), "users/alice/treasureCollection/relic-one"), discovery),
    );
    await assertFails(
      setDoc(
        doc(authenticatedDb("alice"), "users/alice/treasureCollection/missing"),
        { ...discovery, treasureId: "missing" },
      ),
    );
  });
});

describe("team-room messages", () => {
  test("room members can post messages and outsiders cannot", async () => {
    await seed("teamRooms/team-one", {
      creatorId: "alice",
      memberIds: ["alice", "bob"],
    });
    const message = {
      senderId: "alice",
      senderName: "Alice",
      senderAvatarUrl: "",
      text: "Found the next clue",
      messageType: "text",
      createdAt: serverTimestamp(),
    };

    await assertSucceeds(
      setDoc(doc(authenticatedDb("alice"), "teamRooms/team-one/messages/message-one"), message),
    );
    await assertFails(
      setDoc(
        doc(authenticatedDb("mallory"), "teamRooms/team-one/messages/message-two"),
        { ...message, senderId: "mallory", senderName: "Mallory" },
      ),
    );
  });
});
