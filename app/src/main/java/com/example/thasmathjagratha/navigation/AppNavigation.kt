package com.example.thasmathjagratha.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.thasmathjagratha.model.DeviceRole
import com.example.thasmathjagratha.ui.role.RoleSelectionScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.thasmathjagratha.components.CriticalAlertOverlay
import com.example.thasmathjagratha.components.ThasmathJagrathaBottomNav
import com.example.thasmathjagratha.components.NormalNotificationBanner
import com.example.thasmathjagratha.ui.alerts.AlertDetailScreen
import com.example.thasmathjagratha.ui.alerts.AlertsScreen
import com.example.thasmathjagratha.ui.alerts.GovtAlertCreationScreen
import com.example.thasmathjagratha.ui.auth.LoginScreen
import com.example.thasmathjagratha.ui.auth.RegisterScreen
import com.example.thasmathjagratha.ui.history.AlertHistoryScreen
import com.example.thasmathjagratha.ui.home.HomeScreen
import com.example.thasmathjagratha.ui.language.PreferredLanguageScreen
import com.example.thasmathjagratha.ui.mesh.MeshNetworkScreen
import com.example.thasmathjagratha.ui.messages.MessageDetailScreen
import com.example.thasmathjagratha.ui.messages.MessagesScreen
import com.example.thasmathjagratha.ui.messages.MessageTypeScreen
import com.example.thasmathjagratha.ui.messages.SendMessageScreen
import com.example.thasmathjagratha.ui.permissions.EmergencyPermissionsScreen
import com.example.thasmathjagratha.ui.profile.ProfileScreen
import com.example.thasmathjagratha.ui.receiver.DuplicateProtectionScreen
import com.example.thasmathjagratha.ui.receiver.ForwardingStatusScreen
import com.example.thasmathjagratha.ui.receiver.ReceiverMainScreen
import com.example.thasmathjagratha.ui.reports.EmergencyReportScreen
import com.example.thasmathjagratha.ui.settings.LanguageSelectionScreen
import com.example.thasmathjagratha.ui.settings.ReceiverSettingsScreen
import com.example.thasmathjagratha.ui.settings.SettingsScreen
import com.example.thasmathjagratha.ui.speak.SpeakScreen
import com.example.thasmathjagratha.tts.ui.TtsDemoScreen
import com.example.thasmathjagratha.ui.splash.SplashScreen
import com.example.thasmathjagratha.viewmodel.MainViewModel

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val activeCriticalAlert by viewModel.activeCriticalAlert.collectAsState()
    val activeNormalBanner by viewModel.activeNormalBanner.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        NavRoutes.Home.route,
        NavRoutes.Alerts.route,
        NavRoutes.MeshNetwork.route,
        NavRoutes.Profile.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    ThasmathJagrathaBottomNav(
                        currentRoute = currentRoute,
                        onItemSelected = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.Splash.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(NavRoutes.TtsDemo.route) {
                    TtsDemoScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.RoleSelection.route) {
                    val context = LocalContext.current
                    RoleSelectionScreen(
                        onSelectRole = { role ->
                            viewModel.selectRole(role, context)
                            if (role == DeviceRole.SENDER) {
                                navController.navigate(NavRoutes.Speak.route) {
                                    popUpTo(NavRoutes.RoleSelection.route) { inclusive = true }
                                }
                            } else if (role == DeviceRole.RECEIVER) {
                                navController.navigate(NavRoutes.ReceiverMain.route) {
                                    popUpTo(NavRoutes.RoleSelection.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(NavRoutes.Splash.route) {
                    SplashScreen(
                        onNavigateToLogin = {
                            val role = viewModel.deviceRole.value
                            when (role) {
                                DeviceRole.UNSET -> {
                                    navController.navigate(NavRoutes.RoleSelection.route) {
                                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                    }
                                }
                                DeviceRole.SENDER -> {
                                    navController.navigate(NavRoutes.Speak.route) {
                                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                    }
                                }
                                DeviceRole.RECEIVER -> {
                                    navController.navigate(NavRoutes.ReceiverMain.route) {
                                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }

                composable(NavRoutes.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSpeak = { navController.navigate(NavRoutes.Speak.route) },
                        onNavigateToSendMessage = { navController.navigate(NavRoutes.SendMessage.route) },
                        onNavigateToEmergencyReport = { navController.navigate(NavRoutes.EmergencyReport.route) },
                        onNavigateToAlerts = { navController.navigate(NavRoutes.Alerts.route) },
                        onNavigateToMessages = { navController.navigate(NavRoutes.Messages.route) },
                        onNavigateToMeshNetwork = { navController.navigate(NavRoutes.MeshNetwork.route) },
                        onNavigateToGovtAlertCreation = {
                            viewModel.checkBroadcastAuthorization {
                                navController.navigate(NavRoutes.GovtAlertCreation.route)
                            }
                        },
                        onNavigateToAlertDetail = { alertId ->
                            navController.navigate(NavRoutes.AlertDetail.createRoute(alertId))
                        },
                        onNavigateToLanguage = { navController.navigate(NavRoutes.LanguageSelection.route) }
                    )
                }

                composable(NavRoutes.Messages.route) {
                    MessagesScreen(
                        viewModel = viewModel,
                        onNavigateToMessageType = { navController.navigate(NavRoutes.SendMessage.route) }
                    )
                }

                composable(NavRoutes.SendMessage.route) {
                    SendMessageScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.Speak.route) {
                    val context = LocalContext.current
                    SpeakScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            viewModel.changeRole(context)
                            navController.navigate(NavRoutes.RoleSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToTts = { navController.navigate(NavRoutes.TtsDemo.route) }
                    )
                }

                composable(NavRoutes.Alerts.route) {
                    AlertsScreen(
                        viewModel = viewModel,
                        onNavigateToAlertDetail = { alertId ->
                            navController.navigate(NavRoutes.AlertDetail.createRoute(alertId))
                        }
                    )
                }

                composable(NavRoutes.MeshNetwork.route) {
                    MeshNetworkScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.Profile.route) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToLanguage = { navController.navigate(NavRoutes.LanguageSelection.route) },
                        onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                        onLogout = {
                            navController.navigate(NavRoutes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(NavRoutes.ReceiverMain.route) {
                    val context = LocalContext.current
                    ReceiverMainScreen(
                        viewModel = viewModel,
                        onNavigateToForwarding = { alertId ->
                            navController.navigate(NavRoutes.ForwardingStatus.createRoute(alertId))
                        },
                        onNavigateToLanguage = { navController.navigate(NavRoutes.LanguageSelection.route) },
                        onNavigateToRoleSelection = {
                            viewModel.changeRole(context)
                            navController.navigate(NavRoutes.RoleSelection.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(NavRoutes.EmergencyPermissions.route) {
                    EmergencyPermissionsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.LanguageSelection.route) {
                    LanguageSelectionScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToLanguage = { navController.navigate(NavRoutes.LanguageSelection.route) },
                        onNavigateToPermissions = { navController.navigate(NavRoutes.EmergencyPermissions.route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.EmergencyReport.route) {
                    EmergencyReportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.GovtAlertCreation.route) {
                    GovtAlertCreationScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.Login.route) {
                    LoginScreen(
                        onLoginSuccess = { role, mobile ->
                            viewModel.login(role, mobile)
                            navController.navigate(NavRoutes.Home.route) {
                                popUpTo(NavRoutes.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { navController.navigate(NavRoutes.Register.route) }
                    )
                }

                composable(NavRoutes.Register.route) {
                    RegisterScreen(
                        onRegisterSubmit = { name, mobile, govtId, state, district, language ->
                            viewModel.register(name, mobile, govtId, state, district, language)
                            navController.navigate(NavRoutes.Home.route) {
                                popUpTo(NavRoutes.Register.route) { inclusive = true }
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = NavRoutes.AlertDetail.route,
                    arguments = listOf(navArgument("alertId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
                    AlertDetailScreen(
                        alertId = alertId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Top Slide-In Normal Notification Banner
        activeNormalBanner?.let { msg ->
            NormalNotificationBanner(
                message = msg,
                onView = {
                    viewModel.dismissNormalBanner()
                    navController.navigate(NavRoutes.Messages.route)
                },
                onDismiss = { viewModel.dismissNormalBanner() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Full-Screen Emergency Critical Alert Overlay
        activeCriticalAlert?.let { alert ->
            CriticalAlertOverlay(
                alert = alert,
                onAcknowledge = {
                    viewModel.acknowledgeAlert(alert.alertId)
                    viewModel.dismissCriticalAlert()
                },
                onPlayAgain = {
                    viewModel.replayReceivedAlert(alert)
                },
                onViewMap = {},
                onViewDetails = {
                    viewModel.dismissCriticalAlert()
                    navController.navigate(NavRoutes.AlertDetail.createRoute(alert.alertId))
                }
            )
        }
    }
}
