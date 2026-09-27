package com.example.thasmathjagratha.repository

import com.example.thasmathjagratha.model.AlertSeverity
import com.example.thasmathjagratha.model.AppMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockMessageRepository {

    private val now = System.currentTimeMillis()

    private val _messages = MutableStateFlow<List<AppMessage>>(
        listOf(
            AppMessage(
                messageId = "MSG-101",
                senderId = "RES-01",
                senderName = "Rescue Team 01",
                receiverId = "USR-AP-8842",
                receiverName = "Bhargav Ram",
                message = "Proceed to Sector 7 evacuation shelter for food packets and medical aid.",
                language = "Telugu",
                priority = AlertSeverity.NORMAL,
                timestamp = now - (10 * 60 * 1000L),
                isSentByMe = false,
                isRead = false
            ),
            AppMessage(
                messageId = "MSG-102",
                senderId = "USR-AP-8842",
                senderName = "Bhargav Ram",
                receiverId = "RES-01",
                receiverName = "Rescue Team 01",
                message = "Need medical assistance for elderly citizen at Sector 4.",
                language = "Telugu",
                priority = AlertSeverity.IMPORTANT,
                timestamp = "Just now".hashCode().toLong(),
                isSentByMe = true,
                isRead = true
            )
        )
    )
    val messages: StateFlow<List<AppMessage>> = _messages.asStateFlow()

    fun addMessage(message: AppMessage) {
        _messages.value = listOf(message) + _messages.value
    }

    fun markAsRead(messageId: String) {
        _messages.value = _messages.value.map { msg ->
            if (msg.messageId == messageId) msg.copy(isRead = true) else msg
        }
    }
}
