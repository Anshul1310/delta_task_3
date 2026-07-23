package com.anshul.whatsap.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://3.110.157.219:4000"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var savedToken: String = ""
    private var savedUserId: String = ""
    private var savedUserName: String = ""
    private var savedUserEmail: String = ""

    fun getBaseUrl(): String {
        return BASE_URL
    }

    fun getToken(): String {
        return savedToken
    }

    fun getUserId(): String {
        return savedUserId
    }

    fun getUserName(): String {
        return savedUserName
    }

    fun initialize(context: Context) {
        val prefs = getEncryptedPrefs(context)
        savedToken = prefs.getString("jwt_token", "") ?: ""
        savedUserId = prefs.getString("user_id", "") ?: ""
        savedUserName = prefs.getString("user_name", "") ?: ""
        savedUserEmail = prefs.getString("user_email", "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return savedToken.isNotEmpty() && savedUserId.isNotEmpty()
    }

    fun logout(context: Context) {
        savedToken = ""
        savedUserId = ""
        savedUserName = ""
        savedUserEmail = ""
        val prefs = getEncryptedPrefs(context)
        prefs.edit().clear().apply()
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "dchat_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun saveCredentials(context: Context, token: String, userId: String, userName: String, email: String) {
        savedToken = token
        savedUserId = userId
        savedUserName = userName
        savedUserEmail = email
        val prefs = getEncryptedPrefs(context)
        prefs.edit()
            .putString("jwt_token", token)
            .putString("user_id", userId)
            .putString("user_name", userName)
            .putString("user_email", email)
            .apply()
    }

    suspend fun signup(context: Context, name: String, email: String, password: String): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject()
                jsonBody.put("name", name)
                jsonBody.put("email", email)
                jsonBody.put("password", password)

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/signup")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val token = responseJson.getString("token")
                    val userObj = responseJson.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val userName = userObj.getString("name")
                    val userEmail = userObj.getString("email")
                    saveCredentials(context, token, userId, userName, userEmail)
                    Result.success(responseJson)
                } else {
                    val errorMessage = responseJson.optString("error", "Signup failed")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun login(context: Context, email: String, password: String): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject()
                jsonBody.put("email", email)
                jsonBody.put("password", password)

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/login")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val token = responseJson.getString("token")
                    val userObj = responseJson.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val userName = userObj.getString("name")
                    val userEmail = userObj.getString("email")
                    saveCredentials(context, token, userId, userName, userEmail)
                    Result.success(responseJson)
                } else {
                    val errorMessage = responseJson.optString("error", "Login failed")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getUsers(): Result<List<JSONObject>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/users")
                    .addHeader("Authorization", "Bearer $savedToken")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val usersArray = responseJson.getJSONArray("users")
                    val usersList = mutableListOf<JSONObject>()
                    for (i in 0 until usersArray.length()) {
                        usersList.add(usersArray.getJSONObject(i))
                    }
                    Result.success(usersList)
                } else {
                    Result.failure(Exception("Failed to fetch users"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getMessages(peerId: String): Result<List<JSONObject>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/messages/$peerId")
                    .addHeader("Authorization", "Bearer $savedToken")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val messagesArray = responseJson.getJSONArray("messages")
                    val messagesList = mutableListOf<JSONObject>()
                    for (i in 0 until messagesArray.length()) {
                        messagesList.add(messagesArray.getJSONObject(i))
                    }
                    Result.success(messagesList)
                } else {
                    Result.failure(Exception("Failed to fetch messages"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getRooms(): Result<List<JSONObject>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/rooms")
                    .addHeader("Authorization", "Bearer $savedToken")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val roomsArray = responseJson.getJSONArray("rooms")
                    val roomsList = mutableListOf<JSONObject>()
                    for (i in 0 until roomsArray.length()) {
                        roomsList.add(roomsArray.getJSONObject(i))
                    }
                    Result.success(roomsList)
                } else {
                    Result.failure(Exception("Failed to fetch rooms"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun createRoom(name: String, memberIds: List<String>): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject()
                jsonBody.put("name", name)
                val memberIdsArray = JSONArray()
                for (memberId in memberIds) {
                    memberIdsArray.put(memberId)
                }
                jsonBody.put("memberIds", memberIdsArray)

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/rooms")
                    .addHeader("Authorization", "Bearer $savedToken")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    Result.success(responseJson.getJSONObject("room"))
                } else {
                    Result.failure(Exception("Failed to create group"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getRoomMessages(roomId: String): Result<List<JSONObject>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/rooms/$roomId/messages")
                    .addHeader("Authorization", "Bearer $savedToken")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val messagesArray = responseJson.getJSONArray("messages")
                    val messagesList = mutableListOf<JSONObject>()
                    for (i in 0 until messagesArray.length()) {
                        messagesList.add(messagesArray.getJSONObject(i))
                    }
                    Result.success(messagesList)
                } else {
                    Result.failure(Exception("Failed to fetch room messages"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun uploadFile(file: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("application/octet-stream".toMediaType())
                    ).build()

                val request = Request.Builder()
                    .url("$BASE_URL/upload")
                    .addHeader("Authorization", "Bearer $savedToken")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val responseJson = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val url = responseJson.getString("url")
                    Result.success(url)
                } else {
                    Result.failure(Exception("Failed to upload file"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
}
