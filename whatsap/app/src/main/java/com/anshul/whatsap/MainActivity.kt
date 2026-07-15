package com.anshul.whatsap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anshul.whatsap.helper.ApiClient
import com.anshul.whatsap.helper.ChatSocketManager
import com.anshul.whatsap.presentation.ChatListModel
import com.anshul.whatsap.presentation.ChatScreen
import com.anshul.whatsap.presentation.CreateGroupScreen
import com.anshul.whatsap.presentation.GroupChatScreen
import com.anshul.whatsap.presentation.HomeScreen
import com.anshul.whatsap.presentation.LoginScreen
import com.anshul.whatsap.presentation.SignupScreen
import com.anshul.whatsap.presentation.SplashScreen
import com.anshul.whatsap.presentation.UpdateScreen
import com.anshul.whatsap.presentation.WelcomeScreen
import com.anshul.whatsap.ui.theme.WhatsapTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiClient.initialize(this)
        setContent {
            WhatsapTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("splash") }
    var selectedChat by remember { mutableStateOf<ChatListModel?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(2000)
        if (ApiClient.isLoggedIn()) {
            ChatSocketManager.connect(ApiClient.getUserId())
            currentScreen = "home"
            selectedTab = 0
        } else {
            currentScreen = "welcome"
        }
    }

    when (currentScreen) {
        "splash" -> SplashScreen()

        "welcome" -> WelcomeScreen(
            onLoginClick = { currentScreen = "login" },
            onSignupClick = { currentScreen = "signup" }
        )

        "login" -> LoginScreen(
            onLoginSuccess = {
                selectedTab = 0
                currentScreen = "home"
            },
            onSignupClick = { currentScreen = "signup" }
        )

        "signup" -> SignupScreen(
            onSignupSuccess = {
                selectedTab = 0
                currentScreen = "home"
            },
            onLoginClick = { currentScreen = "login" }
        )

        "home" -> HomeScreen(
            onChatClick = { chat ->
                selectedChat = chat
                if (chat.isGroup) {
                    currentScreen = "group_chat"
                } else {
                    currentScreen = "chat"
                }
            },
            onCreateGroupClick = {
                currentScreen = "create_group"
            },
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                when (tab) {
                    0 -> currentScreen = "home"
                    1 -> currentScreen = "updates"
                }
            }
        )

        "chat" -> {
            selectedChat?.let { chat ->
                ChatScreen(
                    chatListModel = chat,
                    onBack = { currentScreen = "home" }
                )
            }
        }

        "group_chat" -> {
            selectedChat?.let { chat ->
                GroupChatScreen(
                    chatListModel = chat,
                    onBack = { currentScreen = "home" }
                )
            }
        }

        "create_group" -> CreateGroupScreen(
            onBack = { currentScreen = "home" },
            onGroupCreated = {
                selectedTab = 0
                currentScreen = "home"
            }
        )

        "updates" -> UpdateScreen(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                when (tab) {
                    0 -> currentScreen = "home"
                    1 -> currentScreen = "updates"
                }
            }
        )
    }
}