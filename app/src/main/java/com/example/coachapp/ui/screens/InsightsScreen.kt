package com.example.coachapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import java.time.LocalDate

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig,
    onNavigate: (String) -> Unit = {}
) {
    val backgroundColor = Color(0xFF0A111F)
    val glassBg = Color(0xFF162133).copy(alpha = 0.8f)
    val accentCyan = Color(0xFF00D2FF)
    val accentPink = Color(0xFFFC2E7F)
    val accentGreen = Color(0xFF4ADE80)

    var showContactDialog by remember { mutableStateOf(false) }
    var showFormationDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    val today = LocalDate.now()

    // --- LOGIQUE DE CALCUL SEGMENTÉE ---
    val pastThemeStats = remember(seasonConfig.plannedTrainings) {
        val completedSessions = seasonConfig.plannedTrainings
            .filter { (it.date.isBefore(today) || it.isValidated) && it.focusArea != null }
        
        val total = completedSessions.size.coerceAtLeast(1)
        completedSessions
            .groupBy { it.focusArea!! }
            .mapValues { (it.value.size.toFloat() / total * 100).toInt() }
            .toList()
            .sortedByDescending { it.second }
    }

    val futureThemeStats = remember(seasonConfig.plannedTrainings) {
        val plannedSessions = seasonConfig.plannedTrainings
            .filter { it.date.isAfter(today.minusDays(1)) && !it.isValidated && it.focusArea != null }
        
        val total = plannedSessions.size.coerceAtLeast(1)
        plannedSessions
            .groupBy { it.focusArea!! }
            .mapValues { (it.value.size.toFloat() / total * 100).toInt() }
            .toList()
            .sortedByDescending { it.second }
    }

    val alerts = remember(pastThemeStats, futureThemeStats, seasonConfig.players) {
        val list = mutableListOf<String>()
        
        // 1. Alerte Déséquilibre
        pastThemeStats.forEach { (theme, percentage) ->
            if (percentage > 40) {
                val avgTech = seasonConfig.players.map { it.techScore }.average()
                if (avgTech < 3.0) {
                    list.add("Optimisation : Le thème '$theme' a occupé $percentage% de vos séances passées, mais le niveau technique global reste faible (%.1f/5).".format(avgTech))
                }
            }
        }

        val avgTechGlobal = seasonConfig.players.map { it.techScore }.average()
        if (avgTechGlobal < 3.2 && futureThemeStats.isEmpty()) {
            list.add("Planification : Votre niveau technique global est en zone de vigilance, prévoyez des thématiques ciblées.")
        }

        if (pastThemeStats.isNotEmpty() && futureThemeStats.isNotEmpty()) {
            val topPast = pastThemeStats.first().first
            val isContinuing = futureThemeStats.any { it.first == topPast }
            if (!isContinuing && avgTechGlobal < 3.5) {
                list.add("Continuité : Prévoyez un rappel de '$topPast' pour stabiliser les acquis.")
            }
        }
        
        if (seasonConfig.plannedTrainings.size > 15) {
            val attendanceAvg = seasonConfig.players.map { p -> 
                val sessions = seasonConfig.plannedTrainings.filter { it.teamId == p.teamId }
                val attended = sessions.count { it.attendance[p.id] == "present" }
                if (sessions.isNotEmpty()) attended.toFloat() / sessions.size else 1f
            }.average()
            if (attendanceAvg < 0.65) {
                list.add("Engagement : L'assiduité moyenne est de ${(attendanceAvg * 100).toInt()}%. Priorité : Cohésion de groupe.")
            }
        }
        list
    }

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        Box(modifier = Modifier.offset(x = (-100).dp, y = 100.dp).size(300.dp).background(accentCyan.copy(alpha = 0.1f), CircleShape).blur(80.dp))
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).size(350.dp).background(accentPink.copy(alpha = 0.1f), CircleShape).blur(100.dp))

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text("CoCoach Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                Text("Analyse de performance et pertinence", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("RÉPARTITION DES THÈMES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentCyan)
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(modifier = Modifier.fillMaxWidth(), color = glassBg, shape = RoundedCornerShape(16.dp), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
                    Column {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = accentCyan,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = accentCyan, height = 2.dp)
                            }
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                Text("RÉALISÉ", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, fontWeight = if(selectedTab == 0) FontWeight.Bold else FontWeight.Normal, color = if(selectedTab == 0) Color.White else Color.White.copy(alpha = 0.5f))
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("PLANIFIÉ", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, fontWeight = if(selectedTab == 1) FontWeight.Bold else FontWeight.Normal, color = if(selectedTab == 1) Color.White else Color.White.copy(alpha = 0.5f))
                            }
                        }
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            val currentStats = if (selectedTab == 0) pastThemeStats else futureThemeStats
                            val progressBarColor = if (selectedTab == 0) accentGreen else accentCyan
                            
                            if (currentStats.isEmpty()) {
                                Text(text = if (selectedTab == 0) "Aucune séance validée." else "Aucun thème planifié.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))
                            } else {
                                currentStats.forEach { (theme, percentage) ->
                                    ThemeProgressRow(theme, percentage, progressBarColor)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("POINTS DE VIGILANCE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentPink)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (alerts.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = glassBg), border = BorderStroke(0.5.dp, accentGreen.copy(alpha = 0.3f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = accentGreen)
                            Spacer(Modifier.width(12.dp))
                            Text("Aucune anomalie détectée.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
            } else {
                items(alerts, key = { it }) { alert ->
                    Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = accentPink.copy(alpha = 0.05f)), border = BorderStroke(0.5.dp, accentPink.copy(alpha = 0.2f))) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Warning, null, tint = accentPink, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(alert, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("RESSOURCES & ACCOMPAGNEMENT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))
                
                ActionCard(title = "Contacter le CTD", subtitle = "Besoin d'un regard extérieur ?", icon = Icons.Default.ContactSupport, color = accentCyan, onClick = { showContactDialog = true })
                ActionCard(title = "Le Vestiaire Anonyme", subtitle = "Partagez vos doutes avec la communauté.", icon = Icons.Default.Forum, color = accentPink, onClick = { onNavigate("COACH_SPACE_VESTIAIRE") })
                ActionCard(title = "Formations", subtitle = "Catalogue et pré-inscriptions.", icon = Icons.Default.School, color = accentGreen, onClick = { showFormationDialog = true })
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showContactDialog) {
        ContactCTDDialog(
            onDismiss = { showContactDialog = false },
            onSend = { showContactDialog = false }
        )
    }

    if (showFormationDialog) {
        FormationAssistantDialog(currentLevel = seasonConfig.coachProfile.formationLevel, acquiredModules = seasonConfig.coachProfile.acquiredModules, onDismiss = { showFormationDialog = false }, onOpenLink = { uri -> uriHandler.openUri(uri) })
    }
}

@Composable
fun ThemeProgressRow(label: String, percentage: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            Text("$percentage%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), strokeCap = StrokeCap.Round, color = color, trackColor = Color.White.copy(alpha = 0.05f))
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF162133).copy(alpha = 0.5f)), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(44.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun FormationAssistantDialog(currentLevel: String, acquiredModules: List<String>, onDismiss: () -> Unit, onOpenLink: (String) -> Unit) {
    val currentDiploma = ffvbDiplomas.find { it.name == currentLevel }
    val nextDiploma = ffvbDiplomas.getOrNull(ffvbDiplomas.indexOf(currentDiploma) + 1)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFC2E7F)); Spacer(Modifier.width(12.dp)); Text("Assistant Formation IA") } },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (currentLevel == "Novice") {
                    Text("Bonjour Coach ! Tu débutes l'aventure. Le meilleur point de départ est le niveau départemental.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    FormationOptionCard(title = "Découvrir les bases (JAPS / AFJ)", subtitle = "Formation Départementale • ~3h à 12h", icon = Icons.Default.LocationOn, onClick = { onOpenLink("https://www.volley2607.fr/formation/") })
                } else {
                    Text("Bonjour Coach ! Je vois que tu es **$currentLevel**. Que souhaites-tu faire ?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    FormationOptionCard(title = "A) Me recycler", subtitle = "Revoir les nouveautés du $currentLevel\n${currentDiploma?.globalScope?.label ?: "Fédéral"} • ${currentDiploma?.totalHours ?: "?"}h", icon = Icons.Default.Refresh, onClick = { onOpenLink("https://catalogue.ffvolley.org/") })
                    val missingModules = currentDiploma?.modules?.filter { !acquiredModules.contains(it.id) } ?: emptyList()
                    if (missingModules.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        FormationOptionCard(title = "B) Passer un module", subtitle = "Compléter mon ${currentDiploma?.name} :\n" + missingModules.joinToString("\n") { "• ${it.label} (${it.hours}h)" }, icon = Icons.Default.AddCircleOutline, onClick = { onOpenLink("https://catalogue.ffvolley.org/") })
                    }
                    if (nextDiploma != null) {
                        val canUpgrade = missingModules.isEmpty()
                        Spacer(Modifier.height(12.dp))
                        FormationOptionCard(title = "C) Monter en compétences", subtitle = if (canUpgrade) "Viser le ${nextDiploma.name} (${nextDiploma.level})\n${nextDiploma.globalScope.label} • ${nextDiploma.totalHours}h" else "Termine d'abord ton ${currentLevel} pour viser le ${nextDiploma.name}.", icon = Icons.AutoMirrored.Filled.TrendingUp, enabled = canUpgrade, onClick = { onOpenLink("https://catalogue.ffvolley.org/") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
fun FormationOptionCard(title: String, subtitle: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f).clickable(enabled = enabled) { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF162133).copy(alpha = 0.5f)), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF00D2FF), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun ContactCTDDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ContactSupport, null, tint = Color(0xFF00D2FF)); Spacer(Modifier.width(12.dp)); Text("Contacter le CTD") } },
        text = {
            Column {
                Text("Besoin d'un conseil ou d'une visite sur l'un de vos créneaux ?", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Votre message") }, placeholder = { Text("Ex: J'aimerais un avis sur ma planification M13...") }, modifier = Modifier.fillMaxWidth().height(150.dp), maxLines = 5)
            }
        },
        confirmButton = {
            Button(onClick = { isSending = true; onSend(message) }, enabled = message.isNotBlank() && !isSending) {
                if (isSending) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("Envoyer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSending) { Text("Annuler") } }
    )
}
