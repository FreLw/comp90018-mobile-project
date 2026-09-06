package com.comp90018.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UserProfile(
	val id: Long,
	val account: String,
	val gender: String,
	val avatarUrl: String?,
	val bio: String?,
)

sealed interface ApiResult<out T> {
	data class Success<T>(val value: T) : ApiResult<T>
	data class Failure(val message: String) : ApiResult<Nothing>
}

object ApiClient {
	private val baseUrl = BuildConfig.API_BASE_URL

	suspend fun register(
		account: String,
		password: String,
		gender: String,
		avatarUrl: String,
		bio: String,
	): ApiResult<UserProfile> = post(
		path = "api/v1/users",
		body = JSONObject()
			.put("account", account)
			.put("password", password)
			.put("gender", gender)
			.put("avatarUrl", avatarUrl)
			.put("bio", bio),
	)

	suspend fun login(account: String, password: String): ApiResult<UserProfile> = post(
		path = "api/v1/users/login",
		body = JSONObject().put("account", account).put("password", password),
	)

	private suspend fun post(path: String, body: JSONObject): ApiResult<UserProfile> = withContext(Dispatchers.IO) {
		try {
			val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
				requestMethod = "POST"
				doOutput = true
				connectTimeout = 10_000
				readTimeout = 10_000
				setRequestProperty("Content-Type", "application/json; charset=utf-8")
			}

			connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
			val status = connection.responseCode
			val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
				?.bufferedReader()?.use { it.readText() }.orEmpty()
			connection.disconnect()

			if (status !in 200..299) {
				return@withContext ApiResult.Failure(if (status == 401) "账号或密码错误" else "请求失败（HTTP $status）")
			}

			val json = JSONObject(response)
			ApiResult.Success(
				UserProfile(
					id = json.getLong("id"),
					account = json.getString("account"),
					gender = json.getString("gender"),
					avatarUrl = json.optString("avatarUrl").ifBlank { null },
					bio = json.optString("bio").ifBlank { null },
				),
			)
		} catch (exception: Exception) {
			ApiResult.Failure(
				"无法连接服务器：${exception.message ?: exception.javaClass.simpleName}",
			)
	}
}
}
