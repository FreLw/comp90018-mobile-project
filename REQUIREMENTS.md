# Lost Treasures — Requirements

## Scope

This document records the behaviour implemented in the Android client. “Explorer” means an authenticated user. The released scope is social connection, private chat, and two-person rooms; Treasure and Map are reserved for future work.

## Functional requirements

### FR-01 Account access

- An explorer can register with a valid email address and a password of at least six characters.
- An explorer can sign in and sign out with Firebase Authentication.
- The app creates a Firestore profile document for a newly authenticated account, assigning a valid default username when required.

**Acceptance:** A new account can register, sign in, sign out, and sign in again without manually creating Firestore data.

### FR-02 Explorer profile

- An explorer can view their email, username, gender, biography, and profile photo.
- An explorer can edit their username and gender and choose a profile image from the device.
- A username must contain 3–30 lowercase letters, digits, or underscores.
- The current implementation validates username format but does not enforce global username uniqueness; friend discovery returns one exact match.

**Acceptance:** Saved username, gender, and photo changes remain visible after reopening the profile and are visible to other authenticated explorers.

### FR-03 Friend discovery and requests

- An explorer can find another explorer by exact username.
- An explorer can send a friend request to a non-friend.
- The recipient can view pending requests and accept or decline each request.
- Either explorer can remove an accepted friendship.

**Acceptance:** After acceptance, both accounts list each other as friends. After removal, neither account lists the other as a friend.

### FR-04 Direct friend chat

- Accepted friends can open a private direct-chat room.
- Both participants can send and receive messages in real time.
- A direct chat provides access to the other explorer's profile and the option to remove that friend.

**Acceptance:** A message sent on one device appears in the other explorer's chat without a manual refresh.

### FR-05 Create and join a two-person room

- An explorer without an active room can create one room and enters it immediately.
- A room exposes a shareable Room ID, which another explorer can use to join.
- A room contains at most two members, and an explorer can hold only one active room membership at a time.

**Acceptance:** Creating a room opens its chat page. A second account can join using the copied ID, and a third account is rejected.

### FR-06 Room chat and details

- Every room member can read and send real-time messages.
- The room header and information icon open Room details.
- Room details show the Room ID, a copy action, current members, and capacity.
- Selecting a member opens their profile.
- A member profile provides the action appropriate to the friend relationship: **Chat**, **Add friend**, **Accept request**, or a pending state.

**Acceptance:** Room members can copy the Room ID, view the other member, manage friendship, and exchange messages while they remain members.

### FR-07 Leave and dismiss a room

- A non-owner can leave a room from Room details.
- Only the creator/owner can dismiss the room.
- Dismissal deletes the room document, its messages, and the active membership links for its members.
- Leaving or dismissal returns the initiating explorer to the no-room entry screen.

**Acceptance:** After leaving, a participant can create or join another room. After dismissal, the Room ID cannot be joined and both former members lose the active room membership.

## Non-functional and security requirements

### NFR-01 Live updates

Profile, friend-request, friend-list, room-membership, room, and message views use Cloud Firestore snapshot listeners. Changes appear without an explicit refresh action.

### NFR-02 Authorization

Firestore Security Rules enforce these controls:

- Explorers can write only their own profile.
- Friend requests are visible only to their sender or recipient.
- Friend-list references are visible only to their owner and can be created or removed only by the friendship participants under the rule constraints.
- Direct-chat messages are readable and creatable only by direct-room members.
- Two-person-room messages are readable and creatable only by active members.
- Only a two-person-room owner can dismiss its room or delete its messages.
- An explorer can delete only their own active membership, except the owner may remove memberships as part of dismissal.

### NFR-03 Input and storage constraints

| Item | Constraint |
| --- | --- |
| Username | 3–30 lowercase letters, digits, or underscores |
| Profile display name | Up to 80 characters in the stored schema |
| Biography | Up to 500 characters |
| Chat message | 1–1,000 characters |
| Sender name | Up to 30 characters |
| Avatar URL | Up to 2,000 characters |
| Uploaded avatar | Image under 5 MiB |
| Room capacity | Two members |

### NFR-04 Supported environment

The client is an Android app built with Kotlin and Jetpack Compose. It requires network access to Firebase, Android API level 26 or higher, Android SDK Platform 37 for compilation, and JDK 21.

### NFR-05 Firebase project configuration and credential handling

- Each developer must download the Firebase Android configuration for the
  project package `com.comp90018.app` from Firebase Console and place it at
  `frontend/app/google-services.json` before building the app.
- `google-services.json` is local Firebase project configuration. It is
  ignored by `frontend/.gitignore`, must not be committed to Git, and must not
  be uploaded to a public repository.
- Team members who need to manage Firebase must be invited to the Firebase
  project with an appropriate IAM role. **Editor** is the normal role for app
  development; **Owner** is required only for project and access management.
- Firebase CLI authentication is performed locally by each developer using
  `firebase login`; Google passwords and verification codes must never be
  shared in source control or project documentation.

## Out of scope / future work

- Treasure discovery, collection, scoring, and task assignment.
- Map rendering and device-location features.
- Rooms with more than two explorers.
- Push notifications, read receipts, typing indicators, and message editing.
- Restoring a room after its owner has dismissed it.
