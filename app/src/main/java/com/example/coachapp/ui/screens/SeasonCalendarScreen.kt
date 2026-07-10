package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import java.time.LocalDate
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

@Composable
fun SeasonCalendarScreen(
    modifier: Modifier = Modifier,
    persistenceManager: PersistenceManager,
    seasonConfig: SeasonConfig,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    onUseHelp: () -> Unit,
    helpUsageCount: Int,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit
) {
    var config by remember { mutableStateOf(seasonConfig) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentView by remember { mutableStateOf("PROGRAM") }
    var filterType by remember { mutableStateOf("ALL") }
    var selectedTeamId by remember { mutableStateOf<String?>(null) }
    
    var showAddEvent by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showImportXml by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Saison 26/27", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            
            IconButton(onClick = { showImportXml = true }) {
                Icon(Icons.Default.FileUpload, contentDescription = "Import XML", tint = MaterialTheme.colorScheme.secondary)
            }
            
            TextButton(
                onClick = { showHelpDialog = true },
                enabled = helpUsageCount < 3,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpCenter, null)
                Spacer(Modifier.width(4.dp))
                Text("HELP! (${3 - helpUsageCount})")
            }

            IconButton(onClick = { showAddEvent = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedTeamId == null, onClick = { selectedTeamId = null }, label = { Text("Tous") })
            config.teams.forEach { team ->
                FilterChip(
                    selected = selectedTeamId == team.id,
                    onClick = { selectedTeamId = team.id },
                    label = { Text(team.name) },
                    leadingIcon = { Box(modifier = Modifier.size(8.dp).background(team.color, CircleShape)) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = currentView == "PROGRAM",
                    onClick = { currentView = "PROGRAM" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("Programme") }
                SegmentedButton(
                    selected = currentView == "MONTH",
                    onClick = { currentView = "MONTH" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("Calendrier") }
                SegmentedButton(
                    selected = currentView == "PLANNING",
                    onClick = { currentView = "PLANNING" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("Planification") }
            }
            
            IconButton(onClick = {
                filterType = when(filterType) {
                    "ALL" -> "TRAINING"
                    "TRAINING" -> "COMPETITION"
                    else -> "ALL"
                }
            }) {
                Icon(
                    imageVector = if (filterType == "ALL") Icons.Default.FilterList else Icons.Default.FilterAlt,
                    contentDescription = "Filtrer",
                    tint = if (filterType == "ALL") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentView == "MONTH") {
            MonthView(selectedDate, config, selectedTeamId, onDateSelected = { selectedDate = it })
            Spacer(modifier = Modifier.height(16.dp))
            DailyEventsList(selectedDate, config, filterType, selectedTeamId, persistenceManager, onNavigateToPreparer, onViewRecap) { config = persistenceManager.loadSeasonConfig() }
        } else if (currentView == "PLANNING") {
            SeasonPlannerView(
                config = config,
                selectedTeamId = selectedTeamId,
                cycles = viewModel.cycles,
                onCyclesUpdated = { updatedCycles ->
                    updatedCycles.forEach { cycle ->
                        if (viewModel.cycles.any { it.id == cycle.id }) {
                            viewModel.modifierCycle(cycle)
                        } else {
                            viewModel.ajouterCycle(cycle)
                        }
                    }
                }
            )
        } else {
            ProgramView(config, filterType, selectedTeamId, persistenceManager, onNavigateToPreparer, onViewRecap) { config = persistenceManager.loadSeasonConfig() }
        }
    }

    if (showAddEvent) {
        AddEventDialog(
            date = selectedDate,
            teams = config.teams,
            initialTeamId = selectedTeamId ?: config.teams.firstOrNull()?.id ?: "default",
            onDismiss = { showAddEvent = false },
            onConfirm = { event ->
                when (event) {
                    is CalendarListItem.Training -> config = config.copy(plannedTrainings = config.plannedTrainings + event.session)
                    is CalendarListItem.Comp -> config = config.copy(competitions = config.competitions + event.event)
                }
                persistenceManager.saveSeasonConfig(config)
                showAddEvent = false
            }
        )
    }

    if (showHelpDialog) {
        HelpSessionDialog(
            onDismiss = { showHelpDialog = false },
            onConfirm = { _ ->
                onUseHelp()
                showHelpDialog = false
            }
        )
    }

    if (showImportXml) {
        ImportXmlDialog(
            teams = config.teams,
            onDismiss = { showImportXml = false },
            onImport = { newEvents ->
                var newConfig = config
                newEvents.forEach { event ->
                    when (event) {
                        is CalendarListItem.Training -> newConfig = newConfig.copy(plannedTrainings = newConfig.plannedTrainings + event.session)
                        is CalendarListItem.Comp -> newConfig = newConfig.copy(competitions = newConfig.competitions + event.event)
                    }
                }
                config = newConfig
                persistenceManager.saveSeasonConfig(config)
                showImportXml = false
            }
        )
    }
}

@Composable
fun ImportXmlDialog(teams: List<Team>, onDismiss: () -> Unit, onImport: (List<CalendarListItem>) -> Unit) {
    var xmlContent by remember { mutableStateOf("") }
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importer Calendrier (XML)") },
        text = {
            Column {
                Text("Équipe cible :", style = MaterialTheme.typography.labelSmall)
                teams.forEach { team ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedTeamId = team.id }) {
                        RadioButton(selected = selectedTeamId == team.id, onClick = { selectedTeamId = team.id })
                        Text(team.name)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Copiez ici le contenu de votre fichier XML prévisionnel :", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = xmlContent,
                    onValueChange = { xmlContent = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("<calendar>...</calendar>") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val simulatedEvents = listOf(
                        CalendarListItem.Comp(CompetitionEvent(UUID.randomUUID().toString(), selectedTeamId, LocalDate.now().plusDays(7), LocalTime.of(15, 0), CompetitionType.CHAMPIONSHIP, "Equipe A (Simulée)", "Gymnase Central")),
                        CalendarListItem.Comp(CompetitionEvent(UUID.randomUUID().toString(), selectedTeamId, LocalDate.now().plusDays(21), LocalTime.of(14, 30), CompetitionType.CHAMPIONSHIP, "Equipe B (Simulée)", "Palais des Sports"))
                    )
                    onImport(simulatedEvents)
                },
                enabled = selectedTeamId.isNotBlank() && xmlContent.isNotBlank()
            ) { Text("Analyser & Importer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun ProgramView(config: SeasonConfig, filterType: String, teamId: String?, persistenceManager: PersistenceManager, onNavigateToPreparer: (TrainingSession) -> Unit, onViewRecap: (TrainingSession) -> Unit, onUpdate: () -> Unit) {
    val today = LocalDate.now()
    val weeksToShow = 8
    val activeDays = remember(config, filterType, teamId) {
        val schedules = if (teamId == null) config.trainingSchedules else config.trainingSchedules.filter { it.teamId == teamId }
        val trainingDays = schedules.map { it.dayOfWeek }.toSet()
        
        val list = mutableListOf<LocalDate>()
        for (i in 0 until (weeksToShow * 7)) {
            val date = today.plusDays(i.toLong())
            val isTrainingDay = trainingDays.contains(date.dayOfWeek)
            val hasComp = config.competitions.any { (teamId == null || it.teamId == teamId) && it.date == date }
            
            if (isTrainingDay || hasComp) {
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
                        DailyEventsList(date, config, filterType, teamId, persistenceManager, onNavigateToPreparer, onViewRecap, onUpdate)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun DailyEventsList(date: LocalDate, config: SeasonConfig, filterType: String, teamId: String?, persistenceManager: PersistenceManager, onNavigateToPreparer: (TrainingSession) -> Unit, onViewRecap: (TrainingSession) -> Unit, onUpdate: () -> Unit) {
    val dayEvents = remember(date, config, filterType, teamId) {
        val sessions = config.plannedTrainings.filter { it.date == date && (teamId == null || it.teamId == teamId) }
        val comps = config.competitions.filter { it.date == date && (teamId == null || it.teamId == teamId) }
        
        val schedules = if (teamId == null) config.trainingSchedules else config.trainingSchedules.filter { it.teamId == teamId }
        val scheduledSchedules = if (sessions.isEmpty()) {
            schedules.filter { it.dayOfWeek == date.dayOfWeek }
        } else emptyList()

        val list = mutableListOf<CalendarListItem>()
        if (filterType == "ALL" || filterType == "TRAINING") {
            list.addAll(sessions.map { CalendarListItem.Training(it) })
            list.addAll(scheduledSchedules.map { schedule -> 
                CalendarListItem.Training(TrainingSession(
                    id = "template_${date}_${schedule.teamId}_${schedule.startTime}",
                    teamId = schedule.teamId,
                    date = date,
                    startTime = schedule.startTime,
                    durationMinutes = schedule.durationMinutes,
                    focusArea = null
                ))
            })
        }
        if (filterType == "ALL" || filterType == "COMPETITION") {
            list.addAll(comps.map { CalendarListItem.Comp(it) })
        }
        list.sortedBy { it.time }
    }

    dayEvents.forEach { item ->
        val team = config.teams.find { it.id == item.teamId }
        when (item) {
            is CalendarListItem.Training -> TrainingSessionCard(item.session, team, persistenceManager, config, onNavigateToPreparer, onViewRecap, onUpdate)
            is CalendarListItem.Comp -> CompetitionEventCard(item.event, team)
        }
    }
}

@Composable
fun TrainingSessionCard(
    session: TrainingSession,
    team: Team?,
    persistenceManager: PersistenceManager,
    seasonConfig: SeasonConfig,
    onNavigateToPreparer: (TrainingSession) -> Unit,
    onViewRecap: (TrainingSession) -> Unit,
    onUpdate: () -> Unit
) {
    var showEditSession by remember { mutableStateOf(false) }
    
    val isPrepared = session.warmup.isNotBlank() || session.drills.isNotBlank() || 
                     session.smallGroupSituations.isNotBlank() || session.collectiveGame.isNotBlank()
    val isEvaluated = session.assessmentId != null
    
    val statusColor = when {
        isEvaluated -> Color(0xFF4CAF50)
        isPrepared -> MaterialTheme.colorScheme.primary
        else -> Color.Gray
    }
    
    val statusLabel = when {
        isEvaluated -> "TERMINÉE"
        isPrepared -> "PRÊTE"
        else -> "À PRÉPARER"
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
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(session.startTime.toString(), style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsVolleyball, contentDescription = null, tint = team?.color ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                TextButton(onClick = { /* Attendance */ }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${session.attendance.size} présents", fontSize = 11.sp)
                }
                
                Spacer(Modifier.weight(1f))
                
                if (!isEvaluated) {
                    Button(
                        onClick = { onNavigateToPreparer(session) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPrepared) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
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
            onDismiss = { showEditSession = false },
            onConfirm = { updated ->
                val config = persistenceManager.loadSeasonConfig()
                val newList = if (!config.plannedTrainings.any { it.id == session.id }) config.plannedTrainings + updated 
                              else config.plannedTrainings.map { if (it.id == session.id) updated else it }
                persistenceManager.saveSeasonConfig(config.copy(plannedTrainings = newList))
                showEditSession = false
                onUpdate()
            }
        )
    }
}

@Composable
fun MonthView(currentDate: LocalDate, config: SeasonConfig, teamId: String?, onDateSelected: (LocalDate) -> Unit) {
    val firstDayOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth())
    val lastDayOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth())
    val daysInMonth = lastDayOfMonth.dayOfMonth
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDateSelected(currentDate.minusMonths(1).withDayOfMonth(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent")
            }
            Text(
                text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onDateSelected(currentDate.plusMonths(1).withDayOfMonth(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        val hasTraining = config.trainingSchedules.any { (teamId == null || it.teamId == teamId) && it.dayOfWeek == date.dayOfWeek } || 
                                          config.plannedTrainings.any { (teamId == null || it.teamId == teamId) && it.date == date }
                        val hasComp = config.competitions.any { (teamId == null || it.teamId == teamId) && it.date == date }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayNum.toString(), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Row {
                                    if (hasTraining) Box(modifier = Modifier.size(4.dp).background(Color.Gray, CircleShape))
                                    if (hasComp) Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
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
fun EditSessionDialog(
    session: TrainingSession,
    seasonConfig: SeasonConfig,
    onDismiss: () -> Unit,
    onConfirm: (TrainingSession) -> Unit
) {
    var focus by remember { mutableStateOf(session.focusArea ?: "") }
    val focusOptions = listOf("Service / Réception", "Attaque / Bloc", "Défense / Relance", "Systèmes de jeu", "Physique")
    val tempAttendance = remember { mutableStateListOf<String>().apply { addAll(session.attendance) } }
    val teamPlayers = remember(session.teamId, seasonConfig.players) {
        seasonConfig.players.filter { it.teamId == session.teamId }
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
                OutlinedTextField(value = focus, onValueChange = { focus = it }, label = { Text("Autre thème...") })
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Feuille de présence :", fontWeight = FontWeight.Bold)
                if (teamPlayers.isEmpty()) {
                    Text("Aucun joueur pour cette équipe.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    teamPlayers.forEach { player ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                            if (tempAttendance.contains(player.id)) tempAttendance.remove(player.id)
                            else tempAttendance.add(player.id)
                        }) {
                            Checkbox(checked = tempAttendance.contains(player.id), onCheckedChange = null)
                            Text("${player.fullName} (#${player.number})")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(session.copy(focusArea = focus, attendance = tempAttendance.toList())) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(date: LocalDate, teams: List<Team>, initialTeamId: String, onDismiss: () -> Unit, onConfirm: (CalendarListItem) -> Unit) {
    var type by remember { mutableStateOf("TRAINING") }
    var selectedTeamId by remember { mutableStateOf(initialTeamId) }
    var focus by remember { mutableStateOf("Service / Réception") }
    var opponent by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf("18") }
    var startMinute by remember { mutableStateOf("30") }
    var endHour by remember { mutableStateOf("20") }
    var endMinute by remember { mutableStateOf("00") }
    
    val focusOptions = listOf("Service / Réception", "Attaque / Bloc", "Défense / Relance", "Systèmes de jeu", "Physique")

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Confirmer") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel événement") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Équipe :", fontWeight = FontWeight.Bold)
                teams.forEach { team ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedTeamId = team.id }) {
                        RadioButton(selected = selectedTeamId == team.id, onClick = { selectedTeamId = team.id })
                        Text(team.name)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    FilterChip(selected = type == "TRAINING", onClick = { type = "TRAINING" }, label = { Text("Entraînement") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == "COMPETITION", onClick = { type = "COMPETITION" }, label = { Text("Match") })
                }
                Spacer(Modifier.height(16.dp))
                Text("Date :", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)))
                }
                Spacer(Modifier.height(16.dp))
                Text("Horaires :", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("De ", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = startHour, onValueChange = { if(it.length <= 2) startHour = it }, modifier = Modifier.width(65.dp), label = { Text("HH") })
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = startMinute, onValueChange = { if(it.length <= 2) startMinute = it }, modifier = Modifier.width(65.dp), label = { Text("mm") })
                    Text(" à ", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = endHour, onValueChange = { if(it.length <= 2) endHour = it }, modifier = Modifier.width(65.dp), label = { Text("HH") })
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = endMinute, onValueChange = { if(it.length <= 2) endMinute = it }, modifier = Modifier.width(65.dp), label = { Text("mm") })
                }
                Spacer(Modifier.height(16.dp))
                if (type == "TRAINING") {
                    Text("Thème de la séance :", style = MaterialTheme.typography.labelSmall)
                    focusOptions.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { focus = option }) {
                            RadioButton(selected = focus == option, onClick = { focus = option })
                            Text(option)
                        }
                    }
                } else {
                    OutlinedTextField(value = opponent, onValueChange = { opponent = it }, label = { Text("Adversaire") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val startTime = LocalTime.of(startHour.toInt(), startMinute.toInt())
                    val endTime = LocalTime.of(endHour.toInt(), endMinute.toInt())
                    var duration = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
                    if (duration < 0) duration += 24 * 60
                    if (type == "TRAINING") {
                        onConfirm(CalendarListItem.Training(TrainingSession(UUID.randomUUID().toString(), selectedTeamId, selectedDate, startTime, duration, focus)))
                    } else {
                        onConfirm(CalendarListItem.Comp(CompetitionEvent(UUID.randomUUID().toString(), selectedTeamId, selectedDate, startTime, CompetitionType.CHAMPIONSHIP, opponent, "Gymnase")))
                    }
                } catch (_: Exception) {}
            }) { Text("Ajouter") }
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
