package com.example.coachapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.coachapp.R
import com.example.coachapp.data.SeasonConfig
import com.example.coachapp.data.Team
import com.example.coachapp.data.TrainingSession
import com.example.coachapp.ui.components.RadarChart
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    flashResults: Map<String, Double>? = null,
    globalResults: Map<String, Double>? = null,
    seasonConfig: SeasonConfig = SeasonConfig(),
    onNavigate: (String) -> Unit = {}
) {
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    
    val todaysTraining = remember(seasonConfig) {
        seasonConfig.plannedTrainings.find { it.date == today }
    }
    
    val contextState = remember(todaysTraining, now) {
        when {
            todaysTraining == null -> DashboardState.ANALYSE
            now.toLocalTime().isAfter(todaysTraining.startTime.plusMinutes(todaysTraining.durationMinutes.toLong())) &&
            now.toLocalTime().isBefore(todaysTraining.startTime.plusMinutes(todaysTraining.durationMinutes.toLong() + 180)) -> DashboardState.DEBRIEF
            todaysTraining.isValidated || (
                now.toLocalTime().isAfter(todaysTraining.startTime.minusMinutes(15)) && 
                now.toLocalTime().isBefore(todaysTraining.startTime.plusMinutes(todaysTraining.durationMinutes.toLong()))
            ) -> DashboardState.FIELD
            now.toLocalTime().isBefore(todaysTraining.startTime) -> DashboardState.PREP
            else -> DashboardState.ANALYSE
        }
    }

    val readySessionsByTeam = remember(seasonConfig.plannedTrainings) {
        seasonConfig.plannedTrainings
            .filter { it.isValidated && !it.date.isBefore(today) }
            .sortedBy { it.date }
            .groupBy { it.teamId }
    }

    var isAnalysisExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { HeaderSection() }

        item {
            ContextualCard(state = contextState, session = todaysTraining, onNavigate = onNavigate)
        }

        // --- READY FOR FIELD SECTION (STACKED CAROUSEL) ---
        if (readySessionsByTeam.isNotEmpty()) {
            item {
                Text(
                    text = "PRÊT POUR LE TERRAIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp) // Spacing for stack visuals
                ) {
                    items(readySessionsByTeam.toList()) { (teamId, sessions) ->
                        val team = seasonConfig.teams.find { it.id == teamId }
                        TeamSessionStack(team, sessions, onSessionClick = { onNavigate("COMPANION_${it.id}") })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isAnalysisExpanded = !isAnalysisExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ANALYSE DE COMPÉTENCES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (!isAnalysisExpanded) {
                                Text("Cliquez pour déployer le radar", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Icon(
                            imageVector = if (isAnalysisExpanded) Icons.Default.Remove else Icons.Default.Add,
                            contentDescription = if (isAnalysisExpanded) "Réduire" else "Déployer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (isAnalysisExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (flashResults == null && globalResults == null) {
                            Text("Aucune donnée disponible. Réalisez votre premier diagnostic pour voir vos progrès.", 
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            Text("Session (Bleu) vs Global (Violet)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                                RadarChart(flashScores = flashResults, globalScores = globalResults, modifier = Modifier.size(230.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "PÔLES DE COMPÉTENCES",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(getMainPoles()) { item ->
            ModuleRow(item, onClick = { onNavigate(item.id) })
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun TeamSessionStack(team: Team?, sessions: List<TrainingSession>, onSessionClick: (TrainingSession) -> Unit) {
    // focusedIndex tracks which session in the list is currently at the front
    var focusedIndex by remember { mutableIntStateOf(0) }
    
    // Sort sessions to put the focused one at the end of the list (drawn last -> on top)
    val displaySessions = remember(sessions, focusedIndex) {
        val list = sessions.toMutableList()
        if (focusedIndex < list.size) {
            val item = list.removeAt(focusedIndex)
            list.add(item) // Add to end to be on top
        }
        list
    }

    Box(modifier = Modifier.width(180.dp).height(130.dp)) {
        displaySessions.forEachIndexed { index, session ->
            val isTop = index == displaySessions.size - 1
            // Offset for visual stacking effect
            val xOffset = (index * 6).dp
            val yOffset = (index * 4).dp
            
            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .zIndex(index.toFloat())
            ) {
                ReadySessionCard(
                    session = session,
                    team = team,
                    isFront = isTop,
                    onClick = { 
                        if (isTop) {
                            onSessionClick(session)
                        } else {
                            // Find the original index in the 'sessions' list to update focusedIndex
                            val originalIndex = sessions.indexOf(session)
                            if (originalIndex != -1) {
                                focusedIndex = originalIndex
                            }
                        }
                    }
                )
            }
        }
        
        // Count indicator if more than 3 sessions
        if (sessions.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp).size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${sessions.size}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReadySessionCard(session: TrainingSession, team: Team?, isFront: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp).height(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFront) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        border = if (isFront) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFront) 8.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(modifier = Modifier.size(6.dp), shape = CircleShape, color = team?.color ?: Color(0xFF4CAF50)) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    session.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = session.focusArea ?: "Session Générale", 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 14.sp, 
                maxLines = 2, 
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
                color = if (isFront) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isFront) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("LANCER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text(
                    team?.name ?: "Catégorie", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

enum class DashboardState { PREP, FIELD, DEBRIEF, ANALYSE }

@Composable
fun ContextualCard(state: DashboardState, session: TrainingSession?, onNavigate: (String) -> Unit) {
    val cardColor = when(state) {
        DashboardState.PREP -> MaterialTheme.colorScheme.primaryContainer
        DashboardState.FIELD -> Color(0xFFF44336)
        DashboardState.DEBRIEF -> Color(0xFF4CAF50)
        DashboardState.ANALYSE -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when(state) {
                        DashboardState.PREP -> Icons.Default.EditNote
                        DashboardState.FIELD -> Icons.Default.Timer
                        DashboardState.DEBRIEF -> Icons.Default.Mic
                        DashboardState.ANALYSE -> Icons.Default.AutoGraph
                    },
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when(state) {
                        DashboardState.PREP -> "PRÉPARATION"
                        DashboardState.FIELD -> "SESSION EN COURS"
                        DashboardState.DEBRIEF -> "DÉBRIEFING"
                        DashboardState.ANALYSE -> "VOTRE SAISON"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (state == DashboardState.FIELD && session != null) {
                Text(session.focusArea ?: "Général", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val summary = listOf("🤸 " + session.warmup, "🎯 " + session.drills, "👥 " + session.smallGroupSituations, "🎮 " + session.collectiveGame).filter { it.length > 5 }
                if (summary.isNotEmpty()) {
                    summary.take(2).forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Fin prévue à : ${session.startTime.plusMinutes(session.durationMinutes.toLong())}", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = when(state) {
                        DashboardState.PREP -> "Finalisez la préparation de votre séance ${session?.focusArea ?: ""}."
                        DashboardState.FIELD -> "Ouvrez vos outils de terrain (Chrono, Scoreur, Présences)."
                        DashboardState.DEBRIEF -> "Séance terminée ! Enregistrez votre ressenti à chaud."
                        DashboardState.ANALYSE -> "Consultez vos statistiques de progression et vos ressources."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = {
                    onNavigate(when(state) {
                        DashboardState.PREP -> "PREPARER"
                        DashboardState.FIELD -> "COMPANION" 
                        DashboardState.DEBRIEF -> "DIAGNOSTIC_FLASH"
                        DashboardState.ANALYSE -> "INSIGHTS"
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text(
                    text = when(state) {
                        DashboardState.PREP -> "Préparer ma séance"
                        DashboardState.FIELD -> "Lancer l'Entraînement"
                        DashboardState.DEBRIEF -> "Mener l'évaluation"
                        DashboardState.ANALYSE -> "Voir mes Analyses"
                    },
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp).background(
            brush = Brush.verticalGradient(colors = listOf(Color.Black, Color.Transparent))
        )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.fillMaxWidth().height(70.dp), color = Color.Black, shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), color = Color.Transparent) {
                        Image(painter = painterResource(id = R.drawable.ic_cocoach_logo), contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "CoCoach", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-1).sp)
                            Spacer(Modifier.width(4.dp))
                            Text(text = "v0.4-RC", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text(text = "FFvolley\nDrôme Ardèche", color = Color.White, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

data class ModuleItem(val id: String, val title: String, val description: String, val icon: ImageVector, val color: Color)

fun getMainPoles() = listOf(
    ModuleItem("CALENDAR", "Ma Saison", "Calendrier, Préparateur et Bilans de séances.", Icons.Default.DateRange, Color(0xFFE91E63)),
    ModuleItem("TEAM_HUB", "Mes Collectifs", "Équipes, Joueurs, Paliers et Planning Hebdo.", Icons.Default.Groups, Color(0xFF4CAF50)),
    ModuleItem("COMPANION", "Terrain", "Session Live ou Match Dashboard.", Icons.Default.PlayCircle, Color(0xFF2196F3)),
    ModuleItem("COACH_SPACE", "Espace Coachs", "Vestiaire Anonyme et Labo Pédagogique.", Icons.Default.Forum, Color(0xFFFF9800)),
    ModuleItem("INSIGHTS", "Insights", "Analyses de performance et statistiques.", Icons.Default.AutoGraph, Color(0xFF673AB7)),
    ModuleItem("PROFILE", "Mon Profil", "Identité, Diplômes et Bilan de Carrière.", Icons.Default.Person, Color(0xFF9C27B0))
)

@Composable
fun ModuleRow(item: ModuleItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = item.color.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = item.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}
