package com.example.coachapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*

@Composable
fun CoachSpaceScreen(
    modifier: Modifier = Modifier,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    onResourceClick: (LaboResource) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF001529))
    ) {
        // Background Blobs (Always visible for consistency)
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = 150.dp)
                .size(200.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape)
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .size(250.dp)
                .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape)
                .blur(80.dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = viewModel.coachSpaceTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {}
            ) {
                Tab(
                    selected = viewModel.coachSpaceTab == 0, 
                    onClick = { viewModel.coachSpaceTab = 0 },
                    unselectedContentColor = Color.White.copy(alpha = 0.6f),
                    icon = { Icon(Icons.Default.Forum, null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Vestiaire", fontWeight = if (viewModel.coachSpaceTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
                Tab(
                    selected = viewModel.coachSpaceTab == 1, 
                    onClick = { viewModel.coachSpaceTab = 1 },
                    unselectedContentColor = Color.White.copy(alpha = 0.6f),
                    icon = { Icon(Icons.Default.Science, null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Labo", fontWeight = if (viewModel.coachSpaceTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
                if (viewModel.userRole == UserRole.MEGADMIN) {
                    Tab(
                        selected = viewModel.coachSpaceTab == 2, 
                        onClick = { viewModel.coachSpaceTab = 2 },
                        unselectedContentColor = Color.White.copy(alpha = 0.6f),
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (viewModel.adminAlerts.any { it.statut == "non_traite" }) {
                                        Badge(containerColor = Color.Red, modifier = Modifier.offset(x = 4.dp, y = (-4).dp))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Shield, null, modifier = Modifier.size(20.dp))
                            }
                        },
                        text = { Text("Alertes", fontWeight = if (viewModel.coachSpaceTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                    )
                }
            }

            when (viewModel.coachSpaceTab) {
                0 -> {
                    Column {
                        if (viewModel.lockerRoomError != null) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                                Text(viewModel.lockerRoomError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (viewModel.isLockerRoomLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        LockerRoomScreen(
                            seasonConfig = viewModel.seasonConfig, 
                            posts = viewModel.publicPosts,
                            userRole = viewModel.userRole,
                            onPost = { title, content, cat, alias, official -> 
                                viewModel.postToLockerRoom(title, content, cat, alias, official) 
                            },
                            onDeletePost = { viewModel.deletePost(it) }
                        )
                    }
                }
                1 -> ResourcesScreen(viewModel = viewModel, onResourceClick = onResourceClick)
                2 -> AdminAlertsView(alerts = viewModel.adminAlerts)
            }
        }
    }
}

@Composable
fun AdminAlertsView(alerts: List<AdminAlert>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Espace Sécurité", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
            Text("Messages détectés comme sensibles par l'IA.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))
        }

        if (alerts.isEmpty()) {
            item { Text("Aucune alerte en cours. Le vestiaire est calme.", color = Color.White.copy(alpha = 0.5f)) }
        } else {
            items(alerts) { alert ->
                AlertCard(alert)
            }
        }
    }
}

@Composable
fun AlertCard(alert: AdminAlert) {
    val isUrgent = alert.alertLevel == "urgent"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) Color.Red.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, if (isUrgent) Color.Red.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isUrgent) Color.Red else Color(0xFFFF9800),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        alert.alertLevel.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("ID Auteur: ${alert.authorId.take(8)}...", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                Spacer(Modifier.weight(1f))
                Text(alert.statut.replace("_", " ").uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
            }
            
            Spacer(Modifier.height(16.dp))
            Text("RAISON: ${alert.raison}", fontWeight = FontWeight.Bold, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))
            
            Text(alert.originalTitle, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(alert.originalContent, color = Color.White.copy(alpha = 0.8f))
            
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* Traiter */ }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Marquer traité", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { /* Contacter */ }, 
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salon privé", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
