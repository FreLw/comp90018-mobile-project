# Lost Treasures — Requirements

## Scope

This document specifies the implemented application behaviour. “Explorer”
means an authenticated user. The app is intentionally limited to social and
two-person room functionality; the Treasure and Map tabs are reserved for
future work.

## Functional requirements

### FR-01 Account access

- An explorer can register with a valid email address and a password of at
  least six characters.
- An explorer can sign in and sign out using Firebase Authentication.
- On registration, the app creates a matching Firestore profile document.

**Acceptance:** A new account can register, sign in, sign out, and sign in
again without manually creating Firestore data.

### FR-02 Explorer profile

- An explorer can view their email, username, gender, bio, and profile photo.
- An explorer can edit their username and gender and can choose a profile
  image from the device.
- Usernames contain 3–30 lowercase letters, digits, or underscores.

**Acceptance:** Saved profile changes are visible after reopening the profile
page and to other authenticated explorers who view the profile.

### FR-03 Friend discovery and requests

- An explorer can find another explorer by exact username.
- An explorer can send a friend request to a non-friend.
- The recipient can view pending requests and accept or decline each request.
- Either explorer can remove an accepted friendship.

**Acceptance:** After acceptance, both accounts list each other as friends.
After removal, neither account lists the other as a friend.

### FR-04 Direct friend chat

- Accepted friends can open a private direct-chat room.
- Both participants can send and receive messages in real time.
- A direct chat provides access to the other explorer’s profile.

**Acceptance:** A message sent on one device appears in the other friend’s
chat without manually refreshing the app.

### FR-05 Create and join a two-person room

- An explorer without an active room can create one room.
- The creator enters the room immediately after creation.
- A room exposes a shareable Room ID.
- Another explorer can enter that Room ID to join the room.
- A room contains no more than two members; an explorer can belong to one
  active room at a time.

**Acceptance:** Creating a room immediately opens its chat page. A second
account can join using the copied Room ID. A third account is rejected.

### FR-06 Room chat and details

- Every room member can read and send real-time room messages.
- The room header and information icon open Room details.
- Room details display the Room ID, a Copy action, current members, and room
capacity.
- Selecting a member opens their profile.
- A member profile displays **Chat** for an existing friend, **Add friend**
  for a non-friend, **Accept request** for an incoming request, or a pending
  state when a request was sent.

**Acceptance:** Each member can copy the Room ID, open the other member’s
profile, and exchange messages while they remain room members.

### FR-07 Leave and dismiss a room

- A non-owner can leave their room from Room details.
- Only the creator/owner can dismiss the room.
- Dismissing deletes all room messages, all active member links, and the room
document.
- Leaving or dismissing returns the initiating explorer to the no-room entry
screen.

**Acceptance:** After a participant leaves, they can create or join another
room. After an owner dismisses, both previous members see the no-room entry
screen and the old Room ID cannot be joined.

## Non-functional and security requirements

### NFR-01 Live updates

Profile, friendship, room membership, and message views use Firestore snapshot
listeners so changes appear without an explicit refresh action.

### NFR-02 Authorization

Firestore Security Rules enforce the following:

- Only an owner can edit or delete their own profile.
- Friend requests can only be read by their sender or recipient.
- Direct-chat messages can only be read or created by direct-room members.
- Team-room messages can only be read or created by active room members.
- Only a room owner can dismiss a room or delete its messages.
- A participant can remove only their own active room membership.

### NFR-03 Input constraints

- Usernames: 3–30 permitted lowercase characters.
- Profile display name: up to 80 characters.
- Biography: up to 500 characters.
- Chat message: 1–1000 characters.
- Sender name: up to 30 characters.
- Avatar URL: up to 2000 characters.
- Room capacity: two members.

### NFR-04 Supported environment

The client is an Android app, built with Kotlin and Jetpack Compose, requiring
network access to Firebase. The minimum Android SDK is 26.

## Out of scope / future work

- Treasure discovery, collection, scoring, and task assignment.
- Map rendering and device-location features.
- Group rooms larger than two explorers.
- Push notifications, read receipts, typing indicators, and message editing.
- Room restoration after an owner dismisses it.
