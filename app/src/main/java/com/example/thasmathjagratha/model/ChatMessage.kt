package com.example.thasmathjagratha.model

data class ChatMessage(
    val id: String,
    val sender: String,
    val senderRole: UserRole,
    val receiver: String,
    val content: String,
    val timestamp: String,
    val isEmergency: Boolean = false,
    val isSentByMe: Boolean = false,
    val isVerified: Boolean = true
)
