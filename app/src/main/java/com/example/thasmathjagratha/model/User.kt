package com.example.thasmathjagratha.model

data class User(
    val userId: String = "USR-AP-8842",
    val name: String = "Bhargav Ram",
    val role: UserRole = UserRole.CITIZEN,
    val verified: Boolean = true,
    val preferredLanguage: String = "Telugu",
    val state: String = "Andhra Pradesh",
    val district: String = "Vijayawada East"
)
