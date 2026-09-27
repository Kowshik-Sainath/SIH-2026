package com.example.thasmathjagratha.model

data class NearbyDevice(
    val deviceId: String,
    val name: String,
    val type: String,
    val distance: String,
    val signal: String,
    val connected: Boolean = false
)
