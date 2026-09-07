package com.comp90018.app

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Comp90018App() }
    }
}

internal val Brand = Color(0xFF4B5FE7)
internal val BrandSoft = Color(0xFFE9ECFF)
internal val Background = Color(0xFFF7F8FC)
internal val Ink = Color(0xFF18203A)
internal val Muted = Color(0xFF7B8198)

@Composable
private fun Comp90018App() {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    MaterialTheme {
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            if (user == null) AuthScreen(auth)
            else LoggedInApp(requireNotNull(user), firestore, onLogout = auth::signOut)
        }
    }
}

@Composable
private fun AuthScreen(auth: FirebaseAuth) {
    var loginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("COMP90018", style = MaterialTheme.typography.labelLarge, color = Brand)
                Text(
                    if (loginMode) "Welcome back" else "Create account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    if (loginMode) "Sign in with Firebase" else "A Firestore profile will be created automatically.",
                    color = Muted,
                )
                AppTextField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                AppTextField(
                    "Password (at least 6 characters)",
                    password,
                    { password = it },
                    password = true,
                    keyboardType = KeyboardType.Password,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        error = validateCredentials(email, password)
                        if (error != null) return@Button
                        submitting = true
                        val onComplete: (String?) -> Unit = { message ->
                            submitting = false
                            error = message
                        }
                        if (loginMode) FirebaseAuthService.login(auth, email, password, onComplete)
                        else FirebaseAuthService.register(auth, email, password, onComplete)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) {
                    Text(if (submitting) "Please wait…" else if (loginMode) "Sign in" else "Create account")
                }
                TextButton(
                    onClick = { loginMode = !loginMode; error = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (loginMode) "No account? Create one" else "Already have an account? Sign in", color = Brand)
                }
            }
        }
    }
}

private fun validateCredentials(email: String, password: String): String? = when {
    email.isBlank() || password.isBlank() -> "Enter your email and password"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
    password.length < 6 -> "Password must be at least 6 characters"
    else -> null
}

@Composable
internal fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> ({ Icon(icon, contentDescription = null) }) },
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand,
            focusedLabelColor = Brand,
            focusedLeadingIconColor = Brand,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

private enum class AppDestination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Search("Search", Icons.Rounded.Search),
    Friends("Friends", Icons.Rounded.Group),
    Profile("Profile", Icons.Rounded.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggedInApp(user: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf(AppDestination.Search) }
    var profile by remember(user.uid) { mutableStateOf<UserProfile?>(null) }
    var profileError by remember(user.uid) { mutableStateOf<String?>(null) }
    var reloadKey by remember(user.uid) { mutableStateOf(0) }
    var creatingProfile by remember(user.uid) { mutableStateOf(false) }

    DisposableEffect(user.uid, firestore, reloadKey) {
        val reference = firestore.collection("users").document(user.uid)
        val registration = reference.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                profileError = exception.localizedMessage ?: "Unable to load your Firestore profile"
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                profileError = null
                val username = snapshot.getString("username").orEmpty()
                profile = UserProfile(
                    uid = user.uid,
                    email = snapshot.getString("email") ?: user.email.orEmpty(),
                    username = username,
                    displayName = snapshot.getString("displayName").orEmpty(),
                    gender = snapshot.getString("gender") ?: "unspecified",
                    bio = snapshot.getString("bio").orEmpty(),
                )
                if (username.isBlank() && !creatingProfile) {
                    creatingProfile = true
                    FirebaseAuthService.ensureProfile(user, firestore) { message ->
                        creatingProfile = false
                        profileError = message
                    }
                }
            } else if (!creatingProfile) {
                creatingProfile = true
                FirebaseAuthService.ensureProfile(user, firestore) { message ->
                    creatingProfile = false
                    profileError = message
                }
            }
        }
        onDispose { registration.remove() }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("COMP90018", color = Brand, style = MaterialTheme.typography.labelMedium)
                        Text(destination.label, color = Ink, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (destination == AppDestination.Profile) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out", tint = Muted)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        bottomBar = { AppBottomNavigation(destination) { destination = it } },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp)) {
            when (destination) {
                AppDestination.Home -> HomeScreen()
                AppDestination.Search -> SearchScreen()
                AppDestination.Friends -> FriendsScreen(user, firestore, profile)
                AppDestination.Profile -> ProfileScreen(
                    user, firestore, profile, profileError,
                    onRetry = { reloadKey += 1 },
                    onLogout = onLogout,
                )
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(selected: AppDestination, onSelected: (AppDestination) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 10.dp) {
        AppDestination.entries.forEach { destination ->
            val isSearch = destination == AppDestination.Search
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Box(
                        modifier = if (isSearch) {
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(if (selected == destination) Brand else BrandSoft)
                        } else Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label,
                            tint = when {
                                isSearch && selected == destination -> Color.White
                                selected == destination -> Brand
                                else -> Muted
                            },
                        )
                    }
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun HomeScreen() = Box(Modifier.fillMaxSize())

@Composable
private fun SearchScreen() = Box(Modifier.fillMaxSize())

@Composable
private fun FriendFinder(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    currentUsername: String,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<SearchUser?>(null) }
    var friendship by remember { mutableStateOf<FriendshipStatus?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var actionInProgress by remember { mutableStateOf(false) }
    var roomId by remember { mutableStateOf<String?>(null) }

    if (roomId != null) {
        ChatRoomScreen(
            firestore = firestore,
            roomId = requireNotNull(roomId),
            currentUid = user.uid,
            title = target?.username ?: "Chat",
            onBack = { roomId = null },
        )
        return
    }

    if (target != null) {
        val foundUser = requireNotNull(target)
        Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { target = null; friendship = null; message = null }) { Text("Search again") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) { Text(foundUser.username.take(1).uppercase(), color = Brand, style = MaterialTheme.typography.displaySmall) }
                    Text(foundUser.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Text(genderLabel(foundUser.gender), color = Muted)
                    foundUser.bio.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink, textAlign = TextAlign.Center) }
                }
            }
            Spacer(Modifier.weight(1f))
            message?.let {
                Text(
                    it,
                    color = if (it == "Friend request sent" || it == "Friend request accepted") Brand
                    else MaterialTheme.colorScheme.error,
                )
            }
            when (friendship) {
                FriendshipStatus.Friends -> Button(
                    onClick = {
                        actionInProgress = true
                        FirebaseSocialService.openDirectRoom(firestore, user.uid, foundUser.uid, foundUser.username) { id, error ->
                            actionInProgress = false
                            roomId = id
                            message = error
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (actionInProgress) "Opening chat…" else "Chat") }
                FriendshipStatus.IncomingPending -> Button(
                    onClick = {
                        actionInProgress = true
                        FirebaseSocialService.acceptFriendRequest(firestore, foundUser.uid, user.uid) { error ->
                            actionInProgress = false
                            friendship = if (error == null) FriendshipStatus.Friends else friendship
                            message = error ?: "Friend request accepted"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (actionInProgress) "Processing…" else "Accept request") }
                FriendshipStatus.OutgoingPending -> Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                ) { Text("Friend request sent") }
                FriendshipStatus.None -> Button(
                    onClick = {
                        actionInProgress = true
                        FirebaseSocialService.sendFriendRequest(firestore, user.uid, foundUser.uid, currentUsername) { error ->
                            actionInProgress = false
                            friendship = if (error == null) FriendshipStatus.OutgoingPending else friendship
                            message = error ?: "Friend request sent"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (actionInProgress) "Sending…" else "Add friend") }
                null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Brand)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Text("Find friends", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text("Search by username", color = Muted)
            AppTextField("Username", query, { query = it }, leadingIcon = Icons.Rounded.Search)
            Button(
                onClick = {
                    searching = true
                    message = null
                    FirebaseSocialService.findUserByUsername(firestore, query) { foundUser, error ->
                        searching = false
                        if (error != null) {
                            message = error
                            return@findUserByUsername
                        }
                        // Searching for the signed-in account intentionally leaves this page empty.
                        if (foundUser == null || foundUser.uid == user.uid) return@findUserByUsername
                        target = foundUser
                        FirebaseSocialService.getFriendshipStatus(firestore, user.uid, foundUser.uid) { status, statusError ->
                            friendship = status
                            message = statusError
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !searching,
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
            ) { Text(if (searching) "Searching…" else "Search") }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun genderLabel(gender: String): String = when (gender) {
    "male" -> "Male"
    "female" -> "Female"
    "prefer_not_to_say" -> "Prefer not to say"
    else -> "Not specified"
}

@Composable
private fun FriendsScreen(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
) {
    var addFriendMode by remember { mutableStateOf(false) }
    var showFriendRequests by remember { mutableStateOf(false) }
    var incomingRequests by remember { mutableStateOf<List<IncomingFriendRequest>>(emptyList()) }
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var requestMessage by remember { mutableStateOf<String?>(null) }

    if (addFriendMode) {
        FriendFinder(
            user = user,
            firestore = firestore,
            currentUsername = profile?.username.orEmpty(),
            onClose = { addFriendMode = false },
        )
        return
    }

    DisposableEffect(user.uid, firestore) {
        val requestRegistration = FirebaseSocialService.observeIncomingFriendRequests(firestore, user.uid) { requests, error ->
            incomingRequests = requests
            requestMessage = error
        }
        val friendRegistration = FirebaseSocialService.observeFriends(firestore, user.uid) { updatedFriends, error ->
            friends = updatedFriends
            if (error != null) requestMessage = error
        }
        onDispose {
            requestRegistration.remove()
            friendRegistration.remove()
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { addFriendMode = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add friend")
        }
        requestMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Friends",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            TextButton(onClick = { showFriendRequests = !showFriendRequests }) {
                Text(
                    if (incomingRequests.isEmpty()) "Friend requests"
                    else "Friend requests (${incomingRequests.size})",
                )
            }
        }
        if (showFriendRequests) {
            if (incomingRequests.isEmpty()) {
                EmptyState(Icons.Rounded.Group, "No friend requests", "New requests will appear here.")
            } else {
            Text("Friend requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            incomingRequests.forEach { request ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(request.fromUsername, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                        TextButton(onClick = {
                            FirebaseSocialService.declineFriendRequest(firestore, request.fromUid, user.uid) { requestMessage = it }
                        }) { Text("Decline") }
                        Button(onClick = {
                            FirebaseSocialService.acceptFriendRequest(firestore, request.fromUid, user.uid) { requestMessage = it }
                        }) { Text("Accept") }
                    }
                }
            }
            }
        } else {
            if (friends.isEmpty()) {
                EmptyState(Icons.Rounded.Group, "No friends yet", "Tap Add friend to search by username.")
            } else {
                friends.forEach { friend ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(friend.username, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                            TextButton(onClick = {
                                FirebaseSocialService.removeFriend(firestore, user.uid, friend.uid) { error ->
                                    requestMessage = error ?: "Friend removed"
                                }
                            }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRoomScreen(
    firestore: FirebaseFirestore,
    roomId: String,
    currentUid: String,
    title: String,
    onBack: () -> Unit,
) {
    var messages by remember(roomId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember(roomId) { mutableStateOf("") }
    var error by remember(roomId) { mutableStateOf<String?>(null) }

    DisposableEffect(roomId, firestore) {
        val registration = FirebaseSocialService.observeMessages(firestore, roomId) { newMessages, newError ->
            messages = newMessages
            error = newError
        }
        onDispose { registration.remove() }
    }

    Column(Modifier.fillMaxSize().padding(top = 10.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text("Room ${roomId.take(8)}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "Start the conversation.", color = Muted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(messages, key = { it.id }) { chatMessage ->
                        val mine = chatMessage.senderId == currentUid
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                color = if (mine) Brand else BrandSoft,
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text(
                                    chatMessage.text,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = if (mine) Color.White else Ink,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppTextField("Message", input, { input = it }, leadingIcon = Icons.AutoMirrored.Rounded.Chat)
        }
        Button(
            onClick = {
                val sending = input
                input = ""
                FirebaseSocialService.sendMessage(firestore, roomId, currentUid, sending) { error = it }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Send")
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Brand) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Brand, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(18.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Muted, textAlign = TextAlign.Center)
        }
    }
}
