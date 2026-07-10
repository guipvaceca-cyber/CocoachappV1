package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.coachapp.data.SeasonConfig
import com.example.coachapp.data.TrainingSession
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun SessionBuilderScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig,
    initialSessionId: String? = null,
    onUpdateSession: (TrainingSession) -> Unit
) {
    val sortedSessions = remember(seasonConfig.plannedTrainings) {
        seasonConfig.plannedTrainings.sortedBy { it.date }
    }
    
    var selectedSessionId by remember { mutableStateOf(initialSessionId ?: sortedSessions.firstOrNull()?.id) }
    
    // Force update if initialSessionId changes
    LaunchedEffect(initialSessionId) {
        if (initialSessionId != null) {
            selectedSessionId = initialSessionId
        }
    }
    val session = remember(selectedSessionId, seasonConfig.plannedTrainings) {
        seasonConfig.plannedTrainings.find { it.id == selectedSessionId }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Préparateur de Séance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (sortedSessions.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    val dateStr = session?.date?.format(DateTimeFormatter.ofPattern("dd/MM")) ?: "Choisir une séance"
                    val teamName = seasonConfig.teams.find { it.id == session?.teamId }?.name ?: ""
                    Text("$dateStr - $teamName : ${session?.focusArea ?: "Thème non défini"}")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    sortedSessions.forEach { s ->
                        val team = seasonConfig.teams.find { it.id == s.teamId }
                        DropdownMenuItem(
                            text = { Text("${s.date.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${team?.name} : ${s.focusArea ?: "Sans thème"}") },
                            onClick = { selectedSessionId = s.id; expanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (session != null) {
            val totalPlanned = session.warmupDuration + session.drillsDuration + session.smallGroupDuration + session.collectiveDuration
            val isOverTime = totalPlanned > session.durationMinutes

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text("MES INTENTIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    IntentionCard(
                        title = "🔍 Intention d'Entraîneur",
                        subtitle = "Sur quel aspect technique porter mon attention ?",
                        value = session.trainerIntentions,
                        onValueChange = { onUpdateSession(session.copy(trainerIntentions = it)) }
                    )
                    
                    IntentionCard(
                        title = "🧠 Intention de Coach",
                        subtitle = "Comment amener mon élève au but ?",
                        value = session.coachIntentions,
                        onValueChange = { onUpdateSession(session.copy(coachIntentions = it)) }
                    )
                    
                    Spacer(Modifier.height(24.dp))
                }

                if (session.noteForFutureMe.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE67E22))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFE67E22))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("NOTE DE LA DERNIÈRE FOIS :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE67E22))
                                    Text(session.noteForFutureMe, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("STRUCTURE & TIMING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${totalPlanned} / ${session.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = if (isOverTime) Color.Red else Color.Gray)
                    }
                    if (isOverTime) {
                        Text("Attention : le cumul dépasse la durée de séance (${session.durationMinutes} min)", color = Color.Red, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item { PhaseCard("🤸 Échauffement", session.warmup, session.warmupDuration) { text, dur -> onUpdateSession(session.copy(warmup = text, warmupDuration = dur)) } }
                item { PhaseCard("🎯 Gammes", session.drills, session.drillsDuration) { text, dur -> onUpdateSession(session.copy(drills = text, drillsDuration = dur)) } }
                item { PhaseCard("👥 Situations réduites", session.smallGroupSituations, session.smallGroupDuration) { text, dur -> onUpdateSession(session.copy(smallGroupSituations = text, smallGroupDuration = dur)) } }
                item { PhaseCard("🎮 Jeu collectif", session.collectiveGame, session.collectiveDuration) { text, dur -> onUpdateSession(session.copy(collectiveGame = text, collectiveDuration = dur)) } }
                
                item {
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { onUpdateSession(session.copy(isValidated = true)) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (session.isValidated) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (session.isValidated) Icons.Default.CheckCircle else Icons.Default.CloudDone, null)
                        Spacer(Modifier.width(12.dp))
                        Text(if (session.isValidated) "SÉANCE VALIDÉE" else "VALIDER POUR LE TERRAIN", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun IntentionCard(title: String, subtitle: String, value: String, onValueChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("Ex: Placement des pieds, communication...") }
            )
        }
    }
}

@Composable
fun PhaseCard(label: String, text: String, duration: Int, onUpdate: (String, Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                
                // Duration Control
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp)) {
                    IconButton(onClick = { if(duration > 5) onUpdate(text, duration - 5) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                    }
                    Text("$duration min", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { onUpdate(text, duration + 5) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { onUpdate(it, duration) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text("Détail de l'exercice...") },
                trailingIcon = {
                    IconButton(onClick = { /* Simulation IA */ onUpdate(text + " [Dictée IA]", duration) }) {
                        Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}
