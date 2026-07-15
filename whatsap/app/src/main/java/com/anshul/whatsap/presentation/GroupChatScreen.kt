package com.anshul.whatsap.presentation

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.whatsap.R
import com.anshul.whatsap.helper.ApiClient
import com.anshul.whatsap.helper.ChatSocketManager
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppDarkSurface
import com.anshul.whatsap.ui.theme.AppGray
import com.anshul.whatsap.ui.theme.AppGreen
import com.anshul.whatsap.ui.theme.AppInputBg
import com.anshul.whatsap.ui.theme.AppReplyBg
import com.anshul.whatsap.ui.theme.AppWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(chatListModel: ChatListModel, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var typingUserName by remember { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf<String?>(null) }

    val myUserId = ApiClient.getUserId()
    val roomId = chatListModel.roomId
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val file = uriToFile(context, uri)
                if (file != null) {
                    val uploadResult = ApiClient.uploadFile(file)
                    if (uploadResult.isSuccess) {
                        val mediaUrl = uploadResult.getOrDefault("")
                        ChatSocketManager.sendGroupMessage(
                            roomId = roomId,
                            content = "📎 Attachment",
                            messageType = "image",
                            mediaUrl = mediaUrl,
                            replyToId = replyToMessage?.id
                        )
                        replyToMessage = null
                    }
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showVoiceDialog = true
        }
    }

    LaunchedEffect(roomId) {
        val result = ApiClient.getRoomMessages(roomId)
        if (result.isSuccess) {
            val messageList = result.getOrDefault(emptyList())
            for (msgJson in messageList) {
                val senderId = msgJson.getString("senderId")
                val isMe = senderId == myUserId
                val createdAt = msgJson.getString("createdAt")
                val timeFormatted = formatTime(createdAt)

                var senderName = ""
                if (!isMe && msgJson.has("sender") && !msgJson.isNull("sender")) {
                    val senderObj = msgJson.getJSONObject("sender")
                    senderName = senderObj.optString("name", "")
                }

                var replyText: String? = null
                if (!msgJson.isNull("replyTo") && msgJson.has("replyTo")) {
                    val replyObj = msgJson.getJSONObject("replyTo")
                    replyText = replyObj.optString("content", "")
                }

                messages.add(
                    ChatMessage(
                        id = msgJson.getString("id"),
                        text = msgJson.getString("content"),
                        isMe = isMe,
                        time = timeFormatted,
                        status = msgJson.optString("status", "SENT"),
                        messageType = msgJson.optString("messageType", "text"),
                        mediaUrl = msgJson.optString("mediaUrl", null),
                        replyToText = replyText,
                        senderName = senderName
                    )
                )
            }
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(roomId) {
        ChatSocketManager.messageFlow.collectLatest { rawMessage ->
            try {
                val json = JSONObject(rawMessage)
                val type = json.optString("type", "")

                when (type) {
                    "group_message_sent" -> {
                        val targetId = json.optString("targetId", "")
                        if (targetId == roomId) {
                            var replyText: String? = null
                            if (!json.isNull("replyTo") && json.has("replyTo")) {
                                val replyObj = json.optJSONObject("replyTo")
                                if (replyObj != null) {
                                    replyText = replyObj.optString("content", "")
                                }
                            }
                            messages.add(
                                ChatMessage(
                                    id = json.getString("id"),
                                    text = json.getString("content"),
                                    isMe = true,
                                    time = formatTime(json.getString("createdAt")),
                                    messageType = json.optString("messageType", "text"),
                                    mediaUrl = json.optString("mediaUrl", null),
                                    replyToText = replyText,
                                    senderName = "You"
                                )
                            )
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }

                    "group" -> {
                        val targetId = json.optString("targetId", "")
                        if (targetId == roomId) {
                            var replyText: String? = null
                            if (!json.isNull("replyTo") && json.has("replyTo")) {
                                val replyObj = json.optJSONObject("replyTo")
                                if (replyObj != null) {
                                    replyText = replyObj.optString("content", "")
                                }
                            }
                            messages.add(
                                ChatMessage(
                                    id = json.getString("id"),
                                    text = json.getString("content"),
                                    isMe = false,
                                    time = formatTime(json.getString("createdAt")),
                                    messageType = json.optString("messageType", "text"),
                                    mediaUrl = json.optString("mediaUrl", null),
                                    replyToText = replyText,
                                    senderName = json.optString("senderName", "")
                                )
                            )
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }

                    "typing" -> {
                        val targetId = json.optString("targetId", "")
                        if (targetId == roomId) {
                            val senderIsTyping = json.getBoolean("isTyping")
                            typingUserName = if (senderIsTyping) {
                                json.optString("senderId", "Someone")
                            } else {
                                ""
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            ChatSocketManager.sendTypingIndicator(roomId, "group", true)
            delay(2000)
            ChatSocketManager.sendTypingIndicator(roomId, "group", false)
        }
    }

    if (showVoiceDialog) {
        VoiceRecordingDialog(
            onDone = { file ->
                showVoiceDialog = false
                if (file != null && file.exists()) {
                    coroutineScope.launch {
                        val uploadResult = ApiClient.uploadFile(file)
                        if (uploadResult.isSuccess) {
                            val mediaUrl = uploadResult.getOrDefault("")
                            ChatSocketManager.sendGroupMessage(
                                roomId = roomId,
                                content = "🎤 Voice Note",
                                messageType = "voice",
                                mediaUrl = mediaUrl
                            )
                        }
                        file.delete()
                    }
                }
            },
            onReset = {
                showVoiceDialog = false
            }
        )
    }

    if (showImageViewer != null) {
        FullScreenImageViewer(
            imageUrl = showImageViewer!!,
            onDismiss = { showImageViewer = null }
        )
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(chatListModel.image),
                            contentDescription = "",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = chatListModel.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppWhite
                            )
                            if (typingUserName.isNotEmpty()) {
                                Text(
                                    text = "someone is typing...",
                                    fontSize = 12.sp,
                                    color = AppGreen,
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                Text(
                                    text = "Group Chat",
                                    fontSize = 12.sp,
                                    color = AppGray
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppDarkSurface
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(AppDarkSurface)) {
                AnimatedVisibility(visible = replyToMessage != null) {
                    if (replyToMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppReplyBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(30.dp)
                                    .background(AppGreen, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = replyToMessage!!.senderName ?: "User",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppGreen
                                )
                                Text(
                                    text = replyToMessage!!.text,
                                    fontSize = 12.sp,
                                    color = AppGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyToMessage = null }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(18.dp),
                                    tint = AppGray
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(AppInputBg, RoundedCornerShape(24.dp))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message", color = AppGray, fontSize = 15.sp) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = AppGreen,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = AppWhite,
                                unfocusedTextColor = AppWhite
                            ),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(text = "📎", fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(text = "🎤", fontSize = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                ChatSocketManager.sendGroupMessage(
                                    roomId = roomId,
                                    content = messageText,
                                    replyToId = replyToMessage?.id
                                )
                                messageText = ""
                                replyToMessage = null
                            }
                        },
                        shape = CircleShape,
                        color = AppGreen,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = AppBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(AppBlack)
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                if (!message.isMe && !message.senderName.isNullOrEmpty()) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppGreen,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                SwipeableMessageBubble(
                    message = message,
                    onSwipeToReply = { swipedMessage ->
                        replyToMessage = swipedMessage
                    },
                    chatPartnerName = chatListModel.name,
                    onImageClick = { url -> showImageViewer = url }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
