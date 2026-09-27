package com.example.thasmathjagratha.model

/**
 * Operating mode for the device.
 * SENDER: STT Mode — Speak & Send (Push-to-Talk speech-to-text, alert broadcast dispatch).
 * RECEIVER: TTS Mode — Listen & Receive (Background broadcast listener, text-to-speech alarm synthesis).
 * UNSET: Initial state requiring explicit user selection on first launch.
 */
enum class DeviceRole(val displayName: String, val modeTitle: String, val description: String) {
    UNSET(
        displayName = "Unset",
        modeTitle = "Role Unset",
        description = "Select an operating mode to begin."
    ),
    SENDER(
        displayName = "Sender",
        modeTitle = "STT Mode — Speak & Send",
        description = "Walkie-talkie emergency speech input with on-device speech-to-text. Encodes and transmits compressed alert broadcasts to nearby receivers."
    ),
    RECEIVER(
        displayName = "Receiver",
        modeTitle = "TTS Mode — Listen & Receive",
        description = "Continuously listens for local emergency broadcasts over Wi-Fi and Bluetooth. Synthesizes alerts into speech on-device with priority alarm audio."
    )
}
