# Lost Treasures

Lost Treasures is an Android campus-explorer app built with Kotlin and Jetpack
Compose. It lets explorers create a profile, find friends, send private
messages, and form a two-person treasure room with its own real-time chat.

## Features

- Email/password registration and sign-in with Firebase Authentication.
- Explorer profiles: username, gender, bio, and optional profile photo.
- Friend discovery by username, friend requests, accept/decline, and removal.
- One-to-one real-time friend chat.
- Two-person rooms: create or join with a shareable Room ID, real-time chat,
  member details, Room ID copy, and room lifecycle controls.
  - The owner can dismiss a room, removing the room, its messages, and all
    member links.
  - A non-owner can leave; they return to the room entry screen.
- Firestore rules protect profile writes, friendships, direct messages, room
  membership, room messages, leaving, and dismissal.

The **Treasure** and **Map** tabs are navigation placeholders. They are not yet
implemented as treasure or map experiences.

## Technology

| Area | Choice |
| --- | --- |
| Android UI | Kotlin + Jetpack Compose + Material 3 |
| Authentication | Firebase Authentication |
| Data and live updates | Cloud Firestore snapshot listeners |
| Profile photos | Firebase Storage |
| Access control | Firestore Security Rules |

## Repository layout

```text
frontend/                 Android Studio project
  app/                    Compose UI and Firebase service classes
firestore.rules           Firestore authorization and validation rules
firestore.indexes.json    Firestore indexes
storage.rules             Firebase Storage rules
firebase.json             Firebase deployment and emulator configuration
REQUIREMENTS.md           Functional requirements and acceptance criteria
```

## Setup

Prerequisites: Android Studio with a compatible Android SDK, a Firebase
project, and Firebase CLI for deploying rules.

1. In Firebase, create/register an Android application with package name
   `com.comp90018.app`.
2. Download its `google-services.json` and place it at
   `frontend/app/google-services.json`. Do not commit this file.
3. Enable **Authentication → Sign-in method → Email/Password**.
4. Create the default **Cloud Firestore** database and enable **Firebase
   Storage**.
5. From the repository root, deploy the security rules and indexes:

   ```powershell
   firebase deploy --only firestore,storage
   ```

6. Open the `frontend` folder in Android Studio, let Gradle sync, select an
   emulator or physical device with internet access, and press **Run**.

## How to use the app

### Account and profile

1. Select **Create account**, enter a valid email and a password of at least
   six characters, then create the account.
2. Open **profile** from the bottom navigation to review or edit the account.
3. Set a unique username (3–30 lowercase letters, numbers, or `_`), choose a
   gender option, and optionally choose a profile photo. Use **Save changes**.
4. Use **Sign out** on the profile page when finished.

### Friends and direct chat

1. Open **friend** and select **Add friend**.
2. Search for the other explorer's username.
3. Select **Add friend**. The recipient opens **Friend requests** and chooses
   **Accept** or **Decline**.
4. Once accepted, either user can select **Chat** next to the friend to open a
   private conversation. Select the profile icon in a direct chat to inspect
   or remove that friend.

### Two-person rooms

1. Open **Rooms** and select **Create a room**. The app immediately opens the
   new room.
2. Select the information icon—or anywhere on the room header—to open **Room
   details**. Select **Copy** beside the Room ID and send the ID to the second
   explorer.
3. On the other account/device, open **Rooms**, select **Join the room**, enter
   the Room ID, then select **Join the room** again.
4. Both members can send messages in the room. In Room details, select a
   member to view their profile; the screen offers **Chat** for friends or
   **Add friend** otherwise.
5. In Room details, the owner uses **Dismiss room** to permanently remove the
   room and its messages. A participant uses **Leave room** to exit. Both
   actions return the current user to the room entry page.

## Data model

| Collection/path | Purpose |
| --- | --- |
| `users/{uid}` | Explorer profile |
| `users/{uid}/friends/{friendUid}` | Accepted friendship reference |
| `friendRequests/{fromUid_toUid}` | Pending, accepted, or declined request |
| `rooms/{roomId}` | Direct-chat room |
| `rooms/{roomId}/messages/{messageId}` | Direct-chat message |
| `teamRooms/{roomId}` | Two-person room and member IDs |
| `teamRooms/{roomId}/messages/{messageId}` | Team room message |
| `teamMemberships/{uid}` | A user's active team-room reference |

See [REQUIREMENTS.md](REQUIREMENTS.md) for functional requirements and
acceptance criteria. The Android module has an additional
[frontend README](frontend/README.md) for opening and running it in Android
Studio.
