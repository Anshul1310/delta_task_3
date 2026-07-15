package com.anshul.whatsap.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.helper.ApiClient
import com.anshul.whatsap.helper.ChatSocketManager
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppDarkGray
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppWhite
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    onChatClick: (ChatListModel) -> Unit,
    onCreateGroupClick: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    var chatData by remember { mutableStateOf(listOf<ChatListModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        val combinedList = mutableListOf<ChatListModel>()

        val usersResult = ApiClient.getUsers()
        if (usersResult.isSuccess) {
            val users = usersResult.getOrDefault(emptyList())
            for (userJson in users) {
                val userId = userJson.getString("id")
                val userName = userJson.getString("name")
                val isOnline = userJson.optBoolean("isOnline", false)
                val statusText = if (isOnline) "Online" else "Tap to chat"
                combinedList.add(
                    ChatListModel(
                        image = R.drawable.boy,
                        name = userName,
                        lastMessage = statusText,
                        time = "",
                        userId = userId,
                        isOnline = isOnline,
                        isGroup = false
                    )
                )
            }
        } else {
            errorText = usersResult.exceptionOrNull()?.message ?: "Failed to load users"
        }

        val roomsResult = ApiClient.getRooms()
        if (roomsResult.isSuccess) {
            val rooms = roomsResult.getOrDefault(emptyList())
            for (roomJson in rooms) {
                val roomId = roomJson.getString("id")
                val roomName = roomJson.optString("name", "Group")
                val messagesArray = roomJson.optJSONArray("messages")
                var lastMsg = "No messages yet"
                if (messagesArray != null && messagesArray.length() > 0) {
                    lastMsg = messagesArray.getJSONObject(0).optString("content", "")
                }
                combinedList.add(
                    ChatListModel(
                        image = R.drawable.communities_icon,
                        name = roomName,
                        lastMessage = lastMsg,
                        time = "",
                        userId = "",
                        isOnline = false,
                        isGroup = true,
                        roomId = roomId
                    )
                )
            }
        }

        chatData = combinedList
        isLoading = false
    }

    LaunchedEffect(Unit) {
        ChatSocketManager.messageFlow.collectLatest { rawMessage ->
            try {
                val json = org.json.JSONObject(rawMessage)
                val type = json.optString("type", "")
                if (type == "online_status") {
                    val statusUserId = json.getString("userId")
                    val statusIsOnline = json.getBoolean("isOnline")
                    chatData = chatData.map { chat ->
                        if (chat.userId == statusUserId) {
                            chat.copy(
                                isOnline = statusIsOnline,
                                lastMessage = if (statusIsOnline) "Online" else "Tap to chat"
                            )
                        } else {
                            chat
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        containerColor = AppBlack,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { onCreateGroupClick() },
                    containerColor = AppGreen.copy(alpha = 0.85f),
                    contentColor = AppBlack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.communities_icon),
                        contentDescription = "Create Group",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(AppBlack)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = {},
                    containerColor = AppGreen,
                    contentColor = AppBlack
                ) {
                    Image(
                        painter = painterResource(R.drawable.message_4475881),
                        contentDescription = "New Chat",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(AppBlack)
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigation(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    ) {
        Column(modifier = Modifier.padding(it).background(AppBlack)) {
            TopBar({}, {})
            Spacer(modifier = Modifier.height(5.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = AppDarkGray)
            Spacer(modifier = Modifier.height(5.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppGreen)
                }
            } else if (errorText.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorText, color = Color.Red, fontSize = 14.sp)
                }
            } else if (chatData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No conversations yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Sign up more users to start chatting!", fontSize = 13.sp, color = AppGray)
                    }
                }
            } else {
                LazyColumn {
                    items(chatData) { chat ->
                        ChatDesign(chatListModel = chat, onClick = { onChatClick(chat) })
                    }
                }
            }
        }
    }
}