import { readFile } from 'node:fs/promises';

const project = 'demo-lost-treasures';
const password = 'RoomTest123!';
const now = new Date().toISOString();

function value(input) {
  if (input === null) return { nullValue: null };
  if (Array.isArray(input)) return { arrayValue: { values: input.map(value) } };
  if (typeof input === 'object') return { mapValue: { fields: fields(input) } };
  if (typeof input === 'boolean') return { booleanValue: input };
  if (typeof input === 'number') return Number.isInteger(input) ? { integerValue: String(input) } : { doubleValue: input };
  return { stringValue: input };
}
const fields = (data) => Object.fromEntries(Object.entries(data).map(([key, input]) => [key, value(input)]));
async function write(path, data) {
  const encoded = fields(data);
  for (const key of ['createdAt', 'updatedAt']) if (key in data) encoded[key] = { timestampValue: now };
  const response = await fetch(`http://127.0.0.1:8080/v1/projects/${project}/databases/(default)/documents/${path}`, {
    method: 'PATCH', headers: { Authorization: 'Bearer owner', 'Content-Type': 'application/json' },
    body: JSON.stringify({ fields: encoded }),
  });
  if (!response.ok) throw new Error(`Local seed failed: ${path}: ${await response.text()}`);
}

const members = [];
for (let i = 1; i <= 4; i++) {
  const email = `player${i}@example.com`;
  let response = await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  });
  if (!response.ok) response = await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  });
  const account = await response.json();
  if (!response.ok || !account.localId) throw new Error(`Local account failed: ${JSON.stringify(account)}`);
  const uid = account.localId;
  members.push(uid);
  await write(`users/${uid}`, {
    uid, email, username: `player${i}`, displayName: `Player ${i}`, gender: 'unspecified',
    bio: '', avatarUrl: '', phone: '', department: '', major: '', searchable: true,
    notificationsEnabled: true, soundEffectsEnabled: false, hapticsEnabled: false,
    preciseLocationEnabled: true, createdAt: now, updatedAt: now,
  });
  await write(`teamMemberships/${uid}`, { roomId: 'fragment-test', unreadCount: 0, createdAt: now });
}
const catalogue = JSON.parse(await readFile(new URL('../../database/treasures.json', import.meta.url), 'utf8'));
for (const item of catalogue.documents) await write(`treasures/${item.documentId}`, item.data);
await write('teamRooms/fragment-test', {
  creatorId: members[0], memberIds: members, name: 'Local four-player test', maxMembers: 4,
  description: 'Local emulator only', idOnly: true, taskId: 'south_lawn_atlas', taskTitle: 'South Lawn Atlas',
  taskStatus: 'assigned', taskCompletedMemberIds: [], foundFragmentIds: [], taskClaimedMemberIds: [],
  createdAt: now, updatedAt: now,
});
console.log('Local room ready: fragment-test. Accounts: player1@example.com through player4@example.com.');
console.log(`Test password: ${password}. Player 1 is the room owner. Start Hunt to create assignments.`);
