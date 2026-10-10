# Frontend and UI code guide

This guide maps interface work to the actual Android client files. Source comments explain file ownership,
important drawing functions, state contracts, and the interactions that connect them.

## Where interface code belongs

- Main Kotlin root: `app/src/main/kotlin/com/comp90018/app/`.
- Feature-specific screens, artwork, animations, and state: `features/<feature>/` under that root.
- Reusable input/composer/empty-state components: `ui/components/`.
- Shared Compose colours and fonts: `AppTheme.kt`; green-task tokens also live in `QuestArtwork.kt`.
- Images, vector icons, fonts, and map styles: `app/src/main/res/`.
- Development-only task sliders and fixtures: `app/src/debug/`; corresponding stubs: `app/src/release/`.
- Interface/device checks: `app/src/androidTest/kotlin/com/comp90018/app/`.

Screens render state and send user actions to ViewModels or supplied callbacks. Repositories provide cloud data.
Sensor/context adapters provide measurements; rule evaluation determines satisfied conditions. Artwork converts
those readings and conditions into motion, colour, and visual emphasis.

## Common editing routes

| Change | Start here |
| --- | --- |
| Global colour and typography | [AppTheme.kt](../app/src/main/kotlin/com/comp90018/app/AppTheme.kt) |
| Bottom navigation | [AppNavigation.kt](../app/src/main/kotlin/com/comp90018/app/navigation/AppNavigation.kt) |
| Pre-task compass gate | `TreasureCompassGate` and `DivineCompassVisual` in [TreasureCompassGate.kt](../app/src/main/kotlin/com/comp90018/app/features/map/compass/TreasureCompassGate.kt) |
| Green-task page structure | [TreasureChallengeScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeScreen.kt) and [ChallengeExperience.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeExperience.kt) |
| South Lawn vines/stars/Atlas | [SouthLawnArtwork.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/SouthLawnArtwork.kt) |
| Wilson flowers/water/pointer | [WilsonHallArtwork.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/WilsonHallArtwork.kt) and [WilsonFieldDynamics.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/WilsonFieldDynamics.kt) |
| Photo preview and logo morph | [QuestPhotoExperience.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestPhotoExperience.kt) and [QuestPhotoStyle.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestPhotoStyle.kt) |
| How to play scroll | [QuestFieldGuide.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestFieldGuide.kt) |
| Requirement stars | [QuestRequirementStars.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestRequirementStars.kt) |
| Sensor-driven drawing intensity | [QuestVisualSignals.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestVisualSignals.kt) |
| Completion/shutter readiness | [TreasureChallengeViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeViewModel.kt) and `contextengine/challenge/` |
| Discovery artwork and story | [TreasureDiscoveryReveal.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TreasureDiscoveryReveal.kt) and [TreasureStoryBook.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TreasureStoryBook.kt) |

For photo tasks, capture completion starts feedback and saving. The photo first develops into its logo,
then the task text departs and the page warms to yellow. `PhotoRevealArrival` carries page-local logo bounds
into the discovery layout so the artwork does not resize or jump during that handoff.

## Commented source index

### Application shell and theme

`app/src/main/kotlin/com/comp90018/app/`

| File | Responsibility |
| --- | --- |
| [AppShell.kt](../app/src/main/kotlin/com/comp90018/app/AppShell.kt) | Owns the signed-in layout, bottom tabs, and navigation shared between features. Creates shared profile, catalogue, collection, room, location, and feedback state before rendering a tab. |
| [AppShellViewModel.kt](../app/src/main/kotlin/com/comp90018/app/AppShellViewModel.kt) | Provides the current profile and settings to the signed-in interface. Initializes new profiles immediately and exposes profile failures for retry, independently of the selected tab. |
| [AppTheme.kt](../app/src/main/kotlin/com/comp90018/app/AppTheme.kt) | Defines the shared Compose palette, bundled font families, and typography. Change these tokens to update the visual identity used across the main application screens. |
| [AuthSessionViewModel.kt](../app/src/main/kotlin/com/comp90018/app/AuthSessionViewModel.kt) | Keeps feature ViewModels alive during one signed-in session, including activity recreation. Changing or signing out of the account clears that session store and its active listeners. |
| [MainActivity.kt](../app/src/main/kotlin/com/comp90018/app/MainActivity.kt) | Starts the Compose application and chooses the signed-out or signed-in interface. The auth listener switches flows; each signed-in user receives a separate ViewModel store. |

### Bottom navigation

`app/src/main/kotlin/com/comp90018/app/navigation/`

| File | Responsibility |
| --- | --- |
| [AppNavigation.kt](../app/src/main/kotlin/com/comp90018/app/navigation/AppNavigation.kt) | Defines the five bottom-tab destinations and their navigation-bar presentation. Edit tab labels, vector icons, unread badges, and tap feedback here. |

### Shared UI components

`app/src/main/kotlin/com/comp90018/app/ui/components/`

| File | Responsibility |
| --- | --- |
| [AppTextField.kt](../app/src/main/kotlin/com/comp90018/app/ui/components/AppTextField.kt) | Supplies the shared rounded input-field appearance used by forms. The caller owns the value; this component controls keyboard options, password masking, and field styling. |
| [ChatComposer.kt](../app/src/main/kotlin/com/comp90018/app/ui/components/ChatComposer.kt) | Renders the shared message input, send action, emoji tray, and treasure-sticker picker. The caller sends messages; local state only controls which accessory tray is visible. |
| [ChatTimestamp.kt](../app/src/main/kotlin/com/comp90018/app/ui/components/ChatTimestamp.kt) | Formats message timestamps for the chat interface using an Australian locale. Keeps same-year and older-message captions consistent across direct and team conversations. |
| [EmptyState.kt](../app/src/main/kotlin/com/comp90018/app/ui/components/EmptyState.kt) | Displays a reusable icon, title, and explanation when a feature has no content. Use this component to keep empty-list layouts consistent between screens. |
| [SensorErrorHost.kt](../app/src/main/kotlin/com/comp90018/app/ui/components/SensorErrorHost.kt) | Shows sensor failures in the app-wide snackbar host. Filters events from before this UI subscription so another signed-in session does not receive old errors. |
| [TreasureStickerCatalog.kt](../app/src/main/kotlin/com/comp90018/app/ui/components/TreasureStickerCatalog.kt) | Maps collectible treasure IDs to the names and drawable resources used by chat stickers. Update this catalogue when adding or changing a sticker shown in the picker and message bubbles. |

### Login and registration

`app/src/main/kotlin/com/comp90018/app/features/auth/`

| File | Responsibility |
| --- | --- |
| [AuthScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/auth/AuthScreen.kt) | Renders the login/register form and validates user-entered credentials. Layout and field styling live here; AuthViewModel performs the submitted authentication action. |
| [AuthViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/auth/AuthViewModel.kt) | Owns the authentication form values, login/register mode, errors, and submitting state. Repository callbacks update the state observed by AuthScreen and prevent duplicate submissions. |

### Direct chat

`app/src/main/kotlin/com/comp90018/app/features/chat/`

| File | Responsibility |
| --- | --- |
| [DirectChatScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/chat/DirectChatScreen.kt) | Renders one direct conversation, its message bubbles, header, and composer. Connects the room ViewModel to UI callbacks and collection-backed treasure stickers. |
| [DirectChatViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/chat/DirectChatViewModel.kt) | Maintains a direct conversation, its draft, message subscription, and send failures. Visibility controls read acknowledgements; confirmed writes determine when the draft is cleared. |

### Friends

`app/src/main/kotlin/com/comp90018/app/features/friends/`

| File | Responsibility |
| --- | --- |
| [FriendFinder.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendFinder.kt) | Renders friend search results, candidate profiles, and the request composer. The local page selection chooses the view; FriendFinderViewModel owns search and social actions. |
| [FriendFinderViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendFinderViewModel.kt) | Provides search results and the selected candidate for the add-friend flow. Tracks relationship/request state and exposes a direct-room ID when the user opens a conversation. |
| [FriendLabels.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendLabels.kt) | Formats names and profile labels used by friend-related screens. Also identifies legacy starter contacts so they can be excluded from live friend and chat lists. |
| [FriendProfileScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendProfileScreen.kt) | Renders a friend profile and its chat/removal actions. Displays profile-loading and action errors supplied by FriendProfileViewModel. |
| [FriendProfileViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendProfileViewModel.kt) | Loads the selected friend profile and tracks the remove-friend action. Supplies loading/error state while delegating persistence to the profile repository and removal callback. |
| [FriendRequestsContent.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendRequestsContent.kt) | Renders incoming/outgoing request history and incoming-request profile previews. Request-card layout lives here; accept/decline actions are supplied by the parent feature. |
| [FriendsHomeContent.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendsHomeContent.kt) | Renders the Friends header, Chats/Contacts selector, and list-card layouts. Edit conversation previews, unread indicators, and empty-list presentation in this file. |
| [FriendsScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendsScreen.kt) | Coordinates the Friends home, request history, finder, profile, and direct-chat views. Collects shared friend state and connects child screens to the correct social actions. |
| [FriendsViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/FriendsViewModel.kt) | Combines friend, chat-summary, and request subscriptions into the Friends UI state. Owns accept/decline/remove actions and the selected conversation target. |
| [UnreadMessagesViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/friends/UnreadMessagesViewModel.kt) | Provides the direct-message unread count used by the Friends tab badge. Keeps its subscription active for the signed-in shell rather than only while the Friends page is visible. |

### Team rooms

`app/src/main/kotlin/com/comp90018/app/features/rooms/`

| File | Responsibility |
| --- | --- |
| [RoomMemberProfileViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/rooms/RoomMemberProfileViewModel.kt) | Provides a teammate profile and relationship status for the room-member detail interface. Coordinates friend-request actions and exposes a direct-room ID for starting a private conversation. |
| [RoomPlazaScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/rooms/RoomPlazaScreen.kt) | Renders public-room browsing and the decorative room seals. RoomDiamond draws the flat compass/laurel emblem and occupied-seat accents used by room cards. |
| [RoomsScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/rooms/RoomsScreen.kt) | Selects room entry, demo or active chat. Pages and controls live in the entry/chat/hunt/members/settings/demo subdirectories; see the [directory guide](ui-directory-guide.md). |
| [RoomsViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/rooms/RoomsViewModel.kt) | Owns room membership, public-room browsing, create/join inputs, and operation errors. The entry and plaza screens observe this state and invoke its actions. |
| [TeamRoomChatViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/rooms/TeamRoomChatViewModel.kt) | Combines one room, its members, messages, and cooperative-hunt progress for the UI. All room/hunt mutations use repository callbacks, including completion and claim confirmation. |
| [UnreadRoomMessagesViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/rooms/UnreadRoomMessagesViewModel.kt) | Provides the team-message unread count for the Rooms bottom-tab badge. Its shell-owned subscription keeps the badge current while another tab is selected. |

### Profile and settings

`app/src/main/kotlin/com/comp90018/app/features/profile/`

| File | Responsibility |
| --- | --- |
| [ProfileScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/profile/ProfileScreen.kt) | Selects profile overview, editing and settings pages. The overview/edit/settings subdirectories own their contents; the shared avatar lives in ui/components. See the [directory guide](ui-directory-guide.md). |
| [UserProfile.kt](../app/src/main/kotlin/com/comp90018/app/features/profile/UserProfile.kt) | Defines profile details and preferences consumed by profile and signed-in screens. Settings also control location precision, notifications, sound effects, and haptic feedback. |

### Treasure catalogue and collection

`app/src/main/kotlin/com/comp90018/app/features/treasure/`

| File | Responsibility |
| --- | --- |
| [TreasureCatalogViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/treasure/TreasureCatalogViewModel.kt) | Provides the shared treasure catalogue, loading state, and retryable loading errors. The shell shares this catalogue with map, collection, room, and navigation interfaces. |
| [TreasureCollectionViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/treasure/TreasureCollectionViewModel.kt) | Provides collected treasure IDs and the state of an in-flight collection write. UI callbacks use the write result for retry/error presentation and confirmed collection updates. |
| [TreasureNavigationEntry.kt](../app/src/main/kotlin/com/comp90018/app/features/treasure/TreasureNavigationEntry.kt) | Derives the treasure-detail navigation action and distance caption from available location data. This is entry presentation; confirmed arrival and physical challenge eligibility remain separate. |
| [TreasureRemoteImage.kt](../app/src/main/kotlin/com/comp90018/app/features/treasure/TreasureRemoteImage.kt) | Supplies shared treasure/archive image components and drawable-key mappings. Handles remote-image caching and local fallbacks for map, collection, discovery, and story screens. |
| [TreasureScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/treasure/TreasureScreen.kt) | Renders the treasure route catalogue and discovered/undiscovered lore detail pages. Edit cards, detail sections, and navigation-entry presentation here; Map owns the live hunt flow. |

### Map and discovery/story flow

`app/src/main/kotlin/com/comp90018/app/features/map/`

| File | Responsibility |
| --- | --- |
| [CompassGateConfig.kt](../app/src/main/kotlin/com/comp90018/app/features/map/CompassGateConfig.kt) | Defines configurable distance and heading tolerances for the pre-task compass interface. These values determine when the compass may offer entry to the physical challenge. |
| [DeviceEnvironment.kt](../app/src/main/kotlin/com/comp90018/app/features/map/DeviceEnvironment.kt) | Centralizes emulator detection used by map and signed-in location presentation. Keeps emulator-specific display policy consistent between UI entry points. |
| [GuidingThreadGeometry.kt](../app/src/main/kotlin/com/comp90018/app/features/map/GuidingThreadGeometry.kt) | Calculates the geographic positions of the animated guiding-thread glints. Visual geometry travels from treasure toward explorer; it does not determine arrival or task completion. |
| [GuidingThreadUpdates.kt](../app/src/main/kotlin/com/comp90018/app/features/map/GuidingThreadUpdates.kt) | Chooses which guiding-thread overlay parts need updating on a new animation frame. Separates moving glints from line styling so animation ticks do not unnecessarily rebuild the map scene. |
| [HuntProximityStage.kt](../app/src/main/kotlin/com/comp90018/app/features/map/HuntProximityStage.kt) | Converts distance estimates into map-level unknown, far, nearby, and hunt-ready stages. The UI uses these stages for presentation; the challenge evaluator performs its own GPS confirmation. |
| [LocationActionPolicy.kt](../app/src/main/kotlin/com/comp90018/app/features/map/LocationActionPolicy.kt) | Separates coordinates that may be displayed from fixes that may enable hunt actions. Fallback or stale positions can inform the map without granting treasure completion. |
| [MapDiscoveryLogic.kt](../app/src/main/kotlin/com/comp90018/app/features/map/MapDiscoveryLogic.kt) | Contains the map distance bands, direction captions, compass readiness, and visibility policies. Pure helpers keep presentation decisions reusable and testable without rendering a map. |
| [MapRelic.kt](../app/src/main/kotlin/com/comp90018/app/features/map/MapRelic.kt) | Defines the treasure content shared by map, catalogue, rooms, discovery, and story screens. Carries text, coordinates, artwork keys, and challenge/fragment configuration loaded from the catalogue. |
| [MapScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt) | Connects map inputs and hunt actions. Route selection, ordinary map layout, Google Maps rendering, compass, team, detail, sensor and debug code live in named subdirectories; see the [directory guide](ui-directory-guide.md). |
| [PostChallengeRevealScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/map/PostChallengeRevealScreen.kt) | Renders the post-challenge treasure, optional photo-history, and story views. Uses a retained reveal session so save status and the captured-photo/artwork handoff stay consistent. |
| [PostChallengeRevealSession.kt](../app/src/main/kotlin/com/comp90018/app/features/map/PostChallengeRevealSession.kt) | Keeps one discovery handoff, its page, captured photo, save status, and artwork geometry. The coordinator reuses the existing session when repeated completion callbacks arrive. |
| [SouthLawnAssemblyScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/map/SouthLawnAssemblyScreen.kt) | Animates four Atlas fragments into the complete treasure before the team claim. Uses the original untrimmed quadrants so the assembled image matches the full Atlas artwork. |
| [SouthLawnFragmentHunt.kt](../app/src/main/kotlin/com/comp90018/app/features/map/SouthLawnFragmentHunt.kt) | Defines South Lawn fragment IDs, coordinates, and shared fragment-hunt configuration. These IDs connect the map markers and assembly interface to persisted team progress. |
| [TeamHuntClaimPrompt.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TeamHuntClaimPrompt.kt) | Decides when to show the completed-team-hunt claim prompt. Injected persistence remembers acknowledgement across map navigation and app restarts. |
| [TeamHuntLocationBinding.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TeamHuntLocationBinding.kt) | Publishes and observes teammate positions during a foreground cooperative hunt. Lives in the signed-in shell so shared locations remain available when the user changes tabs. |
| [TeamHuntLocationPolicy.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TeamHuntLocationPolicy.kt) | Filters shared teammate locations before the map renders them. Applies hunt/session membership, owner availability, and freshness requirements to shared positions. |
| [TeamHuntTaskEligibility.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TeamHuntTaskEligibility.kt) | Determines whether the current teammate should perform a task, wait, or claim the treasure. One policy keeps map prompts and hunt entry points aligned with shared completion state. |
| [TreasureChallengeRouting.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TreasureChallengeRouting.kt) | Chooses compass, teammate quiz, waiting, or claim entry for a hunt. Validates treasure identity against challenge configuration before opening a sensor task. |
| [TreasureDiscoveryReveal.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TreasureDiscoveryReveal.kt) | Renders the discovery artwork, completion heading, collection status, and story action. Photo-task arrival reuses the measured logo rectangle; story opening animates the artwork into the book. |
| [TreasureStoryBook.kt](../app/src/main/kotlin/com/comp90018/app/features/map/TreasureStoryBook.kt) | Renders the vintage paged story book, parchment surfaces, archive image, and insertion animation. Splits long story text into leaves while preserving all paragraphs and allowing leaf scrolling. |
| [UserLocationViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/map/UserLocationViewModel.kt) | Provides live device location shared by the signed-in screens. Refreshes permissions on foreground entry and owns sensor start/stop independently of the selected tab. |

### Target-focused compass navigation

`app/src/main/kotlin/com/comp90018/app/features/navigation/`

| File | Responsibility |
| --- | --- |
| [NavigationEnergy.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationEnergy.kt) | Maps distance and heading to compass energy, captions, and guiding-trail timing. Includes deterministic test coordinates for continuous navigation-slider previews. |
| [NavigationLocationReadiness.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationLocationReadiness.kt) | Derives the navigation UI explanation for unavailable, imprecise, or stale location data. Uses existing sensor metadata and controls whether the guiding thread can be displayed. |
| [NavigationMapUpdateGate.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationMapUpdateGate.kt) | Avoids redundant map-scene updates within one navigation session. Guiding-thread animation has its own frame updates and is excluded from the scene-change key. |
| [NavigationNorthReference.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationNorthReference.kt) | Converts magnetic device headings into the true-north reference used by GPS bearings. Caches geographic declination so heading animation does not repeatedly recreate the geomagnetic model. |
| [NavigationOrientationBinding.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationOrientationBinding.kt) | Connects the visible navigation screen lifecycle to orientation collection. Clears stopped output before resuming so the UI does not reuse an old heading. |
| [NavigationOrientationSession.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationOrientationSession.kt) | Owns one orientation sensor session and validates its displayed device heading. Repeated lifecycle events do not register duplicate listeners. |
| [NavigationTargetRecovery.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/NavigationTargetRecovery.kt) | Decides whether guidance should close after a requested treasure disappears. Keeps the target pending while the catalogue is still loading. |
| [RelicArrivalConfirmation.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RelicArrivalConfirmation.kt) | Confirms navigation arrival using distinct reliable location samples. Arrival enables starting a hunt; it does not complete the subsequent treasure challenge. |
| [RelicNavigationPanels.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RelicNavigationPanels.kt) | Renders the destination card, resonance description, and arrival/start-hunt action. Edit panel hierarchy, captions, and button styling here. |
| [RelicNavigationScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RelicNavigationScreen.kt) | Renders target-focused map guidance, compass/resonance panels, and movable debug controls. Coordinates location, orientation, arrival confirmation, and guiding-thread animation for one target. |
| [RelicNavigationSimulation.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RelicNavigationSimulation.kt) | Stores target-local debug distance/heading inputs and synthetic GPS fix identity. Only changed slider values create new evidence, so recomposition cannot falsely confirm arrival. |
| [RelicNavigationState.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RelicNavigationState.kt) | Defines navigation stages, direction hints, thresholds, and the state rendered by guidance UI. Combines presentation inputs while keeping arrival confirmation and challenge eligibility separate. |
| [RelicResonancePresentation.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RelicResonancePresentation.kt) | Chooses arrival status, helper text, and resonance-stage labels for guidance panels. Keeps loading/location explanations consistent with whether the hunt action is available. |
| [ResonanceVisualStyle.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/ResonanceVisualStyle.kt) | Maps resonance strength to rosette activation and guiding-thread visual emphasis. These values style the drawing; they do not grant arrival or challenge completion. |
| [RosetteResonanceGauge.kt](../app/src/main/kotlin/com/comp90018/app/features/navigation/RosetteResonanceGauge.kt) | Draws the flat compass-rosette instrument used by the navigation interface. The ring shows distance strength; petals and the arrival sweep provide separate visual cues. |

### Six green treasure tasks

`app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/`

| File | Responsibility |
| --- | --- |
| [ChallengeDiscoverySave.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeDiscoverySave.kt) | Tracks one asynchronous collection save after a task completes. Discovery navigation and completion feedback begin immediately; failed persistence remains retryable. |
| [ChallengeExperience.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeExperience.kt) | Builds the common green-task composition: title, emblem slot, measurements, and field guide. Photo tasks replace the emblem slot; departure progress moves the title up and lower content down. |
| [ChallengeSimulationSession.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationSession.kt) | Defines the debug-control UI and fake context-engine contract shared by build types. The real simulator is supplied by src/debug; production rule evaluation still determines completion. |
| [QuestArtwork.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestArtwork.kt) | Draws shared living backgrounds and the camera, fern, glasshouse, and gramophone emblems. Maps visual sensor signals and condition states to motion, gold fill, ripples, and engraved ornaments. |
| [QuestFieldGuide.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestFieldGuide.kt) | Renders the inline How to play trigger, animated parchment scroll, and task instructions. The scroll expands within the task column so surrounding content moves with it. |
| [QuestPhotoExperience.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestPhotoExperience.kt) | Implements Union Lawn and System Garden camera entry, permission, preview, shutter, and logo morph. Keeps the shutter outside the emblem slot and passes the final logo bounds to the discovery transition. |
| [QuestPhotoStyle.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestPhotoStyle.kt) | Defines photo-task artwork, frame proportions/tilt, sepia filter, and transition durations. Union uses a tilted postcard frame; Garden uses an upright glasshouse frame. |
| [QuestRequirementStars.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestRequirementStars.kt) | Displays completion conditions as animated grey-to-gold stars and readable labels. South Lawn and Wilson keep every condition in one row; combined stillness occupies one star. |
| [QuestVisualSignals.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/QuestVisualSignals.kt) | Converts valid sensor readings into bounded values used by task illustrations and captions. Visual normalization and proximity fill are presentation values, separate from completion thresholds. |
| [SouthLawnArtwork.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/SouthLawnArtwork.kt) | Draws the Atlas/globe emblem, elegant figure, distance-gilded vines, and rotating cross stars. Combined motion/shake drives agitation; heading and turn conditions control their own gold accents. |
| [TreasureChallengeScreen.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeScreen.kt) | Connects task lifecycle, simulation, completion/save callbacks, and the green-task interface. Photo tasks retain the logo while text exits and the background blends into the discovery page. |
| [TreasureChallengeViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeViewModel.kt) | Combines the context engine and rule evaluator into task, camera, condition-star, and completion state. Preserves earned photo readiness and stores the completed artwork handoff while the UI animates. |
| [WilsonFieldDynamics.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/WilsonFieldDynamics.kt) | Calculates seeded flower drift and which floating water stars the distance fill reaches. Independent seeds give each small flower its own movement channels. |
| [WilsonHallArtwork.kt](../app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/WilsonHallArtwork.kt) | Draws the rosette, independent small flowers, heading pointer, and full-screen distance-lit water. Turn and combined stillness each light four petals; heading alignment triggers an expanding ripple. |

### Haptic interaction feedback

`app/src/main/kotlin/com/comp90018/app/features/haptics/`

| File | Responsibility |
| --- | --- |
| [AndroidTreasureHapticDriver.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/AndroidTreasureHapticDriver.kt) | Adapts treasure feedback events to the Android vibration hardware. Uses supported waveform APIs and safely tolerates unavailable vibration capabilities. |
| [ConfirmedTeamClaimHaptics.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/ConfirmedTeamClaimHaptics.kt) | Adds success feedback to confirmed team-claim transactions. Session generations and attempt tokens prevent late callbacks from vibrating for a different hunt. |
| [SafeTreasureHapticDriver.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/SafeTreasureHapticDriver.kt) | Protects vibration calls behind a capability-aware hardware boundary. Lets UI feedback policies be tested without requiring a physical Android vibrator. |
| [TreasureHapticAttempt.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticAttempt.kt) | Identifies one feedback attempt within a signed-in exploration session. Token identity prevents an old asynchronous result from matching a new session. |
| [TreasureHapticController.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticController.kt) | Coordinates treasure proximity/completion feedback and suppresses duplicate vibrations. Tracks attempts across UI navigation and respects session closure, preferences, and lifecycle cancellation. |
| [TreasureHapticEvent.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticEvent.kt) | Defines semantic treasure feedback events and the driver interface that plays them. Separates UI feedback intent from the Android vibration implementation. |
| [TreasureHapticPattern.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticPattern.kt) | Defines waveform timings and amplitudes for treasure feedback. Edit these patterns to change the feel of a feedback event while preserving controller policy. |
| [TreasureHapticProximity.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticProximity.kt) | Determines whether reliable proximity data should trigger arrival feedback. This feedback policy is independent of the rules enabling Start Hunt or completing a challenge. |
| [TreasureHapticSave.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSave.kt) | Decorates collection and team-claim callbacks with confirmed feedback handling. Forwards persistence errors unchanged so the existing UI can still show and retry failed saves. |
| [TreasureHapticSessionBinding.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionBinding.kt) | Connects haptic preferences and foreground/background lifecycle to the shared feedback session. AppShell installs this binding once for the signed-in interface. |
| [TreasureHapticSessionViewModel.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticSessionViewModel.kt) | Retains the feedback controller across screen changes and activity recreation. Owns preference/lifecycle updates and closes the controller when the signed-in session is cleared. |
| [TreasureHapticTarget.kt](../app/src/main/kotlin/com/comp90018/app/features/haptics/TreasureHapticTarget.kt) | Selects the active or nearest eligible treasure for proximity feedback. Uses raw location evidence without changing map visibility or hunt eligibility. |

### Sensor context and task-rule bridge

`app/src/main/kotlin/com/comp90018/app/contextengine/`

| File | Responsibility |
| --- | --- |
| [AndroidDeviceContextEngine.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/AndroidDeviceContextEngine.kt) | Combines Android location, orientation, motion/stability, and sound into task snapshots. The UI receives one coherent stream instead of coordinating each hardware listener itself. |
| [DeviceContextEngine.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/DeviceContextEngine.kt) | Defines the combined device-context stream consumed by task ViewModels. Target setters configure sensing; start/stop follow the visible task lifecycle. |
| [DeviceContextSnapshot.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/DeviceContextSnapshot.kt) | Defines the combined sensor input read by task rules and visual-signal conversion. Contains validity and timing metadata as well as the measurements used by the artwork. |
| [FakeDeviceContextEngine.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/FakeDeviceContextEngine.kt) | Provides controllable context snapshots for task previews, debug sliders, and tests. Emits the same output contract as the Android engine so presentation and rules can be exercised together. |
| [challenge/ChallengeConfigValidation.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/challenge/ChallengeConfigValidation.kt) | Checks that each challenge configuration contains its required task rules. Invalid catalogue rules must not open a task whose UI could suggest an unattainable completion. |
| [challenge/ChallengeModels.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/challenge/ChallengeModels.kt) | Defines task types, required conditions, instructions, configuration, and evaluator progress. These contracts connect rule evaluation to the stars, captions, and camera states shown by the UI. |
| [challenge/ChallengePoise.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/challenge/ChallengePoise.kt) | Combines valid motion/shake measurements into stillness and normalizes turn movement. The same scoring drives South Lawn/Wilson condition checks and their visual agitation. |
| [challenge/ChallengeRuleEvaluator.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/challenge/ChallengeRuleEvaluator.kt) | Evaluates location, heading, posture, movement, rotation, sound, and photo completion rules. Produces the condition states rendered as stars; drawing code does not decide whether a task is completed. |
| [challenge/RelicChallengeConfigs.kt](../app/src/main/kotlin/com/comp90018/app/contextengine/challenge/RelicChallengeConfigs.kt) | Supplies default rule configurations for the six treasure challenges. Actual catalogue configurations are validated before use; visual appearance is defined in treasurechallenge. |

### Camera adapter

`app/src/main/kotlin/com/comp90018/app/sensors/camera/`

| File | Responsibility |
| --- | --- |
| [CameraCapture.kt](../app/src/main/kotlin/com/comp90018/app/sensors/camera/CameraCapture.kt) | Defines preview binding, photo capture, and camera cleanup for the task interface. UI callers receive a captured URI or an error without owning CameraX implementation details. |
| [CameraXCapture.kt](../app/src/main/kotlin/com/comp90018/app/sensors/camera/CameraXCapture.kt) | Binds CameraX preview/capture to the visible lifecycle and returns captured photo URIs. The photo UI owns permission prompts, filtering, and animation; this adapter owns the camera operations. |
| [QrCodeAnalyzer.kt](../app/src/main/kotlin/com/comp90018/app/sensors/camera/QrCodeAnalyzer.kt) | Decodes QR values from camera frames when an analysis callback is configured. This camera analysis helper is separate from the photo-task logo and reveal animations. |

### User-visible sensor errors

`app/src/main/kotlin/com/comp90018/app/diagnostics/`

| File | Responsibility |
| --- | --- |
| [SensorComponent.kt](../app/src/main/kotlin/com/comp90018/app/diagnostics/SensorComponent.kt) | Names hardware/system components in user-visible sensor error messages. SensorErrorHost uses these display names when formatting snackbar text. |
| [SensorErrorEvent.kt](../app/src/main/kotlin/com/comp90018/app/diagnostics/SensorErrorEvent.kt) | Carries the failed component, message, and timestamp to the app-wide error UI. The timestamp lets a newly mounted snackbar host ignore earlier-session events. |
| [SensorErrorReporter.kt](../app/src/main/kotlin/com/comp90018/app/diagnostics/SensorErrorReporter.kt) | Publishes sensor failures for the app-wide snackbar interface. Hardware adapters report here instead of displaying their own feature-specific error dialogs. |

### Debug controls and fixtures

`app/src/debug/`

| File | Responsibility |
| --- | --- |
| [kotlin/com/comp90018/app/features/map/DebugChallengeRelics.kt](../app/src/debug/kotlin/com/comp90018/app/features/map/DebugChallengeRelics.kt) | Supplies the six locally configured treasures used by the map task-preview launcher. These fixtures allow artwork/interaction previews without loading or modifying the cloud catalogue. |
| [kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationFactory.kt](../app/src/debug/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationFactory.kt) | Builds the continuous TEST sliders, calibration panel, and fake sensor engine for task previews. Slider values feed the production evaluator; edit control layout and simulated measurements here. |

### Release counterparts

`app/src/release/`

| File | Responsibility |
| --- | --- |
| [kotlin/com/comp90018/app/features/map/DebugChallengeRelics.kt](../app/src/release/kotlin/com/comp90018/app/features/map/DebugChallengeRelics.kt) | Provides the release counterpart of the map preview catalogue. Returns no development-only challenge fixtures. |
| [kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationFactory.kt](../app/src/release/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationFactory.kt) | Provides the release counterpart of task simulation controls. The factory supplies no simulator and the calibration panel renders no debug interface. |

### Device and Compose UI checks

`app/src/androidTest/`

| File | Responsibility |
| --- | --- |
| [kotlin/com/comp90018/app/features/auth/AuthScreenTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/auth/AuthScreenTest.kt) | Checks authentication form presentation, input handling, and validation with a fake repository. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/haptics/HapticSessionBindingTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/haptics/HapticSessionBindingTest.kt) | Checks that the Compose/lifecycle binding applies preferences and cancels feedback at the correct time. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/map/TreasureCompassGateUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/map/TreasureCompassGateUiTest.kt) | Checks the compass gate presentation and the distance/heading conditions that expose task entry. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/navigation/RelicNavigationPanelsUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/navigation/RelicNavigationPanelsUiTest.kt) | Checks guidance-panel captions, resonance presentation, and the arrival/start-hunt action. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/rooms/RoomClaimHapticUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/rooms/RoomClaimHapticUiTest.kt) | Checks feedback for confirmed room treasure claims through the real UI entry point. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/treasurechallenge/QuestFieldGuideUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/treasurechallenge/QuestFieldGuideUiTest.kt) | Checks the inline scroll, living sensor artwork, TEST controls, condition stars, and automatic discovery feedback. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/treasurechallenge/SystemGardenPhotoUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/treasurechallenge/SystemGardenPhotoUiTest.kt) | Checks glasshouse camera readiness, the external shutter, upright photo-to-logo morph, and discovery handoff. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeUiTest.kt) | Checks challenge state presentation and sensor/completion interactions using controllable device context. Run these device/Compose checks when changing the corresponding interface or interaction contract. |
| [kotlin/com/comp90018/app/features/treasurechallenge/UnionLawnPhotoUiTest.kt](../app/src/androidTest/kotlin/com/comp90018/app/features/treasurechallenge/UnionLawnPhotoUiTest.kt) | Checks camera unlocking, the external shutter, sepia postcard morph, and continuous discovery handoff. Run these device/Compose checks when changing the corresponding interface or interaction contract. |

## Visual assets and resource configuration

Bitmap/font files are binary and the raw data files use JSON, so their responsibilities are indexed here.
Vector XML, window-theme XML, the manifest, and build scripts contain inline English comments.

| Resource | Used for |
| --- | --- |
| [drawable-nodpi/historical_grainger_tone_tool_1952.jpg](../app/src/main/res/drawable-nodpi/historical_grainger_tone_tool_1952.jpg) | Bundled historical scene used by archive/story image presentation. |
| [drawable-nodpi/historical_old_quad_fossil_1875.jpg](../app/src/main/res/drawable-nodpi/historical_old_quad_fossil_1875.jpg) | Bundled historical scene used by archive/story image presentation. |
| [drawable-nodpi/historical_south_lawn_atlas_1900.jpg](../app/src/main/res/drawable-nodpi/historical_south_lawn_atlas_1900.jpg) | Bundled historical scene used by archive/story image presentation. |
| [drawable-nodpi/historical_system_garden.jpg](../app/src/main/res/drawable-nodpi/historical_system_garden.jpg) | Bundled historical scene used by archive/story image presentation. |
| [drawable-nodpi/historical_union_lake_1936.jpg](../app/src/main/res/drawable-nodpi/historical_union_lake_1936.jpg) | Bundled historical scene used by archive/story image presentation. |
| [drawable-nodpi/historical_wilson_hall_fire_1952.jpg](../app/src/main/res/drawable-nodpi/historical_wilson_hall_fire_1952.jpg) | Bundled historical scene used by archive/story image presentation. |
| [drawable-nodpi/map_quest_available.png](../app/src/main/res/drawable-nodpi/map_quest_available.png) | Available treasure map-marker artwork. |
| [drawable-nodpi/map_quest_selected.png](../app/src/main/res/drawable-nodpi/map_quest_selected.png) | Selected treasure map-marker artwork. |
| [drawable-nodpi/nav_friends_game.png](../app/src/main/res/drawable-nodpi/nav_friends_game.png) | Bundled raster navigation artwork; the current bottom-tab mapping uses the symbol vectors. |
| [drawable-nodpi/nav_map_game.png](../app/src/main/res/drawable-nodpi/nav_map_game.png) | Bundled raster navigation artwork; the current bottom-tab mapping uses the symbol vectors. |
| [drawable-nodpi/nav_profile_game.png](../app/src/main/res/drawable-nodpi/nav_profile_game.png) | Bundled raster navigation artwork; the current bottom-tab mapping uses the symbol vectors. |
| [drawable-nodpi/nav_rooms_game.png](../app/src/main/res/drawable-nodpi/nav_rooms_game.png) | Bundled raster navigation artwork; the current bottom-tab mapping uses the symbol vectors. |
| [drawable-nodpi/nav_treasure_game.png](../app/src/main/res/drawable-nodpi/nav_treasure_game.png) | Bundled raster navigation artwork; the current bottom-tab mapping uses the symbol vectors. |
| [drawable-nodpi/treasure_atlas.png](../app/src/main/res/drawable-nodpi/treasure_atlas.png) | South Lawn complete Atlas collectible artwork. |
| [drawable-nodpi/treasure_atlas_fragment_north_east.png](../app/src/main/res/drawable-nodpi/treasure_atlas_fragment_north_east.png) | Untrimmed Atlas quadrant used by the team assembly animation. |
| [drawable-nodpi/treasure_atlas_fragment_north_west.png](../app/src/main/res/drawable-nodpi/treasure_atlas_fragment_north_west.png) | Untrimmed Atlas quadrant used by the team assembly animation. |
| [drawable-nodpi/treasure_atlas_fragment_south_east.png](../app/src/main/res/drawable-nodpi/treasure_atlas_fragment_south_east.png) | Untrimmed Atlas quadrant used by the team assembly animation. |
| [drawable-nodpi/treasure_atlas_fragment_south_west.png](../app/src/main/res/drawable-nodpi/treasure_atlas_fragment_south_west.png) | Untrimmed Atlas quadrant used by the team assembly animation. |
| [drawable-nodpi/treasure_emoji_atlas.png](../app/src/main/res/drawable-nodpi/treasure_emoji_atlas.png) | Collectible treasure sticker artwork for chat presentation. |
| [drawable-nodpi/treasure_emoji_fern.png](../app/src/main/res/drawable-nodpi/treasure_emoji_fern.png) | Collectible treasure sticker artwork for chat presentation. |
| [drawable-nodpi/treasure_emoji_glasshouse.png](../app/src/main/res/drawable-nodpi/treasure_emoji_glasshouse.png) | Collectible treasure sticker artwork for chat presentation. |
| [drawable-nodpi/treasure_emoji_postcard.png](../app/src/main/res/drawable-nodpi/treasure_emoji_postcard.png) | Collectible treasure sticker artwork for chat presentation. |
| [drawable-nodpi/treasure_emoji_press.png](../app/src/main/res/drawable-nodpi/treasure_emoji_press.png) | Collectible treasure sticker artwork for chat presentation. |
| [drawable-nodpi/treasure_emoji_rosette.png](../app/src/main/res/drawable-nodpi/treasure_emoji_rosette.png) | Collectible treasure sticker artwork for chat presentation. |
| [drawable-nodpi/treasure_fern.png](../app/src/main/res/drawable-nodpi/treasure_fern.png) | Old Quad fossil/fern collectible artwork. |
| [drawable-nodpi/treasure_glasshouse.png](../app/src/main/res/drawable-nodpi/treasure_glasshouse.png) | System Garden revealed glasshouse; the photo frame morphs upright. |
| [drawable-nodpi/treasure_postcard.png](../app/src/main/res/drawable-nodpi/treasure_postcard.png) | Union Lawn revealed lake postcard; the photo frame morphs to its tilted inset. |
| [drawable-nodpi/treasure_press.png](../app/src/main/res/drawable-nodpi/treasure_press.png) | Grainger collectible artwork. |
| [drawable-nodpi/treasure_rosette.png](../app/src/main/res/drawable-nodpi/treasure_rosette.png) | Wilson Hall collectible artwork. |
| [drawable-nodpi/treasure_unknown.png](../app/src/main/res/drawable-nodpi/treasure_unknown.png) | Unrevealed or unavailable treasure-image fallback. |
| [drawable/nav_friends_symbol.xml](../app/src/main/res/drawable/nav_friends_symbol.xml) | Friends-tab vector artwork. Path fill colours and viewport coordinates define the flat icon. |
| [drawable/nav_map_symbol.xml](../app/src/main/res/drawable/nav_map_symbol.xml) | Map-tab vector artwork. The pin silhouette and inner disc scale together within the viewport. |
| [drawable/nav_profile_symbol.xml](../app/src/main/res/drawable/nav_profile_symbol.xml) | Profile-tab vector artwork, referenced by the shared bottom navigation. |
| [drawable/nav_rooms_symbol.xml](../app/src/main/res/drawable/nav_rooms_symbol.xml) | Rooms-tab vector artwork, referenced by the shared bottom navigation and room-related UI. |
| [drawable/nav_treasure_symbol.xml](../app/src/main/res/drawable/nav_treasure_symbol.xml) | Treasure-tab vector artwork, also reused by treasure-related UI controls. |
| [drawable/treasure_chest_symbol.xml](../app/src/main/res/drawable/treasure_chest_symbol.xml) | Reusable treasure-chest vector artwork. Edit paths and fill colours here for this resource. |
| [drawable/map_arrived_symbol.xml](../app/src/main/res/drawable/map_arrived_symbol.xml) | Arrival-symbol vector artwork for map/navigation presentation. |
| [font/fredoka_variable.ttf](../app/src/main/res/font/fredoka_variable.ttf) | Rounded heading font referenced by AppTheme.kt. |
| [font/nunito_bold.ttf](../app/src/main/res/font/nunito_bold.ttf) | Body font currently referenced by AppTheme.kt. |
| [font/nunito_variable.ttf](../app/src/main/res/font/nunito_variable.ttf) | Bundled Nunito variable-font asset; current shared body typography uses nunito_bold. |
| [font/unifraktur_cook_bold.ttf](../app/src/main/res/font/unifraktur_cook_bold.ttf) | Gothic treasure/quest heading font. |
| [app/src/main/res/raw/map_style_retro.json](../app/src/main/res/raw/map_style_retro.json) | Google Maps colour/style rules for the vintage map appearance. |
| [app/src/main/res/raw/historical_image_sources.json](../app/src/main/res/raw/historical_image_sources.json) | Source/credit metadata for the bundled archive photographs. |
| [app/src/main/res/values/themes.xml](../app/src/main/res/values/themes.xml) | Android window and status-bar defaults surrounding Compose content. |
| [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml) | Launcher activity, keyboard resizing, permissions, optional hardware, and Maps key placeholder. |
| [app/build.gradle.kts](../app/build.gradle.kts) | Compose/CameraX/Maps/Firebase dependencies, build variants, and UI test support. |
| [build.gradle.kts](../build.gradle.kts) | Shared build-plugin versions. |
| [settings.gradle.kts](../settings.gradle.kts) | Dependency/plugin repositories and the :app module. |

## Kotlin import lines

`import` makes a class, function, or extension available by its short name in the current file.
Compose files import layout primitives, Material components, state/effect helpers, and drawing types.
`getValue` and `setValue` support delegated state such as `var open by remember { mutableStateOf(false) }`.
The app build script declares library dependencies; import lines reference their APIs.
