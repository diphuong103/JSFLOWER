package com.example.jsflower.Model

data class MessageStatus(
    val hasUnreadMessages: Boolean = false,
    val lastReadTimestamp: Long = 0
) {
    constructor() : this(false, 0) // Constructor không tham số cần thiết cho Firebase
}