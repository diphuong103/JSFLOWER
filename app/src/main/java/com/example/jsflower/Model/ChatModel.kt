package com.example.jsflower.Model

data class ChatModel(
    val userId: String = "",
    val senderId: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
