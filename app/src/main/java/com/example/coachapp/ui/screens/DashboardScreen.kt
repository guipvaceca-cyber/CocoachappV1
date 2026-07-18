package com.example.coachapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.coachapp.data.*
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
    onNavigate: (String) -> Unit = {},
    onUpdateAttendance: (String, String) -> Unit = { _, _ -> }
) {
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    
    val clubEvents = remember(seasonConfig.clubEvents) {
        seasonConfig.clubEvents.filter { !it.date.isBefore(today) }.sortedBy { it.date }
    }

    var selectedEventForAttendance by remember { mutableStateOf<ClubEvent?>(null) }
    
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
            .background(Color(0xFF001529)) // Solid Deep Dark Blue
            .statusBarsPadding()
    ) {
        item { HeaderSection() }

        item {
            ContextualCard(state = contextState, session = todaysTraining, onNavigate = onNavigate)
        }

        // --- CLUB EVENTS SECTION ---
        if (clubEvents.isNotEmpty()) {
            item {
                SectionHeader(title = "ÉVÉNEMENTS CLUB")
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clubEvents, key = { it.id }) { event ->
                        val status = seasonConfig.clubEventRegistrations[event.id] ?: "pending"
                        ClubEventCard(
                            event = event, 
                            status = status,
                            onClick = { selectedEventForAttendance = event }
                        )
                    }
                }
            }
        }

        // --- READY FOR FIELD SECTION (STACKED CAROUSEL) ---
        if (readySessionsByTeam.isNotEmpty()) {
            item {
                SectionHeader(title = "PRÊT POUR LE TERRAIN", color = Color.White.copy(alpha = 0.8f))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp) // Spacing for stack visuals
                ) {
                    items(readySessionsByTeam.toList(), key = { it.first }) { (teamId, sessions) ->
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isAnalysisExpanded = !isAnalysisExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ANALYSE DE COMPÉTENCES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            if (!isAnalysisExpanded) {
                                Text("Cliquez pour déployer le radar", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                        Icon(
                            imageVector = if (isAnalysisExpanded) Icons.Default.Remove else Icons.Default.Add,
                            contentDescription = if (isAnalysisExpanded) "Réduire" else "Déployer",
                            tint = Color.White
                        )
                    }
                    
                    if (isAnalysisExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (flashResults == null && globalResults == null) {
                            Text("Aucune donnée disponible. Réalisez votre premier diagnostic pour voir vos progrès.", 
                                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            Text("Session (Bleu) vs Global (Violet)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
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
            SectionHeader(title = "PÔLES DE COMPÉTENCES")
        }

        // Grille de modules
        item {
            val modules = getMainPoles()
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                modules.chunked(2).forEach { rowModules ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowModules.forEach { item ->
                            ModuleCard(item, modifier = Modifier.weight(1f), onClick = { onNavigate(item.id) })
                        }
                        if (rowModules.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    if (selectedEventForAttendance != null) {
        ClubEventAttendanceSheet(
            event = selectedEventForAttendance!!,
            currentStatus = seasonConfig.clubEventRegistrations[selectedEventForAttendance!!.id] ?: "pending",
            onStatusSelected = { status ->
                onUpdateAttendance(selectedEventForAttendance!!.id, status)
                selectedEventForAttendance = null
            },
            onDismiss = { selectedEventForAttendance = null }
        )
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

    Box(modifier = Modifier.width(210.dp).height(150.dp)) {
        displaySessions.forEachIndexed { index, session ->
            val isTop = index == displaySessions.size - 1
            // Offset for visual stacking effect - Increased for better accessibility
            val xOffset = (index * 12).dp
            val yOffset = (index * 8).dp
            
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFront) (team?.color ?: Color.Black) else Color.White.copy(alpha = 0.1f)
        ),
        border = BorderStroke(0.5.dp, if (isFront) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFront) 8.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (!isFront) {
                    Surface(modifier = Modifier.size(6.dp), shape = CircleShape, color = team?.color ?: Color(0xFF4CAF50)) {}
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    session.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    color = if (isFront) Color.White else Color.Black,
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
                color = if (isFront) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isFront) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("LANCER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            } else {
                Text(
                    team?.name ?: "Catégorie", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Black.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

enum class DashboardState { PREP, FIELD, DEBRIEF, ANALYSE }

@Composable
fun ContextualCard(state: DashboardState, session: TrainingSession?, onNavigate: (String) -> Unit) {
    val targetColor = when(state) {
        DashboardState.PREP -> Color(0xFF003399).copy(alpha = 0.4f)
        DashboardState.FIELD -> Color(0xFFD32F2F).copy(alpha = 0.4f)
        DashboardState.DEBRIEF -> Color(0xFF388E3C).copy(alpha = 0.4f)
        DashboardState.ANALYSE -> Color.White.copy(alpha = 0.08f)
    }
    
    val glowColor = when(state) {
        DashboardState.PREP -> Color(0xFF2196F3).copy(alpha = 0.12f)
        DashboardState.FIELD -> Color(0xFFFF5252).copy(alpha = 0.12f)
        DashboardState.DEBRIEF -> Color(0xFF66BB6A).copy(alpha = 0.12f)
        DashboardState.ANALYSE -> Color.White.copy(alpha = 0.05f)
    }
    
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600),
        label = "ContextCardColor"
    )

    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = animatedColor),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Inner Glow / Gradient
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(0f, 0f),
                            radius = 500f
                        )
                    )
            )
            
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(state) {
                            DashboardState.PREP -> Icons.Default.EditNote
                            DashboardState.FIELD -> Icons.Default.Timer
                            DashboardState.DEBRIEF -> Icons.Default.Mic
                            DashboardState.ANALYSE -> Icons.Default.AutoGraph
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = when(state) {
                            DashboardState.PREP -> "PRÉPARATION"
                            DashboardState.FIELD -> "SESSION EN COURS"
                            DashboardState.DEBRIEF -> "DÉBRIEFING"
                            DashboardState.ANALYSE -> "VOTRE SAISON"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                if (state == DashboardState.FIELD && session != null) {
                    Text(session.focusArea ?: "Général", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    val summary = listOf("🤸 " + session.warmup, "🎯 " + session.drills, "👥 " + session.smallGroupSituations, "🎮 " + session.collectiveGame).filter { it.length > 5 }
                    if (summary.isNotEmpty()) {
                        summary.take(2).forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.width(4.dp))
                        Text("Fin prévue à : ${session.startTime.plusMinutes(session.durationMinutes.toLong())}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        onNavigate(when(state) {
                            DashboardState.PREP -> "PREPARER"
                            DashboardState.FIELD -> "COMPANION" 
                            DashboardState.DEBRIEF -> "DIAGNOSTIC_FLASH"
                            DashboardState.ANALYSE -> "INSIGHTS"
                        })
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text(
                        text = when(state) {
                            DashboardState.PREP -> "Préparer ma séance"
                            DashboardState.FIELD -> "Lancer l'Entraînement"
                            DashboardState.DEBRIEF -> "Mener l'évaluation"
                            DashboardState.ANALYSE -> "Voir mes Analyses"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

enum class AppUpdateStatus { UP_TO_DATE, UPDATE_AVAILABLE }

@Composable
fun HeaderSection() {
    val updateStatus = AppUpdateStatus.UP_TO_DATE // Mock status

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top Section: Comite Logo on Global Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) 
                .background(Color(0xFF001529)), // Same as global background
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.comitda),
                contentDescription = "Comité Logo",
                modifier = Modifier.height(65.dp), // Reduced size by ~15%
                contentScale = ContentScale.Fit
            )
        }

        // Bottom Section: CoCoach Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cocoach_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "CoCoach",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "v0.4-RC",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                // App Update Status
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    val (label, color, icon) = when (updateStatus) {
                                        AppUpdateStatus.UP_TO_DATE -> Triple(
                                            "À JOUR",
                                            Color(0xFF2196F3), // Blue
                                            Icons.Default.Verified
                                        )
                                        AppUpdateStatus.UPDATE_AVAILABLE -> Triple(
                                            "MAJ DISPO",
                                            Color(0xFFFF9800), // Orange
                                            Icons.Default.ArrowCircleUp
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        color = color,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Drôme Ardèche Volley Connect",
                                color = Color(0xFF00B4D8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
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
fun ModuleCard(item: ModuleItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "ModuleScale"
    )

    Card(
        modifier = modifier
            .height(110.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = item.color),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Aura behind icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(item.color.copy(alpha = 0.15f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                
                Surface(
                    color = item.color.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    border = BorderStroke(1.dp, item.color.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color = Color(0xFF00B4D8)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ClubEventCard(event: ClubEvent, status: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(220.dp).height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when(event.type) {
                        ClubEventType.TOURNOI -> Color(0xFFFF9800)
                        ClubEventType.SOIRÉE -> Color(0xFFE91E63)
                        ClubEventType.RÉUNION -> Color(0xFF2196F3)
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(event.type.name, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.weight(1f))
                
                // Status Badge
                Surface(
                    color = when(status) {
                        "present" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "absent" -> Color(0xFFE91E63).copy(alpha = 0.2f)
                        else -> Color.White.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, when(status) {
                        "present" -> Color(0xFF4CAF50)
                        "absent" -> Color(0xFFE91E63)
                        else -> Color.White.copy(alpha = 0.3f)
                    })
                ) {
                    Text(
                        text = when(status) {
                            "present" -> "PRÉSENT"
                            "absent" -> "ABSENT"
                            else -> "ATTENTE"
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = when(status) {
                            "present" -> Color(0xFF4CAF50)
                            "absent" -> Color(0xFFE91E63)
                            else -> Color.White.copy(alpha = 0.6f)
                        }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(event.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, null, modifier = Modifier.size(10.dp), tint = Color.White.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text(event.location, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubEventAttendanceSheet(
    event: ClubEvent,
    currentStatus: String,
    onStatusSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF001529),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "${event.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))} à ${event.startTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF00B4D8)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Votre participation :",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttendanceOption(
                    label = "PRÉSENT",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    isSelected = currentStatus == "present",
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected("present") }
                )
                AttendanceOption(
                    label = "ABSENT",
                    icon = Icons.Default.Cancel,
                    color = Color(0xFFE91E63),
                    isSelected = currentStatus == "absent",
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected("absent") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Plus tard", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun AttendanceOption(
    label: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = if (isSelected) color else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
