package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.AssessmentRecord
import com.example.coachapp.data.SeasonConfig
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    history: List<AssessmentRecord> = emptyList(),
    seasonConfig: SeasonConfig = SeasonConfig(),
    userRole: com.example.coachapp.data.UserRole = com.example.coachapp.data.UserRole.USER,
    onUpdateConfig: (SeasonConfig) -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToGlobalAssessment: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var expandedSection by remember { mutableStateOf("IDENTITY") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mon Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    text = "Coach ${seasonConfig.coachProfile.nickname.ifEmpty { seasonConfig.coachProfile.firstName }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // --- BADGE DE RÔLE RÉEL ---
            RoleBadge(userRole)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 1 : IDENTITY ---
        ProfileExpandableSection(
            title = "Ma Carte d'Identité",
            description = "Gérez votre nom, surnom et club d'appartenance.",
            icon = Icons.Default.Person,
            isExpanded = expandedSection == "IDENTITY",
            onToggle = { expandedSection = if (expandedSection == "IDENTITY") "" else "IDENTITY" }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Prénom: ${seasonConfig.coachProfile.firstName}", fontWeight = FontWeight.Bold)
                Text("Nom: ${seasonConfig.coachProfile.lastName}")
                Text("Surnom: ${seasonConfig.coachProfile.nickname}")
                Text("Club: ${seasonConfig.coachProfile.clubName}")
                Text("Niveau: ${seasonConfig.coachProfile.formationLevel}")
                Text("Persona: ${seasonConfig.coachProfile.coachPersona}")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- SECTION 2 : HISTORY ---
        ProfileExpandableSection(
            title = "Mon Historique de Bilan",
            description = "Retrouvez vos diagnostics de carrière passés.",
            icon = Icons.Default.History,
            isExpanded = expandedSection == "HISTORY",
            onToggle = { expandedSection = if (expandedSection == "HISTORY") "" else "HISTORY" }
        ) {
            HistoryListInProfile(history, dateFormat)
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        OutlinedButton(
            onClick = onNavigateToGlobalAssessment,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Assessment, null)
            Spacer(Modifier.width(8.dp))
            Text("Lancer mon Bilan de Carrière")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Déconnexion & Réinitialisation", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoleBadge(role: com.example.coachapp.data.UserRole) {
    val (label, color, icon) = when (role) {
        com.example.coachapp.data.UserRole.USER -> Triple("USER", Color.Gray, Icons.Default.Person)
        com.example.coachapp.data.UserRole.ADMIN -> Triple("ADMIN", MaterialTheme.colorScheme.primary, Icons.Default.Shield)
        com.example.coachapp.data.UserRole.MEGADMIN -> Triple("MEGADMIN", Color.Red, Icons.Default.AutoAwesome)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, color)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ProfileExpandableSection(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (isExpanded) {
                Box(modifier = Modifier.padding(16.dp).padding(top = 0.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun HistoryListInProfile(history: List<AssessmentRecord>, dateFormat: SimpleDateFormat) {
    if (history.isEmpty()) {
        Text("Aucun historique pour le moment.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    } else {
        Column {
            history.reversed().forEach { record ->
                HistoryCard(record, dateFormat)
            }
        }
    }
}

@Composable
fun HistoryCard(record: AssessmentRecord, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Diagnostic du ${dateFormat.format(Date(record.date))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                record.scores.forEach { (id, score) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(id.take(3).uppercase(), style = MaterialTheme.typography.labelSmall)
                        Text("%.1f".format(score), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            record.coachNote?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f), shape = RoundedCornerShape(4.dp)) {
                    Text(it, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
