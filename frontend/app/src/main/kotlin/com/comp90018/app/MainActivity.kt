package com.comp90018.app

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Comp90018App() }
    }
}

private val Brand = Color(0xFF4B5FE7)
private val BrandSoft = Color(0xFFE9ECFF)
private val Background = Color(0xFFF7F8FC)
private val Ink = Color(0xFF18203A)
private val Muted = Color(0xFF7B8198)

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
                    if (loginMode) "欢迎回来" else "创建账号",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    if (loginMode) "使用 Firebase 登录" else "注册后将自动创建 Firestore 用户资料",
                    color = Muted,
                )
                AppTextField("邮箱", email, { email = it }, keyboardType = KeyboardType.Email)
                AppTextField(
                    "密码（至少 6 位）",
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
                    Text(if (submitting) "正在处理…" else if (loginMode) "登录" else "创建账号")
                }
                TextButton(
                    onClick = { loginMode = !loginMode; error = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (loginMode) "没有账号？去注册" else "已有账号？去登录", color = Brand)
                }
            }
        }
    }
}

private fun validateCredentials(email: String, password: String): String? = when {
    email.isBlank() || password.isBlank() -> "请输入邮箱和密码"
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "请输入有效的邮箱地址"
    password.length < 6 -> "密码至少需要 6 位"
    else -> null
}

@Composable
private fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> ({ Icon(icon, contentDescription = null) }) },
        singleLine = true,
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

private data class UserProfile(
    val uid: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String,
)

private enum class AppDestination(val label: String, val icon: ImageVector) {
    Home("首页", Icons.Rounded.Home),
    Search("搜索", Icons.Rounded.Search),
    Friends("好友", Icons.Rounded.Group),
    Profile("我的", Icons.Rounded.Person),
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
                profileError = exception.localizedMessage ?: "无法读取 Firestore 用户资料"
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
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "退出登录", tint = Muted)
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
                AppDestination.Home -> HomeScreen(profile, user)
                AppDestination.Search -> SearchScreen()
                AppDestination.Friends -> FriendsScreen(user, firestore, profile)
                AppDestination.Profile -> ProfileScreen(
                    user, profile, profileError,
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
private fun HomeScreen(profile: UserProfile?, user: FirebaseUser) {
    val name = profile?.displayName?.takeIf { it.isNotBlank() }
        ?: profile?.username?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore('@').orEmpty()
    Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("你好，$name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        Text("今天想认识谁？从搜索开始发现新朋友。", color = Muted)
        FeatureCard(Icons.Rounded.Search, "发现新朋友", "使用邮箱或用户名查找用户")
        FeatureCard(Icons.Rounded.Group, "好友动态", "好友功能将在这里显示")
    }
}

@Composable
private fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("寻找朋友", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        Text("输入邮箱或用户名进行搜索", color = Muted)
        AppTextField("搜索用户", query, { query = it }, leadingIcon = Icons.Rounded.Search)
        EmptyState(
            Icons.Rounded.Search,
            if (query.isBlank()) "开始搜索" else "正在查找“$query”",
            if (query.isBlank()) "找到用户后，可以向对方发送好友请求" else "用户搜索功能将在下一步连接 Firestore",
        )
    }
}

@Composable
private fun FriendsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?) {
    var addFriendMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchUser>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var requestedUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var roomId by remember { mutableStateOf<String?>(null) }
    var creatingRoom by remember { mutableStateOf(false) }

    if (roomId != null) {
        ChatRoomScreen(
            firestore = firestore,
            roomId = requireNotNull(roomId),
            currentUid = user.uid,
            title = "${profile?.username?.ifBlank { "我的" } ?: "我的"}房间",
            onBack = { roomId = null },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { addFriendMode = !addFriendMode; message = null },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("加好友")
            }
            Button(
                onClick = {
                    creatingRoom = true
                    message = null
                    FirebaseSocialService.createRoom(
                        firestore,
                        user.uid,
                        profile?.username.orEmpty(),
                    ) { createdRoomId, error ->
                        creatingRoom = false
                        message = error
                        roomId = createdRoomId
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !creatingRoom,
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (creatingRoom) "创建中…" else "创建房间")
            }
        }

        if (addFriendMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("按用户名查找", fontWeight = FontWeight.Bold, color = Ink)
                    AppTextField("输入用户名", query, { query = it }, leadingIcon = Icons.Rounded.Search)
                    Button(
                        onClick = {
                            searching = true
                            message = null
                            FirebaseSocialService.searchUsers(firestore, user.uid, query) { users, error ->
                                searching = false
                                results = users
                                message = error ?: if (users.isEmpty()) "没有找到用户" else null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !searching,
                    ) { Text(if (searching) "搜索中…" else "搜索") }
                }
            }
        }

        message?.let { Text(it, color = if (it.contains("成功")) Brand else MaterialTheme.colorScheme.error) }

        if (results.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = { it.uid }) { foundUser ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(BrandSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(foundUser.username.take(1).uppercase(), color = Brand, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(foundUser.username, fontWeight = FontWeight.Bold, color = Ink)
                                foundUser.displayName.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted) }
                            }
                            Button(
                                onClick = {
                                    FirebaseSocialService.sendFriendRequest(
                                        firestore, user.uid, foundUser.uid,
                                    ) { error ->
                                        if (error == null) {
                                            requestedUids = requestedUids + foundUser.uid
                                            message = "好友请求发送成功"
                                        } else message = error
                                    }
                                },
                                enabled = foundUser.uid !in requestedUids,
                            ) { Text(if (foundUser.uid in requestedUids) "已发送" else "添加") }
                        }
                    }
                }
            }
        } else if (!addFriendMode) {
            Text("我的好友", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            EmptyState(Icons.Rounded.Group, "还没有好友", "点击上方“加好友”，通过用户名寻找朋友")
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text("房间号 ${roomId.take(8)}", color = Muted, style = MaterialTheme.typography.bodySmall)
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
                    Text(error ?: "房间已创建，发送第一条消息吧", color = Muted)
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
            AppTextField("输入消息", input, { input = it }, leadingIcon = Icons.AutoMirrored.Rounded.Chat)
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
            Text("发送消息")
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

@Composable
private fun ProfileScreen(
    user: FirebaseUser,
    profile: UserProfile?,
    profileError: String?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    val email = profile?.email ?: user.email.orEmpty()
    Column(
        Modifier.fillMaxSize().padding(top = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Brand),
            contentAlignment = Alignment.Center,
        ) {
            Text(email.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            profile?.displayName?.ifBlank { email } ?: email,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(email, color = Muted)
        profile?.username?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text("@$it", color = Brand, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    profileError != null -> {
                        Text(profileError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text("重试") }
                    }
                    profile == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Brand, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("正在读取用户资料…", color = Muted)
                    }
                    else -> {
                        Text("Cloud Firestore 已连接", color = Brand, fontWeight = FontWeight.SemiBold)
                        Text("用户 UID", color = Muted, style = MaterialTheme.typography.labelMedium)
                        Text(profile.uid, color = Ink)
                        profile.bio.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted) }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("退出登录")
        }
    }
}
