package com.example.coachapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import com.example.coachapp.ui.screens.SeasonCycle
import java.time.LocalDate
import java.util.UUID
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*

sealed class CalendarListItem {
    abstract val time: LocalTime
    abstract val teamId: String
    data class Training(val session: TrainingSession) : CalendarListItem() {
        override val time = session.startTime
        override val teamId = session.teamId
    }
    data class Comp(val event: CompetitionEvent) : CalendarListItem() {
        override val time = event.startTime
        override val teamId = event.teamId
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonCalendarScreen(
    modifier: Modifier = Modifier,
    persistenceManager: PersistenceManager,
    seasonConfig: SeasonConfig,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    presidentViewModel: com.example.coachapp.ui.president.PresidentViewModel,
    onUseHelp: () -> Unit,
    helpUsageCount: Int,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit
) {
    var config by remember { mutableStateOf(seasonConfig) }
    val presidentState by presidentViewModel.uiState.collectAsState()
    
    // Les vrais collectifs rattachés à ce coach (Source de vérité unique)
    val coachTeams = remember(presidentState) {
        if (presidentState is com.example.coachapp.ui.president.PresidentUiState.Success) {
            val userId = com.example.coachapp.data.SupabaseManager.auth.currentUserOrNull()?.id
            (presidentState as com.example.coachapp.ui.president.PresidentUiState.Success).collectifs
                .filter { detail -> detail.rattachements.any { it.coachId == userId } }
                .map { detail ->
                    Team(
                        id = detail.collectif.id,
                        name = detail.collectif.nom,
                        color = Color(0xFF2196F3), 
                        format = when(detail.collectif.format) {
                            "2x2" -> TeamFormat.TWO_TWO
                            "3x3" -> TeamFormat.THREE_THREE
                            "4x4" -> TeamFormat.FOUR_FOUR
                            else -> TeamFormat.SIX_SIX
                        }
                    )
                }
        } else emptyList()
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentView by remember { mutableStateOf("PROGRAM") }
    var isWeeklyMode by remember { mutableStateOf(false) }
    
    var activeFilterType by remember { mutableStateOf("MY_TEAMS") }
    var selectedTeamId by remember { mutableStateOf<String?>(null) }

    // Couleurs persistantes (choix coach)
    var coachTeamColors by remember { 
        val saved = persistenceManager.loadTeamColors()
        mutableStateOf(
            saved.mapValues { Color(it.value) }.toMutableMap()
        )
    }

    // Les joueurs réels extraits de Supabase
    val allCoachPlayers = remember(presidentState) {
        if (presidentState is com.example.coachapp.ui.president.PresidentUiState.Success) {
            (presidentState as com.example.coachapp.ui.president.PresidentUiState.Success).collectifs.flatMap { it.joueurs }
                .map { j -> 
                    Player(
                        id = j.id, 
                        teamId = (presidentState as com.example.coachapp.ui.president.PresidentUiState.Success).collectifs.find { it.joueurs.contains(j) }?.collectif?.id ?: "",
                        firstName = j.prenom, 
                        lastName = j.nom, 
                        number = 0, 
                        position = j.poste ?: "",
                        vivierId = j.vivierJoueurId
                    )
                }
        } else emptyList()
    }

    var showAddEvent by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showColorPickerForTeamId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // --- HEADER DYNAMIQUE ---
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = "2026-2027",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
            
            IconButton(
                onClick = { showHelpDialog = true },
                enabled = helpUsageCount < 3,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpCenter, 
                    null, 
                    tint = if (helpUsageCount < 3) MaterialTheme.colorScheme.error else Color.Gray
                )
            }
        }

        // --- BOUTON AJOUTER ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .height(56.dp)
                .clickable { showAddEvent = true },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Ajouter un évènement au planning",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- FILTRES ÉQUIPES (Plusieurs étages) ---
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = activeFilterType == "CLUB",
                onClick = { activeFilterType = "CLUB"; selectedTeamId = null },
                label = { Text("Club") }
            )
            
            FilterChip(
                selected = activeFilterType == "MY_TEAMS" && selectedTeamId == null,
                onClick = { activeFilterType = "MY_TEAMS"; selectedTeamId = null },
                label = { Text("Mes Équipes") }
            )
            
            coachTeams.forEach { team ->
                val color = coachTeamColors[team.id] ?: team.color
                FilterChip(
                    selected = selectedTeamId == team.id,
                    onClick = { 
                        if (selectedTeamId == team.id) showColorPickerForTeamId = team.id
                        else { selectedTeamId = team.id; activeFilterType = "SPECIFIC" }
                    },
                    label = { Text(team.name) },
                    leadingIcon = { Box(modifier = Modifier.size(10.dp).background(color, CircleShape)) }
                )
            }
        }

        // --- SWITCHER V1 (Texte plein, sous les filtres) ---
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                val views = listOf("PROGRAM" to "Programme", "MONTH" to "Calendrier", "PLANNING" to "Planification")
                views.forEach { (id, label) ->
                    val isSelected = currentView == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentView = id },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (currentView == "MONTH") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Vue ${if(isWeeklyMode) "Hebdomadaire" else "Mensuelle"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1f))
                TextButton(onClick = { isWeeklyMode = !isWeeklyMode }) {
                    Text(if(isWeeklyMode) "Voir le mois" else "Zoom semaine")
                }
            }
        }

        // --- CONTENU ---
        Box(modifier = Modifier.weight(1f)) {
            when (currentView) {
                "MONTH" -> {
                    val pPlanning by presidentViewModel.clubPlanning.collectAsState()
                    if (isWeeklyMode) {
                        WeeklyZoomView(
                            date = selectedDate, 
                            config = config, 
                            colors = coachTeamColors,
                            clubPlanning = pPlanning
                        )
                    } else {
                        // Utilisation d'une LazyColumn pour permettre le scroll de l'ensemble Calendrier + Liste
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                MonthView(
                                    currentDate = selectedDate,
                                    config = config,
                                    teamId = selectedTeamId,
                                    filterType = activeFilterType,
                                    teamColors = coachTeamColors,
                                    clubPlanning = pPlanning,
                                    onDateSelected = { selectedDate = it }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            item {
                                DailyEventsList(
                                    date = selectedDate,
                                    config = config,
                                    filterType = "ALL",
                                    teamId = selectedTeamId,
                                    teamColors = coachTeamColors,
                                    allPlayers = allCoachPlayers, // On passe les vrais joueurs
                                    clubPlanning = pPlanning,
                                    persistenceManager = persistenceManager,
                                    presidentViewModel = presidentViewModel,
                                    onNavigateToPreparer = onNavigateToPreparer,
                                    onViewRecap = onViewRecap,
                                    onUpdate = { config = persistenceManager.loadSeasonConfig() }
                                )
                            }
                            
                            item { Spacer(Modifier.height(100.dp)) }
                        }
                    }
                }
                "PLANNING" -> {
                    SeasonPlannerView(config, selectedTeamId, viewModel.cycles) { updated ->
                        updated.forEach { cycle ->
                            if (viewModel.cycles.any { it.id == cycle.id }) viewModel.modifierCycle(cycle)
                            else viewModel.ajouterCycle(cycle)
                        }
                    }
                }
                else -> {
                    val pPlanning by presidentViewModel.clubPlanning.collectAsState()
                    // PROGRAM - On garde le scroll interne propre à ProgramView
                    ProgramView(
                        config = config, 
                        filterType = activeFilterType, 
                        teamId = selectedTeamId, 
                        teamColors = coachTeamColors, 
                        allPlayers = allCoachPlayers, // On passe les vrais joueurs
                        clubPlanning = pPlanning,
                        persistenceManager = persistenceManager, 
                        presidentViewModel = presidentViewModel,
                        onNavigateToPreparer = onNavigateToPreparer, 
                        onViewRecap = onViewRecap,
                        onUpdate = { config = persistenceManager.loadSeasonConfig() }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showAddEvent) {
        AddEventDialog(
            date = selectedDate,
            teams = coachTeams,
            allPlayers = allCoachPlayers,
            initialTeamId = selectedTeamId ?: coachTeams.firstOrNull()?.id ?: "",
            teamColors = coachTeamColors,
            onDismiss = { showAddEvent = false },
            onConfirm = { event, onResult ->
                when (event) {
                    is CalendarListItem.Training -> {
                        val session = event.session
                        config = config.copy(plannedTrainings = config.plannedTrainings + session)
                        persistenceManager.saveSeasonConfig(config)
                        
                        presidentViewModel.pushSession(
                            session = session,
                            onSuccess = { 
                                onResult(true) 
                            },
                            onError = { 
                                onResult(false) 
                            }
                        )
                    }
                    is CalendarListItem.Comp -> {
                        val match = event.event
                        config = config.copy(competitions = config.competitions + match)
                        persistenceManager.saveSeasonConfig(config)
                        
                        presidentViewModel.pushMatch(
                            event = match,
                            onSuccess = { 
                                onResult(true) 
                            },
                            onError = { 
                                onResult(false) 
                            }
                        )
                    }
                }
            }
        )
    }

    if (showHelpDialog) {
        HelpSessionDialog(
            onDismiss = { showHelpDialog = false },
            onConfirm = { theme ->
                onUseHelp()
                showHelpDialog = false
                // On pré-remplit le préparateur avec ce thème
                if (coachTeams.isNotEmpty()) {
                    val targetTeam = selectedTeamId ?: coachTeams.first().id
                    val newSession = TrainingSession(
                        id = UUID.randomUUID().toString(),
                        teamId = targetTeam,
                        date = LocalDate.now(),
                        startTime = LocalTime.of(18, 30),
                        focusArea = theme
                    )
                    onNavigateToPreparer(newSession)
                }
            }
        )
    }

    if (showColorPickerForTeamId != null) {
        ColorPickerDialog(
            teamName = coachTeams.find { it.id == showColorPickerForTeamId }?.name ?: "",
            onColorSelected = { color ->
                val newColors = coachTeamColors.toMutableMap().apply { 
                    put(showColorPickerForTeamId!!, color) 
                }
                coachTeamColors = newColors
                // Sauvegarde persistante
                persistenceManager.saveTeamColors(newColors.mapValues { it.value.toArgb() })
                showColorPickerForTeamId = null
            },
            onDismiss = { showColorPickerForTeamId = null }
        )
    }
}

@Composable
fun PillNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "content"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = contentColor)
        AnimatedVisibility(visible = selected) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun ProgramView(
    config: SeasonConfig,
    filterType: String,
    teamId: String?,
    teamColors: Map<String, Color>,
    allPlayers: List<Player>, // Ajouté
    clubPlanning: List<TrainingSchedule> = emptyList(),
    persistenceManager: PersistenceManager,
    presidentViewModel: com.example.coachapp.ui.president.PresidentViewModel,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit,
    onUpdate: () -> Unit
) {
    val today = LocalDate.now()
    val weeksToShow = 8
    val activeDays = remember(config, filterType, teamId, clubPlanning) {
        val schedules = if (teamId == null) clubPlanning else clubPlanning.filter { it.teamId == teamId }
        val trainingDays = schedules.map { it.dayOfWeek }.toSet()
        
        val list = mutableListOf<LocalDate>()
        for (i in 0 until (weeksToShow * 7)) {
            val date = today.plusDays(i.toLong())
            val isTrainingDay = trainingDays.contains(date.dayOfWeek)
            val hasComp = config.competitions.any { (teamId == null || it.teamId == teamId) && it.date == date }
            val hasSession = config.plannedTrainings.any { (teamId == null || it.teamId == teamId) && it.date == date }
            
            if (isTrainingDay || hasComp || hasSession) {
                list.add(date)
            }
        }
        list
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeDays) { date ->
            val isComp = config.competitions.any { (teamId == null || it.teamId == teamId) && it.date == date }
            
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                if (date.dayOfWeek == java.time.DayOfWeek.MONDAY || date == activeDays.firstOrNull()) {
                    Text(
                        text = "SEMAINE DU ${date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH))}".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isComp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.FRENCH), fontSize = 10.sp, color = if (isComp) Color.White else Color.Unspecified)
                            Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, color = if (isComp) Color.White else Color.Unspecified)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        DailyEventsList(
                            date = date, 
                            config = config, 
                            filterType = filterType, 
                            teamId = teamId, 
                            teamColors = teamColors, 
                            allPlayers = allPlayers, // Transmis
                            clubPlanning = clubPlanning,
                            persistenceManager = persistenceManager, 
                            presidentViewModel = presidentViewModel,
                            onNavigateToPreparer = onNavigateToPreparer, 
                            onViewRecap = onViewRecap, 
                            onUpdate = onUpdate
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
            }
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun DailyEventsList(
    date: LocalDate, 
    config: SeasonConfig, 
    filterType: String, 
    teamId: String?, 
    teamColors: Map<String, Color> = emptyMap(),
    allPlayers: List<Player> = emptyList(), // Ajouté
    clubPlanning: List<TrainingSchedule> = emptyList(),
    persistenceManager: PersistenceManager, 
    presidentViewModel: com.example.coachapp.ui.president.PresidentViewModel,
    onNavigateToPreparer: (TrainingSession) -> Unit, 
    onViewRecap: (TrainingSession) -> Unit, 
    onUpdate: () -> Unit
) {
    val dayEvents = remember(date, config, filterType, teamId, clubPlanning) {
        val sessions = config.plannedTrainings.filter { it.date == date && (teamId == null || it.teamId == teamId) }
        val comps = config.competitions.filter { it.date == date && (teamId == null || it.teamId == teamId) }
        
        // On récupère aussi les créneaux récurrents pour ce jour (Shadows)
        val daySchedules = clubPlanning.filter { 
            it.dayOfWeek == date.dayOfWeek && (teamId == null || it.teamId == teamId) 
        }

        val list = mutableListOf<CalendarListItem>()
        
        // On n'ajoute les templates que s'il n'y a pas déjà une séance réelle pour cette équipe sur ce jour
        daySchedules.forEach { sched ->
            if (sessions.none { it.teamId == sched.teamId }) {
                list.add(CalendarListItem.Training(TrainingSession(
                    id = "template_${date}_${sched.teamId}",
                    teamId = sched.teamId,
                    date = date,
                    startTime = sched.startTime,
                    durationMinutes = sched.durationMinutes,
                    terrain = sched.terrain ?: "Terrain 1",
                    focusArea = null
                )))
            }
        }

        list.addAll(sessions.map { CalendarListItem.Training(it) })
        list.addAll(comps.map { CalendarListItem.Comp(it) })
        list.sortedBy { it.time }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        if (dayEvents.isEmpty()) {
            Text(
                "Aucun événement pour ce jour.", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )
        }
        
        dayEvents.forEach { item ->
            val team = config.teams.find { it.id == item.teamId }
            val customColor = teamColors[item.teamId] ?: team?.color
            when (item) {
                is CalendarListItem.Training -> TrainingSessionCard(
                    session = item.session, 
                    team = team, 
                    accentColor = customColor,
                    allPlayers = allPlayers,
                    persistenceManager = persistenceManager, 
                    seasonConfig = config, 
                    presidentViewModel = presidentViewModel,
                    onNavigateToPreparer = onNavigateToPreparer, 
                    onViewRecap = onViewRecap, 
                    onUpdate = onUpdate
                )
                is CalendarListItem.Comp -> CompetitionEventCard(item.event, team)
            }
        }
    }
}

@Composable
fun TrainingSessionCard(
    session: TrainingSession,
    team: Team?,
    accentColor: Color?,
    allPlayers: List<Player> = emptyList(), // Ajouté
    persistenceManager: PersistenceManager,
    seasonConfig: SeasonConfig,
    presidentViewModel: com.example.coachapp.ui.president.PresidentViewModel,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit,
    onUpdate: () -> Unit
) {
    var showEditSession by remember { mutableStateOf(false) }
    val effectiveColor = accentColor ?: MaterialTheme.colorScheme.primary

    val isPrepared = session.warmup.isNotBlank() || session.drills.isNotBlank() || 
                     session.smallGroupSituations.isNotBlank() || session.collectiveGame.isNotBlank()
    val isEvaluated = session.assessmentId != null
    
    val statusColor = when {
        isEvaluated -> Color(0xFF4CAF50)
        isPrepared -> effectiveColor
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { showEditSession = true }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = statusColor,
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isEvaluated) "TERMINÉE" else if (isPrepared) "PRÊTE" else "À PRÉPARER",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(session.startTime.toString(), style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsVolleyball, null, tint = effectiveColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(team?.name ?: "Entraînement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (session.focusArea != null) {
                        Text(text = session.focusArea, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (!isEvaluated) {
                val preparedPhases = listOf(session.warmup, session.drills, session.smallGroupSituations, session.collectiveGame).count { it.isNotBlank() }
                LinearProgressIndicator(
                    progress = { preparedPhases / 4f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(8.dp))
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val presentCount = session.attendance.values.count { it == "present" }
                val totalCount = session.attendance.size
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("$presentCount / $totalCount présents", fontSize = 11.sp)
                    
                    // Optionnel : Petit bouton de refresh pour forcer la synchro des réponses
                    IconButton(onClick = { onUpdate() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    }
                }
                
                if (!isEvaluated) {
                    Button(
                        onClick = { onNavigateToPreparer(session) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPrepared) MaterialTheme.colorScheme.secondary else effectiveColor
                        )
                    ) {
                        Icon(Icons.Default.EditNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPrepared) "Modifier" else "Préparer", fontSize = 11.sp)
                    }
                } else {
                    TextButton(onClick = { onViewRecap(session) }) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Text("Voir Bilan", fontSize = 11.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }

    if (showEditSession) {
        EditSessionDialog(
            session = session,
            seasonConfig = seasonConfig,
            allPlayers = allPlayers,
            onDismiss = { showEditSession = false },
            presidentViewModel = presidentViewModel,
            onConfirm = { updated: TrainingSession, onResult ->
                var sessionToSave = updated
                
                // Correction UUID : Si c'est un template, on génère un vrai UUID pour Supabase
                if (sessionToSave.id.startsWith("template_")) {
                    sessionToSave = sessionToSave.copy(id = java.util.UUID.randomUUID().toString())
                }

                val config = persistenceManager.loadSeasonConfig()
                // On met à jour ou on ajoute la séance (si l'ID a changé, il faut être vigilant)
                val newList = if (!config.plannedTrainings.any { it.id == sessionToSave.id }) {
                    config.plannedTrainings + sessionToSave
                } else {
                    config.plannedTrainings.map { if (it.id == sessionToSave.id) sessionToSave else it }
                }
                
                val updatedConfig = config.copy(plannedTrainings = newList)
                persistenceManager.saveSeasonConfig(updatedConfig)
                
                // Synchronisation vers Supabase avec l'ID propre
                presidentViewModel.pushSession(
                    session = sessionToSave,
                    onSuccess = { 
                        onResult(true)
                        onUpdate()
                    },
                    onError = { error ->
                        android.util.Log.e("EDIT_SESSION", "Erreur push: $error")
                        onResult(false)
                    }
                )
            }
        )
    }
}

@Composable
fun MonthView(
    currentDate: LocalDate, 
    config: SeasonConfig, 
    teamId: String?, 
    filterType: String,
    teamColors: Map<String, Color>,
    clubPlanning: List<TrainingSchedule> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth())
    val lastDayOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth())
    val daysInMonth = lastDayOfMonth.dayOfMonth
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDateSelected(currentDate.minusMonths(1).withDayOfMonth(1)) }) {
                Icon(Icons.Default.ChevronLeft, null)
            }
            Text(
                text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            IconButton(onClick = { onDateSelected(currentDate.plusMonths(1).withDayOfMonth(1)) }) {
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in 1..7) {
                    val dayNum = week * 7 + day - startOffset
                    if (dayNum in 1..daysInMonth) {
                        val date = firstDayOfMonth.withDayOfMonth(dayNum)
                        val isSelected = date == currentDate
                        
                        // Récupération des événements pour les points de couleur
                        // 1. Les séances réelles (Cloud/Locales)
                        val dayTrainings = config.plannedTrainings.filter { it.date == date && (teamId == null || it.teamId == teamId) }
                        
                        // 2. Les créneaux récurrents (Planning Supabase)
                        val daySchedules = clubPlanning.filter { 
                            it.dayOfWeek == date.dayOfWeek && (teamId == null || it.teamId == teamId) 
                        }
                        
                        val dayComps = config.competitions.filter { it.date == date && (teamId == null || it.teamId == teamId) }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(), 
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.Center) {
                                    // Points pour les séances réelles ou prévues
                                    val teamsWithEvents = (dayTrainings.map { it.teamId } + daySchedules.map { it.teamId }).distinct().take(3)
                                    teamsWithEvents.forEach { tId ->
                                        val color = teamColors[tId] ?: Color.Gray
                                        Box(modifier = Modifier.size(4.dp).padding(horizontal = 0.5.dp).background(color, CircleShape))
                                    }
                                    if (dayComps.isNotEmpty()) {
                                        Box(modifier = Modifier.size(4.dp).padding(horizontal = 0.5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyZoomView(
    date: LocalDate, 
    config: SeasonConfig, 
    colors: Map<String, Color>,
    clubPlanning: List<TrainingSchedule> = emptyList()
) {
    val startOfWeek = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val days = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val hours = (8..22).toList()

    Column(modifier = Modifier.fillMaxWidth().height(800.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Spacer(Modifier.width(50.dp))
            days.forEach { day ->
                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.FRENCH), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(day.dayOfMonth.toString(), fontSize = 12.sp)
                }
            }
        }

        hours.forEach { hour ->
            Row(modifier = Modifier.height(64.dp).fillMaxWidth().drawBehind {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f), 
                    start = Offset(0f, 0f), 
                    end = Offset(size.width, 0f), 
                    strokeWidth = 1.dp.toPx()
                )
            }) {
                Text(
                    text = "${hour}h",
                    modifier = Modifier.width(50.dp).padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                
                days.forEach { day ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.2.dp, Color.LightGray.copy(alpha = 0.1f))) {
                        
                        // 1. Les créneaux récurrents (Shadow slots)
                        val shadows = clubPlanning.filter { it.dayOfWeek == day.dayOfWeek && it.startTime.hour == hour }
                        shadows.forEach { sched ->
                            val color = colors[sched.teamId] ?: Color.Gray
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.2f))
                                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .fillMaxHeight()
                            )
                        }

                        // 2. Les séances réelles (Plein)
                        val sessions = config.plannedTrainings.filter { it.date == day && it.startTime.hour == hour }
                        sessions.forEach { s ->
                            val color = colors[s.teamId] ?: Color.Gray
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.9f))
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = s.focusArea ?: "Entraînement", 
                                    fontSize = 7.sp, 
                                    color = Color.White, 
                                    lineHeight = 8.sp,
                                    maxLines = 3, 
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(teamName: String, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50), 
        Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4),
        Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFFE65100)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Couleur pour $teamName") },
        text = {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(color, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun EditSessionDialog(
    session: TrainingSession,
    seasonConfig: SeasonConfig,
    allPlayers: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (TrainingSession, onResult: (Boolean) -> Unit) -> Unit,
    presidentViewModel: com.example.coachapp.ui.president.PresidentViewModel
) {
    var focus by remember { mutableStateOf(session.focusArea ?: "") }
    var coachNote by remember { mutableStateOf(session.coachNotes ?: "") }
    val focusOptions = listOf("Service / Réception", "Attaque / Bloc", "Défense / Relance", "Systèmes de jeu", "Physique")
    
    // État local des présences : Map<ID_Joueur, Statut>
    val tempAttendance = remember { 
        mutableStateMapOf<String, String>().apply { 
            putAll(session.attendance) 
        } 
    }

    // Synchronisation automatique des réponses joueurs à l'ouverture
    LaunchedEffect(session.id) {
        presidentViewModel.syncPresencesForSession(session.id) { remoteMap ->
            remoteMap.forEach { (vivierId, status) ->
                val localPlayer = allPlayers.find { it.vivierId == vivierId }
                if (localPlayer != null) {
                    tempAttendance[localPlayer.id] = status
                }
            }
        }
    }
    
    val teamPlayers = remember(session.teamId, allPlayers) {
        allPlayers.filter { it.teamId == session.teamId }
    }

    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    // Fermeture automatique après succès
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration Séance") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Thème de la séance :", fontWeight = FontWeight.Bold)
                focusOptions.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { focus = option }) {
                        RadioButton(selected = focus == option, onClick = { focus = option })
                        Text(option)
                    }
                }
                OutlinedTextField(
                    value = focus, 
                    onValueChange = { focus = it }, 
                    label = { Text("Autre thème...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = coachNote,
                    onValueChange = { if (it.length <= 130) coachNote = it },
                    label = { Text("Note aux joueurs (max 130 car.)") },
                    placeholder = { Text("Ex: Prévoir gourde, retard possible...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    supportingText = {
                        Text(
                            text = "${coachNote.length} / 130",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Convocations & Présences :", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (teamPlayers.isEmpty()) {
                    Text("Aucun joueur dans ce collectif.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    teamPlayers.forEach { player ->
                        val currentStatus = tempAttendance[player.id] ?: "pending"
                        
                        PresenceCard(
                            player = player,
                            status = currentStatus,
                            onStatusChange = { newStatus ->
                                tempAttendance[player.id] = newStatus
                            }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    isSaving = true
                    val updatedSession = session.copy(
                        focusArea = focus, 
                        attendance = tempAttendance.toMap(),
                        coachNotes = coachNote,
                        isValidated = true // On valide lors de cette action
                    )
                    onConfirm(updatedSession) { success ->
                        isSaving = false
                        if (success) {
                            saveSuccess = true
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saveSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                ),
                enabled = !isSaving && !saveSuccess
            ) { 
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(if (saveSuccess) Icons.Default.Check else Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (saveSuccess) "Convocation Envoyée !" else "Enregistrer et Convoquer") 
                }
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Annuler") }
        }
    )
}

@Composable
fun PresenceCard(
    player: Player,
    status: String,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${player.firstName} ${player.lastName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(player.position, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            // Sélecteur de statut compact
            StatusToggle(
                status = status,
                onStatusChange = onStatusChange
            )
        }
    }
}

@Composable
fun StatusToggle(
    status: String,
    onStatusChange: (String) -> Unit
) {
    val states = listOf(
        Triple("pending", "Attente", Color.Gray),
        Triple("present", "Présent", Color(0xFF4CAF50)),
        Triple("absent", "Absent", Color(0xFFE91E63)),
        Triple("blesse", "Blessé", Color(0xFFFF9800))
    )
    
    val current = states.find { it.first == status } ?: states[0]
    
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .background(current.third.copy(alpha = 0.1f))
                .border(1.dp, current.third.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = current.second,
                color = current.third,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            states.forEach { (slug, label, color) ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    },
                    onClick = { 
                        onStatusChange(slug)
                        expanded = false 
                    }
                )
            }
        }
    }
}

@Composable
fun CompetitionEventCard(event: CompetitionEvent, team: Team?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, event.type.color)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.opponent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(team?.name ?: "Inconnu", style = MaterialTheme.typography.labelSmall, color = team?.color ?: Color.Gray)
                Text("📍 ${event.location}", style = MaterialTheme.typography.bodySmall)
            }
            Surface(color = event.type.color, shape = RoundedCornerShape(4.dp)) {
                Text(event.type.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEventDialog(
    date: LocalDate, 
    teams: List<Team>, 
    allPlayers: List<Player>,
    initialTeamId: String, 
    teamColors: Map<String, Color>, // Pour les cards
    onDismiss: () -> Unit, 
    onConfirm: (CalendarListItem, onResult: (Boolean) -> Unit) -> Unit
) {
    var type by remember { mutableStateOf("TRAINING") }
    var selectedTeamId by remember { mutableStateOf(initialTeamId) }
    var focus by remember { mutableStateOf("Service / Réception") }
    var opponent by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf("18") }
    var startMinute by remember { mutableStateOf("30") }
    var endHour by remember { mutableStateOf("20") }

    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    // Fermeture automatique après succès
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }

    val playersOfTeam = remember(selectedTeamId, allPlayers) {
        allPlayers.filter { it.teamId == selectedTeamId }
    }
    val selectedPlayerIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(selectedTeamId) {
        selectedPlayerIds.clear()
        playersOfTeam.forEach { selectedPlayerIds[it.id] = true }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state = state) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null, // On enlève le titre statique
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // 1. SWITCH HAUT DE PAGE (Entraînement / Match)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        listOf("TRAINING" to "Entraînement", "COMPETITION" to "Match").forEach { (id, label) ->
                            val isSel = type == id
                            Box(
                                modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { type = id },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // 2. GRILLE DE COLLECTIFS (Cards compactes)
                Text("Équipe :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    teams.forEach { team ->
                        val isSelected = selectedTeamId == team.id
                        val color = teamColors[team.id] ?: team.color
                        Surface(
                            modifier = Modifier.weight(1f, fill = false).height(44.dp).clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTeamId = team.id },
                            color = if (isSelected) color else color.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(team.name, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                // 3. DATE & HEURES
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yy")))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = startHour, onValueChange = { if(it.length <= 2) startHour = it }, modifier = Modifier.width(60.dp), label = { Text("H") }, singleLine = true)
                    Text(":", modifier = Modifier.padding(horizontal = 2.dp))
                    OutlinedTextField(value = startMinute, onValueChange = { if(it.length <= 2) startMinute = it }, modifier = Modifier.width(60.dp), label = { Text("m") }, singleLine = true)
                }

                if (type == "COMPETITION") {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = opponent, onValueChange = { opponent = it }, label = { Text("Adversaire") }, modifier = Modifier.fillMaxWidth())
                }

                // 4. CONVOCATIONS (Pour les deux types)
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CONVOCATIONS (${playersOfTeam.size})", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val allChecked = playersOfTeam.all { selectedPlayerIds[it.id] == true }
                        playersOfTeam.forEach { selectedPlayerIds[it.id] = !allChecked }
                    }) {
                        Text(if(playersOfTeam.all { selectedPlayerIds[it.id] == true }) "Aucun" else "Tous", fontSize = 12.sp)
                    }
                }
                
                playersOfTeam.forEach { player ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                        selectedPlayerIds[player.id] = !(selectedPlayerIds[player.id] ?: false) 
                    }) {
                        Checkbox(checked = selectedPlayerIds[player.id] ?: false, onCheckedChange = { selectedPlayerIds[player.id] = it })
                        Text("${player.firstName} ${player.lastName}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        if (player.position.isNotEmpty()) {
                            Text(player.position, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        isSaving = true
                        val sTime = LocalTime.of(startHour.toInt(), startMinute.toInt())
                        val attendance = selectedPlayerIds.filter { it.value }.mapValues { "pending" }
                        
                        val item = if (type == "TRAINING") {
                            CalendarListItem.Training(TrainingSession(
                                id = java.util.UUID.randomUUID().toString(), 
                                teamId = selectedTeamId, 
                                date = selectedDate, 
                                startTime = sTime, 
                                durationMinutes = 90, 
                                focusArea = focus, 
                                attendance = attendance,
                                isValidated = true // On valide directement lors de l'ajout
                            ))
                        } else {
                            CalendarListItem.Comp(CompetitionEvent(
                                id = java.util.UUID.randomUUID().toString(), 
                                teamId = selectedTeamId, 
                                date = selectedDate, 
                                startTime = sTime, 
                                type = CompetitionType.CHAMPIONSHIP, 
                                opponent = opponent, 
                                location = "Gymnase", 
                                attendance = attendance
                            ))
                        }

                        onConfirm(item) { success ->
                            isSaving = false
                            if (success) {
                                saveSuccess = true
                            }
                        }
                    } catch (_: Exception) {
                        isSaving = false
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saveSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                ),
                enabled = !isSaving && !saveSuccess
            ) { 
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(if (saveSuccess) Icons.Default.Check else Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (saveSuccess) "Convocation Envoyée !" else "Enregistrer et Convoquer") 
                }
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Annuler") }
        }
    )
}

@Composable
fun HelpSessionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedTheme by remember { mutableStateOf("Service / Réception") }
    var generatedSession by remember { mutableStateOf<String?>(null) }
    val themes = listOf("Service / Réception", "Attaque / Bloc", "Défense / Relance", "Systèmes de jeu")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dépannage d'urgence") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (generatedSession == null) {
                    Text("Choisissez un thème pour générer une séance complète :", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    themes.forEach { theme ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedTheme = theme }) {
                            RadioButton(selected = selectedTheme == theme, onClick = { selectedTheme = theme })
                            Text(theme)
                        }
                    }
                } else {
                    Text("SÉANCE GÉNÉRÉE :", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(generatedSession!!, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (generatedSession == null) {
                Button(onClick = {
                    generatedSession = """
                        --- THÈME : $selectedTheme ---
                        1. ÉCHAUFFEMENT (15 min) : Foot-volley + Mobilité articulaire.
                        2. GAMMES (15 min) : Répétitions techniques en binôme (Focus $selectedTheme).
                        3. EXERCICE 1 (20 min) : Travail dirigé en sous-groupes de 3.
                        4. EXERCICE 2 (20 min) : Mise en situation complexe 6x6.
                        5. JEU COLLECTIF (20 min) : Match à thème (Points doublés sur actions $selectedTheme).
                    """.trimIndent()
                }) { Text("Générer") }
            } else {
                Button(onClick = { onConfirm(selectedTheme) }) { Text("OK, j'utilise cette séance") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
