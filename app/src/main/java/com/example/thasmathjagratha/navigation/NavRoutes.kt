package com.example.thasmathjagratha.navigation

sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object RoleSelection : NavRoutes("role_selection")
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Home : NavRoutes("home")
    object Messages : NavRoutes("messages")
    object SendMessage : NavRoutes("send_message")
    object Alerts : NavRoutes("alerts")
    object ReceiverMain : NavRoutes("receiver_main")
    object MeshNetwork : NavRoutes("mesh_network")
    object Profile : NavRoutes("profile")
    object EmergencyPermissions : NavRoutes("emergency_permissions")
    object LanguageSelection : NavRoutes("language_selection")
    object PreferredLanguage : NavRoutes("preferred_language")
    object TtsPlayerScreen : NavRoutes("tts_player_screen")
    object TtsDemo : NavRoutes("tts_demo")

    object IncomingAlertPopup : NavRoutes("incoming_alert_popup/{alertId}") {
        fun createRoute(alertId: String) = "incoming_alert_popup/$alertId"
    }
    object AlertDetail : NavRoutes("alert_detail/{alertId}") {
        fun createRoute(alertId: String) = "alert_detail/$alertId"
    }
    object ForwardingStatus : NavRoutes("forwarding_status/{alertId}") {
        fun createRoute(alertId: String) = "forwarding_status/$alertId"
    }
    object DuplicateProtection : NavRoutes("duplicate_protection")
    object AlertHistory : NavRoutes("alert_history")
    object ReceiverSettings : NavRoutes("receiver_settings")

    // Sender & Utility Routes
    object Speak : NavRoutes("speak")
    object MessageType : NavRoutes("message_type")
    object EmergencyReport : NavRoutes("emergency_report")
    object GovtAlertCreation : NavRoutes("govt_alert_creation")
    object Settings : NavRoutes("settings")
}
