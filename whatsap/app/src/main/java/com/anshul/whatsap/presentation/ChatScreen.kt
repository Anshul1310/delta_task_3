package com.anshul.whatsap.presentation

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anshul.whatsap.R
import com.anshul.whatsap.helper.ApiClient
import com.anshul.whatsap.helper.ChatSocketManager
import com.anshul.whatsap.ui.theme.AppBlack
import com.anshul.whatsap.ui.theme.AppBubbleMe
import com.anshul.whatsap.ui.theme.AppBubbleOther
import com.anshul.whatsap.ui.theme.AppDarkGray
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
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class ChatMessage(
    val id: String = "",
    val text: String,
    val isMe: Boolean,
    val time: String,
    val status: String = "SENT",
    val messageType: String = "text",
    val mediaUrl: String? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val senderName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatListModel: ChatListModel, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isTyping by remember { mutableStateOf(false) }
    var peerIsOnline by remember { mutableStateOf(chatListModel.isOnline) }
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val myUserId = ApiClient.getUserId()
    val peerId = chatListModel.userId
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
                    isUploading = true
                    val uploadResult = ApiClient.uploadFile(file)
                    isUploading = false
                    if (uploadResult.isSuccess) {
                        val mediaUrl = uploadResult.getOrDefault("")
                        ChatSocketManager.sendPrivateMessage(
                            targetId = peerId,
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

    LaunchedEffect(peerId) {
        val result = ApiClient.getMessages(peerId)
        if (result.isSuccess) {
            val messageList = result.getOrDefault(emptyList())
            val unreadIds = mutableListOf<String>()
            for (msgJson in messageList) {
                val senderId = msgJson.getString("senderId")
                val isMe = senderId == myUserId
                val createdAt = msgJson.getString("createdAt")
                val timeFormatted = formatTime(createdAt)

                var replyText: String? = null
                if (!msgJson.isNull("replyTo")) {
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
                        replyToId = msgJson.optString("replyToId", null),
                        replyToText = replyText
                    )
                )

                if (!isMe && msgJson.optString("status", "") != "READ") {
                    unreadIds.add(msgJson.getString("id"))
                }
            }

            if (unreadIds.isNotEmpty()) {
                ChatSocketManager.sendReadReceipt(unreadIds)
            }

            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(peerId) {
        ChatSocketManager.messageFlow.collectLatest { rawMessage ->
            try {
                val json = JSONObject(rawMessage)
                val type = json.optString("type", "")

                when (type) {
                    "message_sent" -> {
                        val msgSenderId = json.getString("senderId")
                        if (msgSenderId == myUserId) {
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
                                    status = json.optString("status", "SENT"),
                                    messageType = json.optString("messageType", "text"),
                                    mediaUrl = json.optString("mediaUrl", null),
                                    replyToText = replyText
                                )
                            )
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }

                    "private" -> {
                        val senderId = json.getString("senderId")
                        if (senderId == peerId) {
                            var replyText: String? = null
                            if (!json.isNull("replyTo") && json.has("replyTo")) {
                                val replyObj = json.optJSONObject("replyTo")
                                if (replyObj != null) {
                                    replyText = replyObj.optString("content", "")
                                }
                            }

                            val newMsg = ChatMessage(
                                id = json.getString("id"),
                                text = json.getString("content"),
                                isMe = false,
                                time = formatTime(json.getString("createdAt")),
                                status = "READ",
                                messageType = json.optString("messageType", "text"),
                                mediaUrl = json.optString("mediaUrl", null),
                                replyToText = replyText
                            )
                            messages.add(newMsg)

                            ChatSocketManager.sendReadReceipt(listOf(json.getString("id")))

                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }

                    "status_update" -> {
                        val messageId = json.getString("messageId")
                        val newStatus = json.getString("status")
                        val index = messages.indexOfFirst { it.id == messageId }
                        if (index >= 0) {
                            messages[index] = messages[index].copy(status = newStatus)
                        }
                    }

                    "typing" -> {
                        val typingSenderId = json.getString("senderId")
                        if (typingSenderId == peerId) {
                            isTyping = json.getBoolean("isTyping")
                        }
                    }

                    "online_status" -> {
                        val statusUserId = json.getString("userId")
                        if (statusUserId == peerId) {
                            peerIsOnline = json.getBoolean("isOnline")
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            ChatSocketManager.sendTypingIndicator(peerId, "private", true)
            delay(2000)
            ChatSocketManager.sendTypingIndicator(peerId, "private", false)
        }
    }

    if (showVoiceDialog) {
        VoiceRecordingDialog(
            onDone = { file ->
                showVoiceDialog = false
                if (file != null && file.exists()) {
                    coroutineScope.launch {
                        isUploading = true
                        val uploadResult = ApiClient.uploadFile(file)
                        isUploading = false
                        if (uploadResult.isSuccess) {
                            val mediaUrl = uploadResult.getOrDefault("")
                            ChatSocketManager.sendPrivateMessage(
                                targetId = peerId,
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

    if (isUploading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(AppDarkSurface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppGreen)
            }
        }
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp)) {
                            Image(
                                painter = painterResource(chatListModel.image),
                                contentDescription = "",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (peerIsOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(AppDarkSurface, CircleShape)
                                        .padding(2.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(AppGreen, CircleShape)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = chatListModel.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppWhite
                            )
                            if (isTyping) {
                                Text(
                                    text = "typing...",
                                    fontSize = 12.sp,
                                    color = AppGreen,
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                Text(
                                    text = if (peerIsOnline) "Online" else "Offline",
                                    fontSize = 12.sp,
                                    color = if (peerIsOnline) AppGreen else AppGray
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
            Column(
                modifier = Modifier
                    .background(AppDarkSurface)
                    .navigationBarsPadding()
            ) {
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
                                    text = if (replyToMessage!!.isMe) "You" else chatListModel.name,
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
                                ChatSocketManager.sendPrivateMessage(
                                    targetId = peerId,
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

@Composable
fun SwipeableMessageBubble(
    message: ChatMessage,
    onSwipeToReply: (ChatMessage) -> Unit,
    chatPartnerName: String,
    onImageClick: (String) -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 80f) {
                            onSwipeToReply(message)
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        if (newOffset >= 0f) {
                            offsetX = newOffset.coerceAtMost(120f)
                        }
                    }
                )
            }
    ) {
        if (offsetX > 20f) {
            Text(
                text = "↩",
                fontSize = 20.sp,
                color = AppGreen,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            ChatBubble(
                message = message,
                chatPartnerName = chatPartnerName,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    chatPartnerName: String = "",
    onImageClick: (String) -> Unit = {}
) {
    val bubbleColor = if (message.isMe) AppBubbleMe else AppBubbleOther
    val alignment = if (message.isMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMe) 16.dp else 4.dp,
                bottomEnd = if (message.isMe) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                if (message.replyToText != null && message.replyToText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppBlack.copy(alpha = 0.3f)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .background(AppGreen, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.replyToText,
                                fontSize = 12.sp,
                                color = AppGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.messageType == "voice") {
                    VoiceNotePlayer(message = message)
                } else if (message.messageType == "image" && message.mediaUrl != null) {
                    val fullUrl = getFullMediaUrl(message.mediaUrl)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fullUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(fullUrl) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.messageType == "text") {
                    Text(text = message.text, fontSize = 15.sp, color = AppWhite)
                } else if (message.messageType == "image" && message.text != "📎 Attachment") {
                    Text(text = message.text, fontSize = 15.sp, color = AppWhite)
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.time,
                        fontSize = 10.sp,
                        color = AppGray
                    )
                    if (message.isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val tickText = when (message.status) {
                            "SENT" -> "✓"
                            "DELIVERED" -> "✓✓"
                            "READ" -> "✓✓"
                            else -> "✓"
                        }
                        val tickColor = when (message.status) {
                            "READ" -> Color(0xFF4FC3F7)
                            else -> AppGray
                        }
                        Text(
                            text = tickText,
                            fontSize = 11.sp,
                            color = tickColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceNotePlayer(message: ChatMessage) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(AppBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable {
                if (isPlaying) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isPlaying = false
                } else {
                    try {
                        val url = getFullMediaUrl(message.mediaUrl)
                        val player = MediaPlayer()
                        player.setDataSource(url)
                        player.prepareAsync()
                        player.setOnPreparedListener {
                            it.start()
                            isPlaying = true
                        }
                        mediaPlayer = player
                        player.setOnCompletionListener {
                            isPlaying = false
                            player.release()
                            mediaPlayer = null
                        }
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                }
            }
    ) {
        Text(
            text = if (isPlaying) "⏸" else "▶",
            fontSize = 22.sp,
            color = AppGreen
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Voice Note", fontSize = 14.sp, color = AppWhite)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isPlaying) "▃▅▇▅▃▅▇▅▃" else "▃▃▃▃▃▃▃▃▃",
            fontSize = 10.sp,
            color = AppGreen
        )
    }
}

@Composable
fun VoiceRecordingDialog(
    onDone: (File?) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            voiceFile = audioFile
            val newRecorder = MediaRecorder(context)
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(audioFile.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            isRecording = true
        } catch (_: Exception) {
            onReset()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                try {
                    recorder?.stop()
                } catch (_: Exception) {}
                recorder?.release()
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            isRecording = false
            voiceFile?.delete()
            onReset()
        },
        containerColor = AppDarkGray,
        title = {
            Text(
                text = "Recording Voice Note",
                color = AppWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(if (isRecording) pulseScale else 1f)
                        .background(Color.Red.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎤", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                val mins = recordingSeconds / 60
                val secs = recordingSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isRecording) {
                    Text(text = "Recording...", color = Color.Red, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {}
                recorder = null
                isRecording = false
                onDone(voiceFile)
            }) {
                Text("Done", color = AppGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {}
                recorder = null
                isRecording = false
                voiceFile?.delete()
                onReset()
            }) {
                Text("Reset", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    )
}

@Composable
fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBlack)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = AppWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        var fileName = "file_${System.currentTimeMillis()}"

        val cursor = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
            cursor.close()
        }

        val tempFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return tempFile
    } catch (_: Exception) {
        return null
    }
}

fun formatTime(isoString: String): String {
    try {
        val parts = isoString.split("T")
        if (parts.size >= 2) {
            val timePart = parts[1].split(":")
            if (timePart.size >= 2) {
                var hour = timePart[0].toIntOrNull() ?: 0
                val minute = timePart[1]
                val amPm = if (hour >= 12) "pm" else "am"
                if (hour > 12) hour -= 12
                if (hour == 0) hour = 12
                return "$hour:$minute $amPm"
            }
        }
        return ""
    } catch (_: Exception) {
        return ""
    }
}

fun getFullMediaUrl(mediaUrl: String?): String {
    if (mediaUrl.isNullOrEmpty()) return ""
    return if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
        mediaUrl
    } else {
        ApiClient.getBaseUrl() + mediaUrl
    }
}