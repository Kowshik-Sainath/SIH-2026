package com.example.thasmathjagratha.model

enum class UserRole(val displayName: String, val description: String) {
    CITIZEN("Citizen", "Send private messages, submit emergency reports, receive alerts automatically"),
    FIELD_OFFICER("Field Officer", "Send messages, submit reports, broadcast local alerts, receive alerts automatically"),
    GOVERNMENT("Government Authority", "Create official verified alerts, select target scope, receive alerts automatically")
}
