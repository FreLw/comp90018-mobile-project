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
  updateDoc,
  writeBatch,
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

describe("team hunt completion and claims", () => {
  const discovery = (uid, treasureId = "relic-one") => ({
    ownerUid: uid, treasureId, status: "discovered", discoveredAt: serverTimestamp(),
  });
  async function hunt(completed = ["alice"], extra = {}) {
    await seed("treasures/relic-one", { title: "Relic One" });
    await seed("teamRooms/team-one", {
      creatorId: "alice", memberIds: ["alice", "bob"],
      taskId: "relic-one", taskTitle: "Relic One", taskStatus: "hunting",
      huntSessionId: "run-one", taskCompletedMemberIds: completed,
      taskClaimedMemberIds: [], foundFragmentIds: [],
      createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...extra,
    });
    for (const uid of ["alice", "bob"]) {
      await seed(`teamMemberships/${uid}`, { roomId: "team-one", createdAt: serverTimestamp() });
    }
  }

  test("an owner cannot use personal collection to bypass an unfinished team task", async () => {
    await hunt();
    const alice = authenticatedDb("alice");
    await assertFails(setDoc(doc(alice, "users/alice/treasureCollection/relic-one"), discovery("alice")));
    await assertFails(updateDoc(doc(alice, "teamRooms/team-one"), {
      taskClaimedMemberIds: ["alice"], updatedAt: serverTimestamp(),
    }));
    await assertFails(updateDoc(doc(alice, "teamRooms/team-one"), {
      taskCompletedMemberIds: ["alice", "bob"], updatedAt: serverTimestamp(),
    }));
    await assertSucceeds(updateDoc(doc(authenticatedDb("bob"), "teamRooms/team-one"), {
      taskCompletedMemberIds: ["alice", "bob"], updatedAt: serverTimestamp(),
    }));
    await assertSucceeds(setDoc(doc(alice, "users/alice/treasureCollection/relic-one"), discovery("alice")));
  });

  test("each member claims independently and the last claim can atomically save and close", async () => {
    await hunt(["alice", "bob"]);
    const alice = authenticatedDb("alice");
    const first = writeBatch(alice);
    first.set(doc(alice, "users/alice/treasureCollection/relic-one"), discovery("alice"));
    first.update(doc(alice, "teamRooms/team-one"), {
      taskClaimedMemberIds: ["alice"], updatedAt: serverTimestamp(),
    });
    await assertSucceeds(first.commit());
    const bob = authenticatedDb("bob");
    const last = writeBatch(bob);
    last.set(doc(bob, "users/bob/treasureCollection/relic-one"), discovery("bob"));
    last.update(doc(bob, "teamRooms/team-one"), {
      taskId: "", taskTitle: "", taskStatus: "unassigned",
      taskCompletedMemberIds: [], taskClaimedMemberIds: [], foundFragmentIds: [],
      updatedAt: serverTimestamp(),
    });
    await assertSucceeds(last.commit());
  });

  test("a member cannot claim for their teammate or change the session during a hunt", async () => {
    await hunt(["alice", "bob"]);
    const alice = authenticatedDb("alice");
    await assertFails(updateDoc(doc(alice, "teamRooms/team-one"), {
      taskClaimedMemberIds: ["bob"], updatedAt: serverTimestamp(),
    }));
    await assertFails(updateDoc(doc(alice, "teamRooms/team-one"), {
      huntSessionId: "run-two", updatedAt: serverTimestamp(),
    }));
    await assertFails(updateDoc(doc(alice, "teamRooms/team-one"), {
      taskStatus: "assigned", updatedAt: serverTimestamp(),
    }));
  });

  test("starting the same treasure again requires a fresh session ID", async () => {
    await hunt([], { taskStatus: "assigned" });
    const alice = authenticatedDb("alice");
    await assertFails(updateDoc(doc(alice, "teamRooms/team-one"), {
      taskStatus: "hunting", updatedAt: serverTimestamp(),
    }));
    await assertSucceeds(updateDoc(doc(alice, "teamRooms/team-one"), {
      taskStatus: "hunting", huntSessionId: "run-two", updatedAt: serverTimestamp(),
    }));
  });

  test("South Lawn remains locked until every shared fragment is found", async () => {
    await hunt(["alice", "bob"], { taskId: "south_lawn_atlas" });
    await seed("treasures/south_lawn_atlas", { title: "Atlas" });
    await assertFails(setDoc(doc(authenticatedDb("alice"), "users/alice/treasureCollection/south_lawn_atlas"),
      discovery("alice", "south_lawn_atlas")));
    await assertSucceeds(updateDoc(doc(authenticatedDb("alice"), "teamRooms/team-one"), {
      foundFragmentIds: ["south_lawn_north_west", "south_lawn_north_east", "south_lawn_south_west", "south_lawn_south_east"],
      updatedAt: serverTimestamp(),
    }));
    await assertSucceeds(setDoc(doc(authenticatedDb("alice"), "users/alice/treasureCollection/south_lawn_atlas"),
      discovery("alice", "south_lawn_atlas")));
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
