package com.example.thasmathjagratha.ui.mesh

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thasmathjagratha.components.ThasmathJagrathaTopBar
import com.example.thasmathjagratha.theme.AlertCritical
import com.example.thasmathjagratha.theme.AlertVerified
import com.example.thasmathjagratha.theme.BackgroundLight
import com.example.thasmathjagratha.theme.NavyPrimary
import com.example.thasmathjagratha.theme.NavySecondary
import com.example.thasmathjagratha.theme.SurfaceWhite
import com.example.thasmathjagratha.theme.TextPrimary
import com.example.thasmathjagratha.theme.TextSecondary
import com.example.thasmathjagratha.viewmodel.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshNetworkScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val meshStatus by viewModel.meshStatus.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            ThasmathJagrathaTopBar(
                title = "Mesh Network Infrastructure",
                subtitle = "Decentralized P2P Emergency Propagation",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Stats Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CellTower, contentDescription = null, tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Network Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                            Text(
                                text = if (meshStatus.connected) "CONNECTED" else "DISCONNECTED",
                                color = AlertVerified,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(title = "Nearby Peers", value = "${meshStatus.nearbyPeers}", modifier = Modifier.weight(1f))
                        StatItem(title = "Alerts Received", value = "${meshStatus.alertsReceived}", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(title = "Alerts Forwarded", value = "${meshStatus.alertsForwarded}", modifier = Modifier.weight(1f))
                        StatItem(title = "Duplicates Blocked", value = "${meshStatus.duplicatesBlocked}", modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Live Network Mesh Topology",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Visualizing peer-to-peer active links in your local vicinity",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Compose Mesh Network Visualizer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySecondary)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val centerOffset = Offset(centerX, centerY)

                        val numNodes = 6
                        val orbitRadius = size.width.coerceAtMost(size.height) * 0.35f

                        val nodePositions = mutableListOf<Offset>()

                        for (i in 0 until numNodes) {
                            val angle = Math.toRadians((i * (360.0 / numNodes)))
                            val x = centerX + (orbitRadius * cos(angle)).toFloat()
                            val y = centerY + (orbitRadius * sin(angle)).toFloat()
                            nodePositions.add(Offset(x, y))
                        }

                        // Draw connection lines from center to nodes
                        nodePositions.forEach { pos ->
                            drawLine(
                                color = SurfaceWhite.copy(alpha = 0.4f),
                                start = centerOffset,
                                end = pos,
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Inter-node connection lines
                        for (i in nodePositions.indices) {
                            val nextPos = nodePositions[(i + 1) % nodePositions.size]
                            drawLine(
                                color = SurfaceWhite.copy(alpha = 0.2f),
                                start = nodePositions[i],
                                end = nextPos,
                                strokeWidth = 2f
                            )
                        }

                        // Draw outer nodes
                        nodePositions.forEachIndexed { idx, pos ->
                            drawCircle(
                                color = if (idx % 2 == 0) AlertVerified else AlertCritical,
                                radius = 16f,
                                center = pos
                            )
                            drawCircle(
                                color = SurfaceWhite,
                                radius = 8f,
                                center = pos
                            )
                        }

                        // Draw center node pulse
                        drawCircle(
                            color = SurfaceWhite.copy(alpha = 0.2f),
                            radius = 50f * pulseRatio,
                            center = centerOffset
                        )

                        // Center node
                        drawCircle(
                            color = AlertCritical,
                            radius = 28f,
                            center = centerOffset
                        )
                        drawCircle(
                            color = SurfaceWhite,
                            radius = 16f,
                            center = centerOffset
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(180.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceWhite.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Current Device (Host Relay)",
                                color = SurfaceWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.showSnackbar("Rescanning mesh routes...") },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Mesh Topology", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
