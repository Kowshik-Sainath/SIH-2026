package com.example.thasmathjagratha.model

data class AppMessage(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String?,
    val receiverName: String?,
    val message: String,
    val language: String,
    val priority: AlertSeverity,
    val timestamp: Long,
    val isSentByMe: Boolean = false,
    val isRead: Boolean = false
)
