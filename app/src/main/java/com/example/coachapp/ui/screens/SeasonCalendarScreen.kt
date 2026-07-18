package com.example.coachapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.window.Dialog
import com.example.coachapp.data.*
import com.example.coachapp.ui.training.TrainingViewModel
import java.time.LocalDate
import java.util.UUID
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlinx.coroutines.delay

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
    data class Club(val event: ClubEvent) : CalendarListItem() {
        override val time = event.startTime
        override val teamId = "CLUB"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonCalendarScreen(
    modifier: Modifier = Modifier,
    persistenceManager: PersistenceManager,
    seasonConfig: SeasonConfig,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    trainingViewModel: TrainingViewModel,
    onUseHelp: () -> Unit,
    helpUsageCount: Int,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit,
    onUpdateAttendance: (String, String) -> Unit = { _, _ -> }
) {
    var config by remember { mutableStateOf(seasonConfig) }
    
    LaunchedEffect(seasonConfig) {
        config = seasonConfig
    }

    val coachTeams = seasonConfig.teams

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentView by remember { mutableStateOf("PROGRAM") }
    var isWeeklyMode by remember { mutableStateOf(false) }
    
    var activeFilterType by remember { mutableStateOf("MY_TEAMS") }
    var selectedTeamId by remember { mutableStateOf<String?>(null) }

    var coachTeamColors by remember { 
        val saved = persistenceManager.loadTeamColors()
        mutableStateOf(saved.mapValues { Color(it.value) }.toMutableMap())
    }

    val allCoachPlayers = seasonConfig.players

    var showAddEvent by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showColorPickerForTeamId by remember { mutableStateOf<String?>(null) }
    var selectedEventForAttendance by remember { mutableStateOf<ClubEvent?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF001529))
    ) {
        // Ambient Glow
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = 100.dp)
                .size(300.dp)
                .background(Color(0xFFE91E63).copy(alpha = 0.12f), CircleShape)
                .blur(90.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(350.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape)
                .blur(100.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // --- HEADER ---
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    text = "2026-2027",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White,
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
                        tint = if (helpUsageCount < 3) Color.Red.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }

            // --- BOUTON AJOUTER ---
            val isDataReady = coachTeams.isNotEmpty()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = isDataReady) { showAddEvent = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isDataReady) 0.2f else 0.05f)),
                color = Color.White.copy(alpha = if (isDataReady) 0.1f else 0.05f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(if (isDataReady) 1f else 0.5f)
                ) {
                    Icon(Icons.Default.AddCircle, null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isDataReady) "Ajouter un évènement" else "Chargement des équipes...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // --- FILTRES ---
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlassFilterChip(
                    selected = activeFilterType == "CLUB",
                    onClick = { activeFilterType = "CLUB"; selectedTeamId = null },
                    label = "Club"
                )
                
                GlassFilterChip(
                    selected = (activeFilterType == "MY_TEAMS" && selectedTeamId == null),
                    onClick = { activeFilterType = "MY_TEAMS"; selectedTeamId = null },
                    label = "Mes Équipes"
                )
                
                coachTeams.forEach { team ->
                    val color = coachTeamColors[team.id] ?: team.color
                    GlassFilterChip(
                        selected = selectedTeamId == team.id,
                        onClick = { 
                            if (selectedTeamId == team.id) showColorPickerForTeamId = team.id
                            else { selectedTeamId = team.id; activeFilterType = "SPECIFIC" }
                        },
                        label = team.name,
                        leadingColor = color
                    )
                }
            }

            // --- SWITCHER ---
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val views = listOf("PROGRAM" to "Programme", "MONTH" to "Calendrier", "PLANNING" to "Planification")
                    views.forEach { (id, label) ->
                        val isSelected = currentView == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { currentView = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (currentView == "MONTH") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Vue ${if(isWeeklyMode) "Hebdomadaire" else "Mensuelle"}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
                    TextButton(onClick = { isWeeklyMode = !isWeeklyMode }) {
                        Text(if(isWeeklyMode) "Voir le mois" else "Zoom semaine", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- CONTENU ---
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    "MONTH" -> {
                        val pPlanning by trainingViewModel.clubPlanning.collectAsState()
                        if (isWeeklyMode) {
                            WeeklyZoomView(
                                date = selectedDate, 
                                config = config, 
                                colors = coachTeamColors,
                                clubPlanning = pPlanning
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    MonthView(
                                        currentDate = selectedDate,
                                        config = config,
                                        teamId = selectedTeamId,
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
                                        teamId = selectedTeamId,
                                        teamColors = coachTeamColors,
                                        allPlayers = allCoachPlayers,
                                        clubPlanning = pPlanning,
                                        persistenceManager = persistenceManager, 
                                        trainingViewModel = trainingViewModel,
                                        onNavigateToPreparer = onNavigateToPreparer,
                                        onViewRecap = onViewRecap,
                                        onUpdateAttendance = { id, status -> onUpdateAttendance(id, status) },
                                        onEventClick = { selectedEventForAttendance = it },
                                        onUpdate = { config = persistenceManager.loadSeasonConfig() }
                                    )
                                }
                                
                                item { Spacer(Modifier.height(100.dp)) }
                            }
                        }
                    }
                    "PLANNING" -> {
                        SeasonPlannerView(
                            config = config, 
                            selectedTeamId = selectedTeamId, 
                            cycles = viewModel.cycles,
                            onCyclesUpdated = { updated ->
                                updated.forEach { cycle ->
                                    if (viewModel.cycles.any { it.id == cycle.id }) viewModel.modifierCycle(cycle)
                                    else viewModel.ajouterCycle(cycle)
                                }
                            }
                        )
                    }
                    else -> {
                        val pPlanning by trainingViewModel.clubPlanning.collectAsState()
                        ProgramView(
                            config = config, 
                            filterType = activeFilterType, 
                            teamId = selectedTeamId, 
                            teamColors = coachTeamColors, 
                            allPlayers = allCoachPlayers, 
                            clubPlanning = pPlanning,
                            persistenceManager = persistenceManager, 
                            trainingViewModel = trainingViewModel,
                            onNavigateToPreparer = onNavigateToPreparer, 
                            onViewRecap = onViewRecap,
                            onUpdateAttendance = { id, status -> onUpdateAttendance(id, status) },
                            onEventClick = { selectedEventForAttendance = it },
                            onUpdate = { config = persistenceManager.loadSeasonConfig() }
                        )
                    }
                }
            }
        }

        if (selectedEventForAttendance != null) {
            ClubEventAttendanceSheet(
                event = selectedEventForAttendance!!,
                currentStatus = config.clubEventRegistrations[selectedEventForAttendance!!.id] ?: "pending",
                onStatusSelected = { status ->
                    onUpdateAttendance(selectedEventForAttendance!!.id, status)
                    selectedEventForAttendance = null
                },
                onDismiss = { selectedEventForAttendance = null }
            )
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
                        trainingViewModel.pushSession(session = session, onSuccess = { onResult(true) }, onError = { onResult(false) })
                    }
                    is CalendarListItem.Comp -> {
                        val match = event.event
                        config = config.copy(competitions = config.competitions + match)
                        persistenceManager.saveSeasonConfig(config)
                        trainingViewModel.pushMatch(event = match, onSuccess = { onResult(true) }, onError = { onResult(false) })
                    }
                    is CalendarListItem.Club -> {
                        // Club events added via Hub, but we handle the type safety here
                        onResult(true)
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
                if (coachTeams.isNotEmpty()) {
                    val targetTeam = selectedTeamId ?: coachTeams.first().id
                    val date = LocalDate.now()
                    val time = LocalTime.of(18, 30)
                    val newSession = TrainingSession(
                        id = TrainingSession.generateDeterministicId(targetTeam, date, time),
                        teamId = targetTeam,
                        date = date,
                        startTime = time,
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
                val newColors = coachTeamColors.toMutableMap().apply { put(showColorPickerForTeamId!!, color) }
                coachTeamColors = newColors
                persistenceManager.saveTeamColors(newColors.mapValues { it.value.toArgb() })
                showColorPickerForTeamId = null
            },
            onDismiss = { showColorPickerForTeamId = null }
        )
    }
}

@Composable
fun GlassFilterChip(selected: Boolean, onClick: () -> Unit, label: String, leadingColor: Color? = null) {
    Surface(
        modifier = Modifier.height(32.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (selected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leadingColor != null) {
                Box(modifier = Modifier.size(8.dp).background(leadingColor, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Color.White else Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ProgramView(
    config: SeasonConfig,
    filterType: String,
    teamId: String?,
    teamColors: Map<String, Color>,
    allPlayers: List<Player>,
    clubPlanning: List<TrainingSchedule> = emptyList(),
    persistenceManager: PersistenceManager,
    trainingViewModel: TrainingViewModel,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit,
    onUpdateAttendance: (String, String) -> Unit,
    onEventClick: (ClubEvent) -> Unit,
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
            val hasClubEvent = config.clubEvents.any { it.date == date && (teamId == null || it.targetTeamIds.isEmpty() || it.targetTeamIds.contains(teamId)) }
            if (isTrainingDay || hasComp || hasSession || hasClubEvent) list.add(date)
        }
        list
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeDays, key = { it.toString() }) { date ->
            val isComp = config.competitions.any { (teamId == null || it.teamId == teamId) && it.date == date }
            val hasClubEvent = config.clubEvents.any { it.date == date }
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                if (date.dayOfWeek == java.time.DayOfWeek.MONDAY || date == activeDays.firstOrNull()) {
                    Text(text = "SEMAINE DU ${date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH))}".uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B4D8), fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when {
                            isComp -> Color(0xFF673AB7).copy(alpha = 0.2f)
                            hasClubEvent -> Color(0xFF00B4D8).copy(alpha = 0.2f)
                            else -> Color.White.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(52.dp),
                        border = BorderStroke(0.5.dp, when {
                            isComp -> Color(0xFF673AB7).copy(alpha = 0.4f)
                            hasClubEvent -> Color(0xFF00B4D8).copy(alpha = 0.4f)
                            else -> Color.White.copy(alpha = 0.15f)
                        })
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.FRENCH), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isComp || hasClubEvent) Color.White else Color.White.copy(alpha = 0.6f))
                            Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        DailyEventsList(date, config, teamId, teamColors, allPlayers, clubPlanning, persistenceManager, trainingViewModel, onNavigateToPreparer, onViewRecap, onUpdateAttendance, onEventClick, onUpdate)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color.White.copy(alpha = 0.05f))
            }
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun DailyEventsList(
    date: LocalDate, 
    config: SeasonConfig, 
    teamId: String?, 
    teamColors: Map<String, Color>, 
    allPlayers: List<Player>, 
    clubPlanning: List<TrainingSchedule>, 
    persistenceManager: PersistenceManager, 
    trainingViewModel: TrainingViewModel, 
    onNavigateToPreparer: (TrainingSession) -> Unit, 
    onViewRecap: (TrainingSession) -> Unit, 
    onUpdateAttendance: (String, String) -> Unit,
    onEventClick: (ClubEvent) -> Unit,
    onUpdate: () -> Unit
) {
    val dayEvents = remember(date, config, teamId, clubPlanning) {
        val sessions = config.plannedTrainings.filter { it.date == date && (teamId == null || it.teamId == teamId) }
        val comps = config.competitions.filter { it.date == date && (teamId == null || it.teamId == teamId) }
        val clubs = config.clubEvents.filter { it.date == date && (teamId == null || it.targetTeamIds.isEmpty() || it.targetTeamIds.contains(teamId)) }
        val daySchedules = clubPlanning.filter { it.dayOfWeek == date.dayOfWeek && (teamId == null || it.teamId == teamId) }
        val list = mutableListOf<CalendarListItem>()
        daySchedules.forEach { sched -> if (sessions.none { it.teamId == sched.teamId }) { list.add(CalendarListItem.Training(TrainingSession(id = TrainingSession.generateDeterministicId(sched.teamId, date, sched.startTime), teamId = sched.teamId, date = date, startTime = sched.startTime, durationMinutes = sched.durationMinutes, terrain = sched.terrain ?: "Terrain 1", focusArea = null))) } }
        list.addAll(sessions.map { CalendarListItem.Training(it) })
        list.addAll(comps.map { CalendarListItem.Comp(it) })
        list.addAll(clubs.map { CalendarListItem.Club(it) })
        list.sortedBy { it.time }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        dayEvents.forEach { item ->
            val team = config.teams.find { it.id == item.teamId }
            val customColor = teamColors[item.teamId] ?: team?.color
            when (item) {
                is CalendarListItem.Training -> TrainingSessionCard(item.session, team, customColor, allPlayers, persistenceManager, trainingViewModel, onNavigateToPreparer, onViewRecap, onUpdate)
                is CalendarListItem.Comp -> CompetitionEventCard(item.event, team, allPlayers, trainingViewModel, onUpdate)
                is CalendarListItem.Club -> {
                    val status = config.clubEventRegistrations[item.event.id] ?: "pending"
                    ClubEventCalendarCard(item.event, status, onEventClick)
                }
            }
        }
    }
}

@Composable
fun ClubEventCalendarCard(event: ClubEvent, status: String, onClick: (ClubEvent) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(event) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00B4D8).copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFF00B4D8))
                    Spacer(Modifier.width(6.dp))
                    Text(event.type.name, style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.width(12.dp))
                    
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
                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(event.location, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            }
            Text(event.startTime.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
fun TrainingSessionCard(session: TrainingSession, team: Team?, accentColor: Color?, allPlayers: List<Player>, persistenceManager: PersistenceManager, trainingViewModel: TrainingViewModel, onNavigateToPreparer: (TrainingSession) -> Unit, onViewRecap: (TrainingSession) -> Unit, onUpdate: () -> Unit) {
    var showEditSession by remember { mutableStateOf(false) }
    val effectiveColor = accentColor ?: Color(0xFF00B4D8)
    val preparedPhases = listOf(session.warmup, session.drills, session.smallGroupSituations, session.collectiveGame).count { it.isNotBlank() }
    val isPrepared = preparedPhases > 0
    val isFullyPrepared = preparedPhases == 4
    val isEvaluated = session.assessmentId != null
    val statusColor = when { isEvaluated -> Color(0xFF4CAF50); isPrepared -> effectiveColor; else -> Color.White.copy(alpha = 0.4f) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { showEditSession = true }, 
        shape = RoundedCornerShape(18.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)), 
        border = BorderStroke(
            width = if (isFullyPrepared) 1.dp else 0.5.dp, 
            color = if (isFullyPrepared) effectiveColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = statusColor, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
                Spacer(Modifier.width(10.dp))
                Text(text = if (isEvaluated) "TERMINÉE" else if (isPrepared) "PRÊTE" else "À PRÉPARER", style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                Spacer(Modifier.weight(1f))
                Text(session.startTime.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = effectiveColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp), border = BorderStroke(0.5.dp, effectiveColor.copy(alpha = 0.3f))) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SportsVolleyball, null, tint = effectiveColor, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(team?.name ?: "Entraînement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    if (session.focusArea != null) { Text(text = session.focusArea, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!isEvaluated) {
                LinearProgressIndicator(progress = { preparedPhases / 4f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = statusColor, trackColor = Color.White.copy(alpha = 0.1f), strokeCap = StrokeCap.Round)
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val presentCount = session.attendance.values.count { it == "present" }
                val totalCount = session.attendance.size
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.width(6.dp))
                    Text("$presentCount / $totalCount présents", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
                if (!isEvaluated) {
                    Button(onClick = { onNavigateToPreparer(session) }, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(34.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isPrepared) Color.White.copy(alpha = 0.1f) else effectiveColor, contentColor = if (isPrepared) Color.White else Color.Black), border = if (isPrepared) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)) else null) {
                        Icon(Icons.Default.EditNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPrepared) "Modifier" else "Préparer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { onViewRecap(session) }) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Voir Bilan", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showEditSession) {
        EditSessionDialog(session, allPlayers, { showEditSession = false }, { updated, onResult ->
            val configLocal = persistenceManager.loadSeasonConfig()
            val newList = if (!configLocal.plannedTrainings.any { it.id == updated.id }) configLocal.plannedTrainings + updated else configLocal.plannedTrainings.map { if (it.id == updated.id) updated else it }
            persistenceManager.saveSeasonConfig(configLocal.copy(plannedTrainings = newList))
            trainingViewModel.pushSession(
                session = updated, 
                onSuccess = { 
                    onResult(null)
                    onUpdate() 
                }, 
                onError = { error ->
                    onResult(error)
                }
            )
        }, trainingViewModel)
    }
}

@Composable
fun MonthView(currentDate: LocalDate, config: SeasonConfig, teamId: String?, teamColors: Map<String, Color>, clubPlanning: List<TrainingSchedule> = emptyList(), onDateSelected: (LocalDate) -> Unit) {
    val firstDayOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth())
    val lastDayOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth())
    val daysInMonth = lastDayOfMonth.dayOfMonth
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7

    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)).border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onDateSelected(currentDate.minusMonths(1).withDayOfMonth(1)) }) { Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text(text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
            IconButton(onClick = { onDateSelected(currentDate.plusMonths(1).withDayOfMonth(1)) }) { Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { listOf("L", "M", "M", "J", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) } }
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in 1..7) {
                    val dayNum = week * 7 + day - startOffset
                    if (dayNum in 1..daysInMonth) {
                        val date = firstDayOfMonth.withDayOfMonth(dayNum)
                        val isSelected = date == currentDate
                        val dayTrainings = config.plannedTrainings.filter { it.date == date && (teamId == null || it.teamId == teamId) }
                        val daySchedules = clubPlanning.filter { it.dayOfWeek == date.dayOfWeek && (teamId == null || it.teamId == teamId) }
                        val dayComps = config.competitions.filter { it.date == date && (teamId == null || it.teamId == teamId) }
                        val dayClubEvents = config.clubEvents.filter { it.date == date && (teamId == null || it.targetTeamIds.isEmpty() || it.targetTeamIds.contains(teamId)) }
                        
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color(0xFF00B4D8).copy(alpha = 0.3f) else Color.Transparent).then(if (isSelected) Modifier.border(1.5.dp, Color(0xFF00B4D8), RoundedCornerShape(10.dp)) else Modifier).clickable { onDateSelected(date) }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = dayNum.toString(), fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
                                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 2.dp)) {
                                    val teamsWithEvents = (dayTrainings.map { it.teamId } + daySchedules.map { it.teamId }).distinct().take(3)
                                    teamsWithEvents.forEach { tId -> Box(modifier = Modifier.size(5.dp).padding(horizontal = 0.5.dp).background(teamColors[tId] ?: Color.Gray, CircleShape)) }
                                    if (dayComps.isNotEmpty()) Box(modifier = Modifier.size(5.dp).padding(horizontal = 0.5.dp).background(Color(0xFF673AB7), CircleShape))
                                    if (dayClubEvents.isNotEmpty()) Box(modifier = Modifier.size(5.dp).padding(horizontal = 0.5.dp).background(Color(0xFF00B4D8), CircleShape))
                                }
                            }
                        }
                    } else Spacer(modifier = Modifier.weight(1f))
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
    val hourHeight = 64.dp
    val timeColumnWidth = 50.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // --- DAYS HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Spacer(Modifier.width(timeColumnWidth))
            days.forEach { day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.FRENCH),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // --- CALENDAR GRID & EVENTS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(hourHeight * hours.size)
        ) {
            // 1. Background Grid (Hour lines)
            Column {
                hours.forEach { hour ->
                    Row(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    ) {
                        Text(
                            text = "${hour}h",
                            modifier = Modifier
                                .width(timeColumnWidth)
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                        // Empty cells for the grid
                        repeat(7) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.2.dp, Color.White.copy(alpha = 0.05f))
                            )
                        }
                    }
                }
            }

            // 2. Events Overlay
            Row(
                modifier = Modifier
                    .padding(start = timeColumnWidth)
                    .fillMaxSize()
            ) {
                days.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Gather all events for this specific day
                        val daySessions = config.plannedTrainings.filter { it.date == day }
                        val dayComps = config.competitions.filter { it.date == day }
                        val dayClubEvents = config.clubEvents.filter { it.date == day }
                        val dayShadows = clubPlanning.filter { it.dayOfWeek == day.dayOfWeek }

                        // Draw Shadows (Planned schedules without sessions yet)
                        dayShadows.forEach { sched ->
                            if (daySessions.none { it.teamId == sched.teamId && it.startTime == sched.startTime }) {
                                WeeklyEventBlock(
                                    title = "Prévu",
                                    startTime = sched.startTime,
                                    durationMinutes = sched.durationMinutes,
                                    color = (colors[sched.teamId] ?: Color.Gray).copy(alpha = 0.15f),
                                    borderColor = (colors[sched.teamId] ?: Color.Gray).copy(alpha = 0.3f),
                                    hourHeight = hourHeight,
                                    startHour = hours.first(),
                                    isShadow = true
                                )
                            }
                        }

                        // Draw Real Sessions
                        daySessions.forEach { session ->
                            WeeklyEventBlock(
                                title = session.focusArea ?: "Entraînement",
                                startTime = session.startTime,
                                durationMinutes = session.durationMinutes,
                                color = (colors[session.teamId] ?: Color.Gray).copy(alpha = 0.85f),
                                hourHeight = hourHeight,
                                startHour = hours.first()
                            )
                        }

                        // Draw Competitions
                        dayComps.forEach { comp ->
                            WeeklyEventBlock(
                                title = "MATCH: ${comp.opponent}",
                                startTime = comp.startTime,
                                durationMinutes = 120, // Default match duration
                                color = Color(0xFF673AB7).copy(alpha = 0.85f),
                                hourHeight = hourHeight,
                                startHour = hours.first()
                            )
                        }

                        // Draw Club Events
                        dayClubEvents.forEach { event ->
                            WeeklyEventBlock(
                                title = event.title,
                                startTime = event.startTime,
                                durationMinutes = 90, // Default club event duration
                                color = Color(0xFF00B4D8).copy(alpha = 0.85f),
                                hourHeight = hourHeight,
                                startHour = hours.first(),
                                isClub = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyEventBlock(
    title: String,
    startTime: LocalTime,
    durationMinutes: Int,
    color: Color,
    borderColor: Color = Color.Transparent,
    hourHeight: androidx.compose.ui.unit.Dp,
    startHour: Int,
    isShadow: Boolean = false,
    isClub: Boolean = false
) {
    val minutesFromStart = (startTime.hour - startHour) * 60 + startTime.minute
    val topOffset = (minutesFromStart.toFloat() / 60f) * hourHeight.value
    val blockHeight = (durationMinutes.toFloat() / 60f) * hourHeight.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = topOffset.dp)
            .height(blockHeight.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(6.dp))
                else Modifier
            )
            .padding(4.dp)
    ) {
        Text(
            text = title,
            fontSize = if (blockHeight < 30) 7.sp else 8.sp,
            color = if (isShadow) Color.White.copy(alpha = 0.6f) else Color.White,
            lineHeight = 9.sp,
            maxLines = if (blockHeight < 40) 1 else 3,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isClub) FontWeight.Black else FontWeight.Bold
        )
    }
}

@Composable
fun ColorPickerDialog(teamName: String, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFFE65100))
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF002147).copy(alpha = 0.95f), title = { Text("Couleur pour $teamName", color = Color.White, fontWeight = FontWeight.Black) }, text = { @OptIn(ExperimentalLayoutApi::class) FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { options.forEach { color -> Box(modifier = Modifier.size(44.dp).background(color, CircleShape).border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape).clickable { onColorSelected(color) }) } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer", color = Color.White) } })
}

@Composable
fun EditSessionDialog(
    session: TrainingSession, 
    allPlayers: List<Player>, 
    onDismiss: () -> Unit, 
    onConfirm: (TrainingSession, (String?) -> Unit) -> Unit, 
    trainingViewModel: TrainingViewModel
) {
    var focus by remember { mutableStateOf(session.focusArea ?: "") }
    var coachNote by remember { mutableStateOf(session.coachNotes ?: "") }
    val tempAttendance = remember { mutableStateMapOf<String, String>().apply { putAll(session.attendance) } }
    
    LaunchedEffect(session.id) { 
        trainingViewModel.syncPresencesForSession(session.id) { remoteMap -> 
            remoteMap.forEach { (vid, status) -> 
                allPlayers.find { it.vivierId == vid }?.let { tempAttendance[it.id] = status } 
            } 
        } 
    }
    
    val teamPlayers = remember(session.teamId, allPlayers) { allPlayers.filter { it.teamId == session.teamId } }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveSuccess) { 
        if (saveSuccess) { 
            delay(1500)
            onDismiss() 
        } 
    }

    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = Color(0xFF002147).copy(alpha = 0.95f), 
        title = { Text("Configuration Séance", color = Color.White, fontWeight = FontWeight.Black) }, 
        text = { 
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) { 
                if (errorMessage != null) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Text("Thème :", fontWeight = FontWeight.Bold, color = Color.White)
                listOf("Service / Réception", "Attaque / Bloc", "Défense / Relance", "Systèmes de jeu", "Physique").forEach { opt -> 
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { focus = opt }) { 
                        RadioButton(selected = focus == opt, onClick = { focus = opt }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00B4D8), unselectedColor = Color.White.copy(alpha = 0.6f)))
                        Text(opt, color = Color.White) 
                    } 
                }
                
                OutlinedTextField(
                    value = focus, 
                    onValueChange = { focus = it }, 
                    label = { Text("Autre thème...", color = Color.White.copy(alpha = 0.6f)) }, 
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                OutlinedTextField(
                    value = coachNote, 
                    onValueChange = { if (it.length <= 130) coachNote = it }, 
                    label = { Text("Note aux joueurs (max 130 car.)", color = Color.White.copy(alpha = 0.6f)) }, 
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), 
                    supportingText = { Text("${coachNote.length} / 130", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f)) }
                )
                
                Spacer(Modifier.height(24.dp))
                Text("Convocations & Présences :", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                if (teamPlayers.isEmpty()) { 
                    Text("Aucun joueur.", color = Color.White.copy(alpha = 0.5f)) 
                } else { 
                    teamPlayers.forEach { p -> PresenceCard(p, tempAttendance[p.id] ?: "pending") { tempAttendance[p.id] = it } } 
                } 
            } 
        }, 
        confirmButton = { 
            Button(
                onClick = { 
                    isSaving = true
                    errorMessage = null
                    val updated = session.copy(focusArea = focus, attendance = tempAttendance.toMap(), coachNotes = coachNote, isValidated = true)
                    onConfirm(updated) { error -> 
                        isSaving = false
                        if (error == null) {
                            saveSuccess = true
                        } else {
                            errorMessage = error
                        }
                    } 
                }, 
                shape = RoundedCornerShape(10.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = if (saveSuccess) Color(0xFF4CAF50) else Color.White, contentColor = Color.Black), 
                enabled = !isSaving && !saveSuccess
            ) { 
                if (isSaving) { 
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp) 
                } else { 
                    Icon(if (saveSuccess) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (saveSuccess) "Envoyé !" else "Enregistrer & Convoquer") 
                } 
            } 
        }, 
        dismissButton = { 
            TextButton(onClick = onDismiss, enabled = !isSaving) { 
                Text("Annuler", color = Color.White.copy(alpha = 0.6f)) 
            } 
        }
    )
}

@Composable
fun PresenceCard(player: Player, status: String, onStatusChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text("${player.firstName} ${player.lastName}", color = Color.White, fontWeight = FontWeight.Bold); Text(player.position, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f)) }
            StatusToggle(status, onStatusChange)
        }
    }
}

@Composable
fun StatusToggle(status: String, onStatusChange: (String) -> Unit, enabled: Boolean = true) {
    val states = listOf(Triple("pending", "Attente", Color.Gray), Triple("present", "Présent", Color(0xFF4CAF50)), Triple("absent", "Absent", Color(0xFFE91E63)), Triple("blesse", "Blessé", Color(0xFFFF9800)))
    val current = states.find { it.first == status } ?: states[0]
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { expanded = true }
                .background(current.third.copy(alpha = 0.15f))
                .border(1.dp, current.third.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) { 
            Text(text = current.second, color = current.third, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) 
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
                    onClick = { onStatusChange(slug); expanded = false }
                ) 
            } 
        }
    }
}

@Composable
fun CompetitionEventCard(event: CompetitionEvent, team: Team?, allPlayers: List<Player>, trainingViewModel: TrainingViewModel, onUpdate: () -> Unit) {
    var showMatchSheet by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { showMatchSheet = true }, 
        shape = RoundedCornerShape(18.dp), 
        colors = CardDefaults.cardColors(containerColor = event.type.color.copy(alpha = 0.15f)), 
        border = BorderStroke(1.dp, event.type.color.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(event.opponent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White); Text(team?.name ?: "Inconnu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = event.type.color); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.6f)); Spacer(Modifier.width(4.dp)); Text(event.location, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f)) } }
            Surface(color = event.type.color, shape = RoundedCornerShape(8.dp), shadowElevation = 4.dp) { Text(event.type.label.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
        }
    }
    
    if (showMatchSheet) {
        MatchSheetDialog(
            event = event,
            allPlayers = allPlayers,
            onDismiss = { showMatchSheet = false },
            onConfirm = { updated, callback ->
                trainingViewModel.pushMatch(updated, onSuccess = { 
                    onUpdate()
                    callback(true) 
                }, onError = { callback(false) })
            }
        )
    }
}

@Composable
fun MatchSheetDialog(event: CompetitionEvent, allPlayers: List<Player>, onDismiss: () -> Unit, onConfirm: (CompetitionEvent, (Boolean) -> Unit) -> Unit) {
    var coachNote by remember { mutableStateOf(event.coachNotes) }
    val tempAttendance = remember { mutableStateMapOf<String, String>().apply { putAll(event.attendance) } }
    val tempCarpooling = remember { mutableStateMapOf<String, Int>().apply { putAll(event.carpooling) } }
    
    val teamPlayers = remember(event.teamId, allPlayers) { allPlayers.filter { it.teamId == event.teamId } }
    
    // Règle des 15 jours
    val isConvocable = remember(event.date) {
        val deadline = LocalDate.now().plusDays(15)
        !event.date.isAfter(deadline)
    }

    // Stats de covoiturage
    val playersConvoques = tempAttendance.values.count { it == "present" }
    val totalPlaces = (tempCarpooling["coach"] ?: 0) + teamPlayers.filter { tempAttendance[it.id] == "present" }.sumOf { tempCarpooling[it.id] ?: 0 }
    
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(saveSuccess) { if (saveSuccess) { delay(1500); onDismiss() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF002147).copy(alpha = 0.95f),
        title = {
            Column {
                Text(event.opponent, color = Color.White, fontWeight = FontWeight.Black)
                Text(event.location, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // LOGISTIQUE COVOITURAGE
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("LOGISTIQUE COVOITURAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF00B4D8))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Places disponibles :", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$totalPlaces / $playersConvoques joueurs", 
                                color = if (totalPlaces >= playersConvoques) Color(0xFF4CAF50) else Color(0xFFE91E63),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (playersConvoques > 0) (totalPlaces.toFloat() / playersConvoques).coerceAtMost(1f) else 0f },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (totalPlaces >= playersConvoques) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = coachNote,
                    onValueChange = { coachNote = it },
                    label = { Text("Notes (RDV, parking, etc.)", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(Modifier.height(24.dp))
                Text("COACH", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Ma voiture (places dispo) :", color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    CarpoolingSelector(
                        count = tempCarpooling["coach"] ?: 0,
                        onCountChange = { tempCarpooling["coach"] = it }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CONVOCATIONS & VOITURES", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    if (!isConvocable) {
                        Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("Ouverture le ${event.date.minusDays(15).format(DateTimeFormatter.ofPattern("dd/MM"))}", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                
                if (teamPlayers.isEmpty()) {
                    Text("Aucun joueur.", color = Color.White.copy(alpha = 0.5f))
                } else {
                    teamPlayers.forEach { p ->
                        MatchPresenceCard(
                            player = p,
                            status = tempAttendance[p.id] ?: "pending",
                            carCapacity = tempCarpooling[p.id] ?: 0,
                            onStatusChange = { tempAttendance[p.id] = it },
                            enabled = isConvocable
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    val updated = event.copy(
                        attendance = tempAttendance.toMap(),
                        carpooling = tempCarpooling.toMap(),
                        coachNotes = coachNote
                    )
                    onConfirm(updated) { success ->
                        if (success) saveSuccess = true
                        isSaving = false
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (saveSuccess) Color(0xFF4CAF50) else Color.White, contentColor = Color.Black),
                enabled = !isSaving && !saveSuccess && (isConvocable || coachNote != event.coachNotes)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(if (saveSuccess) Icons.Default.Check else if (isConvocable) Icons.AutoMirrored.Filled.Send else Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (saveSuccess) "Envoyé !" else if (isConvocable) "Enregistrer & Convoquer" else "Enregistrer Notes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Annuler", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun MatchPresenceCard(player: Player, status: String, carCapacity: Int, onStatusChange: (String) -> Unit, enabled: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${player.firstName} ${player.lastName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(player.position, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
                StatusToggle(status, onStatusChange, enabled = enabled)
            }
            if (status == "present") {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Places voiture :", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
                    
                    if (carCapacity > 0) {
                        Surface(
                            color = Color(0xFF00B4D8).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$carCapacity places",
                                color = Color(0xFF00B4D8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text("Pas de voiture", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun CarpoolingSelector(count: Int, onCountChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (count > 0) onCountChange(count - 1) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Remove, null, tint = if (count > 0) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
        }
        Text(
            text = "$count",
            color = if (count > 0) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(
            onClick = { if (count < 8) onCountChange(count + 1) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
    teamColors: Map<String, Color>, 
    onDismiss: () -> Unit, 
    onConfirm: (CalendarListItem, (Boolean) -> Unit) -> Unit
) {
    var type by remember { mutableStateOf("TRAINING") }
    var selectedTeamId by remember { mutableStateOf(initialTeamId) }
    var focus by remember { mutableStateOf("Service / Réception") }
    var opponent by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf("18") }
    var startMinute by remember { mutableStateOf("30") }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    
    val playersOfTeam = remember(selectedTeamId, allPlayers) { allPlayers.filter { it.teamId == selectedTeamId } }
    val selectedPlayerIds = remember { mutableStateMapOf<String, Boolean>() }
    
    // Règle des 15 jours
    val isConvocable = remember(selectedDate) {
        val deadline = LocalDate.now().plusDays(15)
        !selectedDate.isAfter(deadline)
    }

    LaunchedEffect(saveSuccess) { if (saveSuccess) { delay(1500); onDismiss() } }
    LaunchedEffect(selectedTeamId) { 
        selectedPlayerIds.clear()
        playersOfTeam.forEach { selectedPlayerIds[it.id] = true } 
    }
    
    if (showDatePicker) { 
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false }, 
            confirmButton = { 
                TextButton(onClick = { 
                    state.selectedDateMillis?.let { 
                        selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() 
                    }
                    showDatePicker = false 
                }) { Text("OK") } 
            }
        ) { DatePicker(state = state) } 
    }

    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = Color(0xFF002147).copy(alpha = 0.95f), 
        title = null, 
        text = { 
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) { 
                Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) { 
                    Row(modifier = Modifier.padding(4.dp)) { 
                        listOf("TRAINING" to "Entraînement", "COMPETITION" to "Match").forEach { (id, label) -> 
                            val isSel = type == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF00B4D8) else Color.Transparent)
                                    .clickable { type = id }, 
                                contentAlignment = Alignment.Center
                            ) { 
                                Text(label, color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) 
                            } 
                        } 
                    } 
                }
                
                Text("Équipe :", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                    teams.forEach { team -> 
                        val isSelected = selectedTeamId == team.id
                        val color = teamColors[team.id] ?: team.color
                        Surface(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTeamId = team.id }, 
                            color = if (isSelected) color else color.copy(alpha = 0.15f), 
                            border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
                        ) { 
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) { 
                                Text(team.name, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 12.sp) 
                            } 
                        } 
                    } 
                }
                
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { 
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yy"))) 
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = startHour, onValueChange = { if(it.length <= 2) startHour = it }, modifier = Modifier.width(60.dp), label = { Text("H", color = Color.White.copy(alpha = 0.6f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Text(":", color = Color.White, modifier = Modifier.padding(horizontal = 2.dp))
                    OutlinedTextField(value = startMinute, onValueChange = { if(it.length <= 2) startMinute = it }, modifier = Modifier.width(60.dp), label = { Text("m", color = Color.White.copy(alpha = 0.6f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) 
                }
                
                if (type == "COMPETITION") { 
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = opponent, onValueChange = { opponent = it }, label = { Text("Adversaire", color = Color.White.copy(alpha = 0.6f)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) 
                }
                
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CONVOCATIONS (${playersOfTeam.size})", fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
                    if (!isConvocable && type == "COMPETITION") {
                        Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("Ouverture le ${selectedDate.minusDays(15).format(DateTimeFormatter.ofPattern("dd/MM"))}", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                
                playersOfTeam.forEach { p -> 
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isConvocable || type == "TRAINING") { 
                                selectedPlayerIds[p.id] = !(selectedPlayerIds[p.id] ?: false) 
                            }
                            .alpha(if (isConvocable || type == "TRAINING") 1f else 0.5f)
                    ) { 
                        Checkbox(
                            checked = selectedPlayerIds[p.id] ?: false, 
                            onCheckedChange = { selectedPlayerIds[p.id] = it }, 
                            enabled = isConvocable || type == "TRAINING",
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00B4D8), uncheckedColor = Color.White.copy(alpha = 0.5f))
                        )
                        Text("${p.firstName} ${p.lastName}", color = Color.White)
                        Spacer(Modifier.weight(1f))
                        Text(p.position, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)) 
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
                        val att = selectedPlayerIds.filter { it.value }.mapValues { "pending" }
                        val item = if (type == "TRAINING") {
                            CalendarListItem.Training(TrainingSession(id = UUID.randomUUID().toString(), teamId = selectedTeamId, date = selectedDate, startTime = sTime, durationMinutes = 90, focusArea = focus, attendance = att, isValidated = true))
                        } else {
                            CalendarListItem.Comp(CompetitionEvent(id = UUID.randomUUID().toString(), teamId = selectedTeamId, date = selectedDate, startTime = sTime, type = CompetitionType.CHAMPIONSHIP, opponent = opponent, location = "Gymnase", attendance = att))
                        }
                        onConfirm(item) { if (it) saveSuccess = true; isSaving = false } 
                    } catch (_: Exception) { isSaving = false } 
                }, 
                colors = ButtonDefaults.buttonColors(containerColor = if (saveSuccess) Color(0xFF4CAF50) else Color.White, contentColor = Color.Black), 
                enabled = !isSaving && !saveSuccess && (isConvocable || type == "TRAINING" || opponent.isNotBlank())
            ) { 
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp) 
                } else { 
                    val label = if (saveSuccess) "Convoqué !" else if (isConvocable || type == "TRAINING") "Enregistrer & Convoquer" else "Enregistrer Match"
                    val icon = if (saveSuccess) Icons.Default.Check else if (isConvocable || type == "TRAINING") Icons.AutoMirrored.Filled.Send else Icons.Default.Save
                    Icon(icon, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label) 
                } 
            } 
        }, 
        dismissButton = { 
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Annuler", color = Color.White.copy(alpha = 0.6f)) } 
        }
    )
}

@Composable
fun HelpSessionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedTheme by remember { mutableStateOf("Service / Réception") }
    var generatedSession by remember { mutableStateOf<String?>(null) }
    val themes = listOf("Service / Réception", "Attaque / Bloc", "Défense / Relance", "Systèmes de jeu")
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF002147).copy(alpha = 0.95f), title = { Text("Dépannage d'urgence", color = Color.White, fontWeight = FontWeight.Black) }, text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { if (generatedSession == null) { Text("Thème :", color = Color.White); themes.forEach { t -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedTheme = t }) { RadioButton(selected = selectedTheme == t, onClick = { selectedTheme = t }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00B4D8))); Text(t, color = Color.White) } } } else { Text("SÉANCE GÉNÉRÉE :", fontWeight = FontWeight.Bold, color = Color(0xFF00B4D8)); Text(generatedSession!!, color = Color.White, style = MaterialTheme.typography.bodySmall) } } }, confirmButton = { if (generatedSession == null) { Button(onClick = { generatedSession = "Séance $selectedTheme générée avec succès." }) { Text("Générer") } } else { Button(onClick = { onConfirm(selectedTheme) }) { Text("Utiliser cette séance") } } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Color.White.copy(alpha = 0.6f)) } })
}
