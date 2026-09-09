# Lost Treasures

Lost Treasures is an Android app for campus explorers. The current release
focuses on the social experience: accounts and profiles, friend requests,
one-to-one chat, and a shareable two-person room with real-time messaging.

> The Treasure and Map tabs are intentionally empty placeholders for future
> coursework features; they do not currently provide gameplay or mapping.

## Implemented features

- Email/password registration, sign-in, and sign-out with Firebase Authentication.
- An automatically created explorer profile, with username, gender, bio, and optional photo stored in Firebase.
- Profile editing for username, gender, and profile photo.
- Exact username lookup, friend requests, accept/decline actions, and friend removal.
- Private, real-time direct chat between accepted friends.
- One active two-person room per explorer: create a room, share/copy its ID, join by ID, chat in real time, inspect room members, leave, or dismiss.
- Firebase Security Rules for Firestore and Storage.

## Technology

| Area | Implementation |
| --- | --- |
| Android client | Kotlin, Jetpack Compose, Material 3 |
| Build | Gradle 9.7.1, Android Gradle Plugin 9.3.2, JDK 21 |
| Authentication | Firebase Authentication (email/password) |
| Data and live updates | Cloud Firestore snapshot listeners |
| Images | Firebase Storage |
| Access control | Firestore and Storage Security Rules |

The Android app has `minSdk 26`, `targetSdk 37`, and package name `com.comp90018.app`.

## Repository layout

```text
frontend/                 Android Studio / Gradle project
  app/                    Compose UI and Firebase service classes
firestore.rules           Firestore authorization and validation rules
firestore.indexes.json    Firestore indexes
storage.rules             Firebase Storage rules
firebase.json             Firebase deployment and emulator configuration
REQUIREMENTS.md           Implemented requirements and acceptance criteria
```

## Run the app

### Prerequisites

- Android Studio with Android SDK Platform 37 and JDK 21.
- A Firebase project and the Firebase CLI.
- An Android emulator or device with internet access.

### Firebase setup

1. Create a Firebase project and register an Android app with package name `com.comp90018.app`.
2. Download `google-services.json` and place it at `frontend/app/google-services.json`. Treat this as local configuration; do not commit it.
3. In Firebase Authentication, enable **Email/Password** as a sign-in provider.
4. Create the default Cloud Firestore database and enable Firebase Storage.
5. From the repository root, deploy the rules and Firestore index:

   ```powershell
   firebase deploy --only firestore,storage
   ```

### Build and launch

1. Open the `frontend` directory in Android Studio.
2. Allow Gradle sync to finish, select a device, then choose **Run**.

Or, from `frontend` in PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The module-specific [frontend README](frontend/README.md) has the same quick-start details for Android Studio users.

## Use the app

### Account and profile

1. Create an account with a valid email and a password of at least six characters, or sign in to an existing account.
2. Open **profile** in the bottom navigation to view the profile or sign out.
3. Select **Edit profile** to set a username, choose a gender option, and optionally select a profile photo. A username must be 3–30 lowercase letters, digits, or underscores.

### Friends and direct chat

1. Open **friend** and select **Add friend**.
2. Search for another explorer by their exact username and select **Add friend**.
3. The recipient opens **Friend requests** and selects **Accept** or **Decline**.
4. Once accepted, either explorer selects **Chat** beside that friend to open the private conversation. The friend profile in a direct chat also provides a remove-friend action.

### Two-person rooms

1. Open **Rooms** and select **Create a room**.
2. Select the room header or information icon to open **Room details**. Copy the Room ID and share it with one other explorer.
3. The second explorer opens **Rooms**, selects **Join the room**, enters the ID, then selects **Join the room**.
4. Both members can exchange messages. In Room details, select a member to view their profile and send or manage a friend request.
5. The owner can select **Dismiss room**, which permanently removes the room and its messages. A participant can select **Leave room**. Either action returns the initiating user to the room entry screen.

## Data model

| Path | Purpose |
| --- | --- |
| `users/{uid}` | Explorer profile |
| `users/{uid}/friends/{friendUid}` | Accepted friendship reference |
| `friendRequests/{fromUid_toUid}` | Friend-request state |
| `rooms/{roomId}` | Deterministic private direct-chat room |
| `rooms/{roomId}/messages/{messageId}` | Direct-chat message |
| `teamRooms/{roomId}` | Two-person room and member IDs |
| `teamRooms/{roomId}/messages/{messageId}` | Two-person room message |
| `teamMemberships/{uid}` | Explorer's active two-person-room reference |

See [REQUIREMENTS.md](REQUIREMENTS.md) for detailed requirements, constraints, and acceptance criteria.
