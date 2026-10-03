# Frontend architecture

This document is the short guide to the Android client. It is intended both
for maintenance and for explaining the project in a demonstration.

## The one-sentence explanation

Lost Treasures is a single-module Jetpack Compose app: screens render immutable
ViewModel state, ViewModels call repository interfaces, and Firebase-specific
code stays behind repository implementations and small service objects.

## Package tree

```text
com.comp90018.app/
├── MainActivity.kt              App process and signed-in/signed-out entry
├── AppShell.kt                  Signed-in tabs and cross-feature coordination
├── AppShellViewModel.kt         Current user's profile state
├── AppTheme.kt                  Shared colours, fonts, and Material theme
├── navigation/                  Bottom navigation destinations and UI
├── data/
│   ├── auth/                    Authentication repository + Firebase service
│   ├── chat/                    Direct-chat models, repository, message service
│   ├── profile/                 User profile persistence
│   ├── rooms/                   Team-room models, repository, Firebase service
│   ├── social/                  Friends, requests, search, direct-room discovery
│   └── treasure/                Treasure catalogue and collected treasures
├── features/
│   ├── auth/                    Login/register screen and state
│   ├── chat/                    One-to-one conversation screen and state
│   ├── friends/                 Friends navigation, lists, requests, profiles
│   ├── map/                     Map orchestration, discovery and hunt UI/logic
│   ├── profile/                 Profile display, edit, settings and About
│   ├── rooms/                   Team-room entry, chat and cooperative hunts
│   ├── treasure/                Treasure route and detail screens
│   └── treasurechallenge/       Sensor/minigame challenge flow
├── contextengine/               Combines device context for challenge rules
├── sensors/                     Testable wrappers for location/motion/audio/etc.
├── diagnostics/                 Shared sensor error reporting
└── ui/components/               Reusable UI with no feature ownership
```

Non-code reference material belongs in `frontend/docs/reference/`, not under a
Kotlin source directory. Android images, fonts, and map styles remain under
`app/src/main/res/` because Android generates their `R` identifiers there.

## Dependency direction

```text
Compose Screen/Content
        ↓ user events / observes StateFlow
ViewModel + UiState
        ↓ calls an interface
Repository interface
        ↓ implemented by
Firebase…Repository
        ↓ delegates low-level operations
Firebase…Service → Auth / Firestore / Storage
```

The important rule is that arrows do not point upwards. A ViewModel must not
import Firestore, and a Firebase service must not import a Compose screen. This
is why ViewModels can be unit-tested with fake repositories.

## Naming convention

| Suffix | Meaning | Example |
| --- | --- | --- |
| `Screen` | Feature entry point or full-screen route | `FriendsScreen` |
| `Content` | A sizeable UI section extracted from a screen | `FriendsHomeContent` |
| `ViewModel` | Owns screen state and feature actions | `DirectChatViewModel` |
| `UiState` | Immutable values rendered by a screen | `FriendsUiState` |
| `Repository` | Interface used by ViewModels | `ChatRepository` |
| `Firebase…Repository` | Firebase-backed repository adapter | `FirebaseChatRepository` |
| `Firebase…Service` | Low-level SDK queries/writes | `FirebaseDirectChatService` |
| `…Models` | Data classes/enums for one data domain | `SocialModels` |

## Main feature ownership

### Friends and direct chat

- `FriendsScreen.kt` only chooses between Friends sub-pages.
- `FriendsHomeContent.kt` owns the Chats/Contacts presentation.
- `FriendRequestsContent.kt` owns incoming/outgoing request presentation.
- `FriendsViewModel.kt` combines Firebase contacts with starter contacts.
- `data/social/` owns friend search, requests and chat-room summaries.
- `data/chat/` owns message models, message writes and image upload.

### Map and treasure hunt

- `MapScreen.kt` coordinates map, selected treasure and hunt screens.
- `MapDiscoveryLogic.kt` contains the distance bands and readiness conditions;
  it is pure logic and is covered by unit tests.
- `DeviceEnvironment.kt` contains the shared emulator-location policy.
- `MapRelic.kt` is the catalogue model used by Map, Treasure and Rooms.
- `treasurechallenge/` owns challenge-specific sensor/minigame state.

The hunt unlock rule is deliberately visible in one place:

```text
within 10 m + facing treasure + phone level → ready to continue
```

### Team rooms

- `RoomsScreen.kt` renders room entry, team chat and room details.
- `RoomsViewModel.kt` owns membership/create/join state.
- `TeamRoomChatViewModel.kt` owns one active room and shared hunt progress.
- `data/rooms/` owns `TeamRoom` models and Firebase persistence.

## Firebase collections used by the client

| Collection | Purpose |
| --- | --- |
| `users/{uid}` | Profile and settings |
| `users/{uid}/friends` | Friend rows and unread counts |
| `users/{uid}/treasureCollection` | Treasures obtained by one user |
| `friendRequests` | Incoming/outgoing friend-request workflow |
| `rooms/{roomId}/messages` | One-to-one chat |
| `teamMemberships/{uid}` | User's current team room and unread count |
| `teamRooms/{roomId}` | Team membership and cooperative hunt state |
| `teamRooms/{roomId}/messages` | Team-room chat |
| `treasures` | Shared treasure catalogue |

Firestore and Storage rules are part of the feature implementation. If a data
shape changes, update and deploy the matching rules with the application code.

## How to explain the design

1. Start at `MainActivity`: it selects authentication or the signed-in shell.
2. `AppShell` owns the five bottom tabs and state shared between tabs.
3. Open one feature package: its Screen renders a `UiState` from its ViewModel.
4. The ViewModel talks only to a repository interface, so it is easy to test.
5. Firebase SDK details are isolated in `data/`, while hardware APIs are
   isolated in `sensors/` and combined by `contextengine/`.

## Comment policy

Comments should explain a constraint or a non-obvious decision—for example,
why a Firebase write is atomic or why pending snapshots are ignored. Comments
should not repeat a function name or narrate straightforward Compose layout.
