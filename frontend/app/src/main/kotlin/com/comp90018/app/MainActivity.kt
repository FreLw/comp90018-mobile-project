package com.comp90018.app

import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent { Comp90018App() }
	}
}

private val Brand = Color(0xFF4659D6)
private val Background = Color(0xFFF7F8FC)

@Composable
private fun Comp90018App() {
	MaterialTheme {
		Surface(color = Background, modifier = Modifier.fillMaxSize()) {
			var user by remember { mutableStateOf<UserProfile?>(null) }
			if (user == null) AuthScreen(onAuthenticated = { user = it })
			else ProfileScreen(user = requireNotNull(user), onLogout = { user = null })
		}
	}
}

@Composable
private fun AuthScreen(onAuthenticated: (UserProfile) -> Unit) {
	var loginMode by remember { mutableStateOf(true) }
	var account by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }
	var avatarUrl by remember { mutableStateOf("") }
	var bio by remember { mutableStateOf("") }
	var gender by remember { mutableStateOf("UNSPECIFIED") }
	var error by remember { mutableStateOf<String?>(null) }
	var submitting by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
		Card(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(28.dp),
			colors = CardDefaults.cardColors(containerColor = Color.White),
		) {
			Column(
				modifier = Modifier.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(14.dp),
			) {
				Text("COMP90018", style = MaterialTheme.typography.labelLarge, color = Brand)
				Text(if (loginMode) "欢迎回来" else "创建账号", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
				Text(if (loginMode) "登录以继续使用应用" else "注册后即可使用本地服务", color = Color.Gray)

				AppTextField("账号", account, { account = it })
				AppTextField("密码（至少 8 位）", password, { password = it }, password = true)

				if (!loginMode) {
					AppTextField("头像 URL（可选）", avatarUrl, { avatarUrl = it })
					AppTextField("简介（可选）", bio, { bio = it }, singleLine = false)
					Text("性别", style = MaterialTheme.typography.labelLarge)
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
						listOf("UNSPECIFIED", "FEMALE", "MALE").forEach { option ->
							FilterChip(selected = gender == option, onClick = { gender = option }, label = { Text(option) })
						}
					}
				}

				if (error != null) Text(requireNotNull(error), color = MaterialTheme.colorScheme.error)
				Button(
					onClick = {
						error = null
						if (account.isBlank() || password.isBlank()) {
							error = "请输入账号和密码"
							return@Button
						}
						if (!loginMode && password.length < 8) {
							error = "密码至少需要 8 位"
							return@Button
						}
						submitting = true
						scope.launch {
							val result = if (loginMode) ApiClient.login(account, password)
							else ApiClient.register(account, password, gender, avatarUrl, bio)
							submitting = false
							when (result) {
								is ApiResult.Success -> onAuthenticated(result.value)
								is ApiResult.Failure -> error = result.message
							}
						}
					},
					modifier = Modifier.fillMaxWidth(),
					contentPadding = PaddingValues(vertical = 14.dp),
					enabled = !submitting,
				) { Text(if (submitting) "正在处理…" else if (loginMode) "登录" else "创建账号") }

				TextButton(onClick = { loginMode = !loginMode; error = null }, modifier = Modifier.fillMaxWidth()) {
					Text(if (loginMode) "没有账号？去注册" else "已有账号？去登录")
				}
			}
		}
	}
}

@Composable
private fun AppTextField(
	label: String,
	value: String,
	onValueChange: (String) -> Unit,
	password: Boolean = false,
	singleLine: Boolean = true,
) {
	OutlinedTextField(
		value = value,
		onValueChange = onValueChange,
		label = { Text(label) },
		singleLine = singleLine,
		visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
		colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, focusedLabelColor = Brand),
		modifier = Modifier.fillMaxWidth(),
	)
}

@Composable
private fun ProfileScreen(user: UserProfile, onLogout: () -> Unit) {
	Column(
		modifier = Modifier.fillMaxSize().padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Box(
			modifier = Modifier.size(92.dp).clip(CircleShape).background(Brand),
			contentAlignment = Alignment.Center,
		) {
			Text(user.account.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.displaySmall)
		}
		Spacer(Modifier.height(20.dp))
		Text("登录成功", color = Brand, style = MaterialTheme.typography.labelLarge)
		Text(user.account, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
		Spacer(Modifier.height(8.dp))
		Text(user.bio ?: "还没有填写简介", textAlign = TextAlign.Center, color = Color.Gray)
		Spacer(Modifier.height(28.dp))
		Text("用户 ID：${user.id}")
		Text("性别：${user.gender}")
		Spacer(Modifier.height(24.dp))
		TextButton(onClick = onLogout) { Text("退出登录") }
	}
}
