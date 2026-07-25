package com.anshul.whatsap.presentation

data class ChatListModel(
    val image: Int=0,
    val name: String="",
    val lastMessage: String="",
    val time: String="",
    val userId: String = "",
    val isOnline: Boolean = false,
    val isGroup: Boolean = false,
    val roomId: String = ""
)