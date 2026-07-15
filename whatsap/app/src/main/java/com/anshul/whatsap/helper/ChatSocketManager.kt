package com.anshul.whatsap.helper

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

object ChatSocketManager {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val _messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val messageFlow = _messageFlow.asSharedFlow()

    private var currentUserId: String = ""

    fun connect(userId: String) {
        if (webSocket != null) return

        currentUserId = userId

        val wsUrl = ApiClient.getBaseUrl().replace("http://", "ws://")
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                val registerJson = JSONObject()
                registerJson.put("type", "register")
                registerJson.put("senderId", currentUserId)
                webSocket.send(registerJson.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _messageFlow.tryEmit(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@ChatSocketManager.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _messageFlow.tryEmit("""{"error": "Connection failed: ${t.message}"}""")
                this@ChatSocketManager.webSocket = null
            }
        })
    }

    fun sendPrivateMessage(targetId: String, content: String, messageType: String = "text", mediaUrl: String? = null, replyToId: String? = null) {
        val messageJson = JSONObject()
        messageJson.put("type", "private")
        messageJson.put("senderId", currentUserId)
        messageJson.put("targetId", targetId)
        messageJson.put("content", content)
        messageJson.put("messageType", messageType)
        if (mediaUrl != null) {
            messageJson.put("mediaUrl", mediaUrl)
        }
        if (replyToId != null) {
            messageJson.put("replyToId", replyToId)
        }
        webSocket?.send(messageJson.toString())
    }

    fun sendGroupMessage(roomId: String, content: String, messageType: String = "text", mediaUrl: String? = null, replyToId: String? = null) {
        val messageJson = JSONObject()
        messageJson.put("type", "group")
        messageJson.put("senderId", currentUserId)
        messageJson.put("targetId", roomId)
        messageJson.put("content", content)
        messageJson.put("messageType", messageType)
        if (mediaUrl != null) {
            messageJson.put("mediaUrl", mediaUrl)
        }
        if (replyToId != null) {
            messageJson.put("replyToId", replyToId)
        }
        webSocket?.send(messageJson.toString())
    }

    fun sendTypingIndicator(targetId: String, targetType: String, isTyping: Boolean) {
        val typingJson = JSONObject()
        typingJson.put("type", "typing")
        typingJson.put("senderId", currentUserId)
        typingJson.put("targetId", targetId)
        typingJson.put("targetType", targetType)
        typingJson.put("isTyping", isTyping)
        webSocket?.send(typingJson.toString())
    }

    fun sendReadReceipt(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val readJson = JSONObject()
        readJson.put("type", "read_receipt")
        readJson.put("senderId", currentUserId)
        val idsArray = JSONArray()
        for (id in messageIds) {
            idsArray.put(id)
        }
        readJson.put("messageIds", idsArray)
        webSocket?.send(readJson.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User logged out")
        webSocket = null
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }
}