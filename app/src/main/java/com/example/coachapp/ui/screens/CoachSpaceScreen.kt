package com.example.coachapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coachapp.data.*

@Composable
fun CoachSpaceScreen(
    modifier: Modifier = Modifier,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    onResourceClick: (LaboResource) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = viewModel.coachSpaceTab) {
            Tab(selected = viewModel.coachSpaceTab == 0, onClick = { viewModel.coachSpaceTab = 0 }) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Forum, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vestiaire")
                }
            }
            Tab(selected = viewModel.coachSpaceTab == 1, onClick = { viewModel.coachSpaceTab = 1 }) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Science, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Laboratoire")
                }
            }
            if (viewModel.userRole == UserRole.MEGADMIN) {
                Tab(selected = viewModel.coachSpaceTab == 2, onClick = { viewModel.coachSpaceTab = 2 }) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Badge(containerColor = if (viewModel.adminAlerts.any { it.statut == "non_traite" }) Color.Red else Color.Gray) {
                            Icon(Icons.Default.Shield, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Alertes")
                    }
                }
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

@Composable
fun AdminAlertsView(alerts: List<AdminAlert>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Espace Sécurité (Megadmin)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Messages détectés comme sensibles par l'IA.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
        }

        if (alerts.isEmpty()) {
            item { Text("Aucune alerte en cours. Le vestiaire est calme.", color = Color.Gray) }
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
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) Color(0xFFFFF1F0) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUrgent) Color.Red else Color.Gray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isUrgent) Color.Red else Color(0xFFFFA500),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        alert.alertLevel.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("ID Auteur: ${alert.authorId.take(8)}...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.weight(1f))
                Text(alert.statut.replace("_", " "), style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(Modifier.height(12.dp))
            Text("RAISON: ${alert.raison}", fontWeight = FontWeight.Bold, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            
            Text("TITRE: ${alert.originalTitle}", fontWeight = FontWeight.Bold)
            Text(alert.originalContent)
            
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* Traiter */ }, modifier = Modifier.weight(1f)) {
                    Text("Marquer traité")
                }
                OutlinedButton(onClick = { /* Contacter */ }, modifier = Modifier.weight(1f)) {
                    Text("Ouvrir salon privé")
                }
            }
        }
    }
}
