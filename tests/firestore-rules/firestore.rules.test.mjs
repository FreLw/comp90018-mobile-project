import { readFile } from "node:fs/promises";
import { after, afterEach, before, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  query,
  where,
  getDocs,
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


describe("room plaza and owner removal", () => {
  const room = (idOnly) => ({
    creatorId: "alice", name: "Explorers", maxMembers: 4, description: "Campus hunt", idOnly,
    memberIds: ["alice", "bob", "carol"], taskId: "relic-one", taskTitle: "Relic One",
    taskStatus: "hunting", taskCompletedMemberIds: ["bob"], taskClaimedMemberIds: ["bob"],
    foundFragmentIds: [], createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  });
  test("the plaza query can list public rooms but cannot list private or all rooms", async () => {
    await seed("teamRooms/public", room(false));
    await seed("teamRooms/private", room(true));
    const db = authenticatedDb("carol");
    await assertSucceeds(getDocs(query(collection(db, "teamRooms"), where("idOnly", "==", false))));
    await assertFails(getDocs(collection(db, "teamRooms")));
    await assertFails(getDocs(query(collection(db, "teamRooms"), where("idOnly", "==", true))));
    await assertSucceeds(getDoc(doc(db, "teamRooms/private")));
  });
  test("only owners can remove members and progress with an atomic membership deletion", async () => {
    await seed("teamRooms/removal", room(false));
    await seed("teamMemberships/bob", { roomId: "removal", unreadCount: 0, createdAt: serverTimestamp() });
    const changes = { memberIds: ["alice", "carol"], taskCompletedMemberIds: [], taskClaimedMemberIds: [], updatedAt: serverTimestamp() };
    await assertFails(updateDoc(doc(authenticatedDb("alice"), "teamRooms/removal"), changes));
    for (const uid of ["carol", "alice"]) {
      const db = authenticatedDb(uid);
      const batch = writeBatch(db);
      batch.update(doc(db, "teamRooms/removal"), changes);
      batch.delete(doc(db, "teamMemberships/bob"));
      if (uid === "carol") await assertFails(batch.commit());
      else {
        await assertSucceeds(getDoc(doc(db, "teamMemberships/bob")));
        await assertSucceeds(batch.commit());
      }
    }
    await assertFails(getDoc(doc(authenticatedDb("bob"), "teamRooms/removal/messages/any")));
  });
  test("only the owner can change the join mode and it must be boolean", async () => {
    await seed("teamRooms/settings", room(true));
    await assertFails(updateDoc(doc(authenticatedDb("bob"), "teamRooms/settings"), { idOnly: false, updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(authenticatedDb("alice"), "teamRooms/settings"), { idOnly: "public", updatedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(doc(authenticatedDb("alice"), "teamRooms/settings"), { idOnly: false, updatedAt: serverTimestamp() }));
  });
});


describe("room join modes", () => {
  test("room creation atomically saves membership for both join modes", async () => {
    for (const idOnly of [true, false]) {
      const uid = `creator-${idOnly}`;
      const roomId = `created-${idOnly}`;
      const db = authenticatedDb(uid);
      const batch = writeBatch(db);
      batch.set(doc(db, `teamRooms/${roomId}`), {
        creatorId: uid, name: "Explorers", description: "Campus hunt", maxMembers: 4, idOnly,
        memberIds: [uid], taskId: "", taskTitle: "", taskStatus: "unassigned",
        taskCompletedMemberIds: [], foundFragmentIds: [], taskClaimedMemberIds: [],
        createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      });
      batch.set(doc(db, `teamMemberships/${uid}`), {
        roomId, createdAt: serverTimestamp(), unreadCount: 0,
      });
      await assertSucceeds(batch.commit());
      const membership = await getDoc(doc(db, `teamMemberships/${uid}`));
      if (membership.data().roomId !== roomId) throw new Error("Created room membership is missing");
    }
  });

  test("room creation rejects invalid join modes and impersonated owners", async () => {
    const db = authenticatedDb("alice");
    for (const [roomId, creatorId, idOnly] of [
      ["invalid-mode", "alice", "private"], ["impersonated-owner", "bob", true],
    ]) {
      const batch = writeBatch(db);
      batch.set(doc(db, `teamRooms/${roomId}`), {
        creatorId, name: "Explorers", description: "", maxMembers: 4, idOnly,
        memberIds: [creatorId], taskId: "", taskTitle: "", taskStatus: "unassigned",
        taskCompletedMemberIds: [], foundFragmentIds: [], taskClaimedMemberIds: [],
        createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      });
      batch.set(doc(db, "teamMemberships/alice"), {
        roomId, createdAt: serverTimestamp(), unreadCount: 0,
      });
      await assertFails(batch.commit());
    }
  });

  test("public and ID-only rooms both allow direct joining, but full rooms reject it", async () => {
    for (const [roomId, idOnly, memberIds] of [
      ["public", false, ["alice"]], ["private", true, ["alice"]], ["full", false, ["alice", "bob"]],
    ]) {
      await seed(`teamRooms/${roomId}`, {
        creatorId: "alice", name: "Room", description: "", maxMembers: 2, idOnly, memberIds,
        taskId: "", taskTitle: "", taskStatus: "unassigned", taskCompletedMemberIds: [],
        taskClaimedMemberIds: [], foundFragmentIds: [], createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      });
      const uid = `joining-${roomId}`;
      const db = authenticatedDb(uid);
      const batch = writeBatch(db);
      batch.update(doc(db, `teamRooms/${roomId}`), { memberIds: [...memberIds, uid], updatedAt: serverTimestamp() });
      batch.set(doc(db, `teamMemberships/${uid}`), { roomId, unreadCount: 0, createdAt: serverTimestamp() });
      if (roomId === "full") await assertFails(batch.commit());
      else await assertSucceeds(batch.commit());
    }
  });
});
