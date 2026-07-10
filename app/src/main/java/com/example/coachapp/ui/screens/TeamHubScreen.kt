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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

@Composable
fun TeamHubScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig,
    onUpdateConfig: (SeasonConfig) -> Unit,
    onUpdatePlayer: (Player) -> Unit,
    onDeletePlayer: (String) -> Unit,
    onAddAssessment: (String, PlayerAssessment) -> Unit
) {
    var expandedSection by remember { mutableStateOf("PLAYERS") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Mes Collectifs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Gestion des groupes, joueurs et plannings.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))

        ProfileExpandableSection(
            title = "Joueurs & Effectifs",
            description = "Gérez vos licenciés, postes et bilans de paliers.",
            icon = Icons.Default.Groups,
            isExpanded = expandedSection == "PLAYERS",
            onToggle = { expandedSection = if (expandedSection == "PLAYERS") "" else "PLAYERS" }
        ) {
            TeamScreenContent(seasonConfig, onUpdatePlayer, onDeletePlayer, onAddAssessment)
        }

        Spacer(modifier = Modifier.height(12.dp))

        ProfileExpandableSection(
            title = "Gestion des Catégories",
            description = "Ajoutez ou modifiez vos équipes (M13, Seniors, etc.).",
            icon = Icons.Default.Settings,
            isExpanded = expandedSection == "TEAMS",
            onToggle = { expandedSection = if (expandedSection == "TEAMS") "" else "TEAMS" }
        ) {
            var showAddTeam by remember { mutableStateOf(false) }
            TeamManager(seasonConfig, onUpdateConfig, onAddClick = { showAddTeam = true })
            if (showAddTeam) {
                AddTeamDialog(onDismiss = { showAddTeam = false }, onConfirm = { onUpdateConfig(seasonConfig.copy(teams = seasonConfig.teams + it)); showAddTeam = false })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ProfileExpandableSection(
            title = "Planning Hebdomadaire",
            description = "Définissez les créneaux récurrents de vos équipes.",
            icon = Icons.Default.DateRange,
            isExpanded = expandedSection == "SCHEDULE",
            onToggle = { expandedSection = if (expandedSection == "SCHEDULE") "" else "SCHEDULE" }
        ) {
            var showAddTraining by remember { mutableStateOf(false) }
            ScheduleManager(seasonConfig, onUpdateConfig, onAddClick = { showAddTraining = true })
            if (showAddTraining) {
                AddTrainingScheduleDialog(teams = seasonConfig.teams, onDismiss = { showAddTraining = false }, onConfirm = { onUpdateConfig(seasonConfig.copy(trainingSchedules = seasonConfig.trainingSchedules + it)); showAddTraining = false })
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun TeamScreenContent(
    seasonConfig: SeasonConfig,
    onUpdatePlayer: (Player) -> Unit,
    onDeletePlayer: (String) -> Unit,
    onAddAssessment: (String, PlayerAssessment) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(seasonConfig.teams.firstOrNull()?.id) }
    val selectedTeam = seasonConfig.teams.find { it.id == selectedTeamId }
    var showAddPlayer by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<Player?>(null) }
    var selectedPlayerForReview by remember { mutableStateOf<Player?>(null) }

    Column {
        if (seasonConfig.teams.isNotEmpty()) {
            ScrollableTabRow(selectedTabIndex = seasonConfig.teams.indexOf(selectedTeam).coerceAtLeast(0), edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}) {
                seasonConfig.teams.forEach { team ->
                    Tab(selected = selectedTeamId == team.id, onClick = { selectedTeamId = team.id }) {
                        Text(team.name, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Effectif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddPlayer = true }) { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) }
            }

            val teamPlayers = seasonConfig.players.filter { it.teamId == selectedTeamId }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                teamPlayers.forEach { player ->
                    PlayerCard(
                        player = player, 
                        seasonConfig = seasonConfig, 
                        onDelete = { onDeletePlayer(player.id) },
                        onEditClick = { playerToEdit = player },
                        onReviewClick = { selectedPlayerForReview = player }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { if (selectedTeamId != null) importTestData(selectedTeamId!!, selectedTeam?.name ?: "", onUpdatePlayer) }, modifier = Modifier.fillMaxWidth()) {
                Text("Import Test JSON (Discriminé)", fontSize = 11.sp)
            }
        } else {
            Text("Créez d'abord une équipe dans l'onglet 'Gestion des Catégories'.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }

    if (showAddPlayer && selectedTeamId != null) {
        PlayerDialog(teamId = selectedTeamId!!, onDismiss = { showAddPlayer = false }, onConfirm = { onUpdatePlayer(it); showAddPlayer = false })
    }
    if (playerToEdit != null) {
        PlayerDialog(teamId = playerToEdit!!.teamId, player = playerToEdit, onDismiss = { playerToEdit = null }, onConfirm = { onUpdatePlayer(it); playerToEdit = null })
    }
    if (selectedPlayerForReview != null) {
        PlayerReviewDialog(player = selectedPlayerForReview!!, onDismiss = { selectedPlayerForReview = null }, onConfirm = { onAddAssessment(selectedPlayerForReview!!.id, it); selectedPlayerForReview = null })
    }
}

@Composable
fun PlayerDialog(teamId: String, player: Player? = null, onDismiss: () -> Unit, onConfirm: (Player) -> Unit) {
    var firstName by remember { mutableStateOf(player?.firstName ?: "") }
    var lastName by remember { mutableStateOf(player?.lastName ?: "") }
    var number by remember { mutableStateOf(player?.number?.toString() ?: "") }
    var license by remember { mutableStateOf(player?.licenseNumber ?: "") }
    var birthYear by remember { mutableStateOf(player?.birthYear?.toString() ?: "") }
    var position by remember { mutableStateOf(player?.position ?: "Réceptionneur-Attaquant") }
    var yearsOfPractice by remember { mutableStateOf(player?.yearsOfPractice?.toString() ?: "0") }
    var category by remember { mutableStateOf(player?.category ?: "M13") }
    var categoryYear by remember { mutableIntStateOf(player?.categoryYear ?: 1) }
    val positions = listOf("Passeur", "Pointu", "Central", "Réceptionneur-Attaquant", "Libero")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (player == null) "Nouveau Joueur" else "Modifier") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Prénom") })
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Nom") })
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Numéro") })
                OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("Licence") })
                OutlinedTextField(value = birthYear, onValueChange = { birthYear = it }, label = { Text("Année Naissance") })
                Text("Poste :")
                Row { positions.take(3).forEach { p -> FilterChip(selected = position == p, onClick = { position = p }, label = { Text(p.take(3)) }) } }
                OutlinedTextField(value = yearsOfPractice, onValueChange = { yearsOfPractice = it }, label = { Text("Années Pratique") })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(Player(player?.id ?: UUID.randomUUID().toString(), teamId, firstName, lastName, number.toIntOrNull() ?: 0, position, license, birthYear.toIntOrNull() ?: 0, yearsOfPractice.toIntOrNull() ?: 0, category, categoryYear)) }) { Text("OK") } }
    )
}

@Composable
fun PlayerCard(player: Player, seasonConfig: SeasonConfig, onDelete: () -> Unit, onEditClick: () -> Unit, onReviewClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp).clickable { onEditClick() }, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("#${player.number}", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.fullName, fontWeight = FontWeight.Bold)
                Text("${player.position} (${player.category})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onReviewClick) { Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun PlayerReviewDialog(player: Player, onDismiss: () -> Unit, onConfirm: (PlayerAssessment) -> Unit) {
    var tech by remember { mutableFloatStateOf(player.techScore.toFloat()) }
    var tact by remember { mutableFloatStateOf(player.tactScore.toFloat()) }
    var phys by remember { mutableFloatStateOf(player.physicalScore.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bilan : ${player.fullName}") },
        text = {
            Column {
                Text("Technique: ${tech.toInt()}"); Slider(value = tech, onValueChange = { tech = it }, valueRange = 0f..5f)
                Text("Tactique: ${tact.toInt()}"); Slider(value = tact, onValueChange = { tact = it }, valueRange = 0f..5f)
                Text("Physique: ${phys.toInt()}"); Slider(value = phys, onValueChange = { phys = it }, valueRange = 0f..5f)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(PlayerAssessment(System.currentTimeMillis(), "Mois", tech.toInt(), tact.toInt(), phys.toInt())) }) { Text("OK") } }
    )
}

@Composable
fun TeamManager(config: SeasonConfig, onUpdate: (SeasonConfig) -> Unit, onAddClick: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Équipes", fontWeight = FontWeight.Bold)
            IconButton(onClick = onAddClick) { Icon(Icons.Default.Add, null) }
        }
        config.teams.forEach { team ->
            ListItem(headlineContent = { Text(team.name) }, leadingContent = { Box(Modifier.size(16.dp).background(team.color, CircleShape)) })
        }
    }
}

@Composable
fun ScheduleManager(config: SeasonConfig, onUpdate: (SeasonConfig) -> Unit, onAddClick: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Plannings Hebdo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAddClick) { Icon(Icons.Default.Add, null) }
        }
        config.trainingSchedules.forEach { schedule ->
            val team = config.teams.find { it.id == schedule.teamId }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(schedule.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(team?.name ?: "Inconnu", style = MaterialTheme.typography.labelSmall, color = team?.color ?: Color.Gray)
                    }
                    Text("${schedule.startTime} (${schedule.durationMinutes} min)", fontSize = 11.sp)
                    IconButton(onClick = {
                        onUpdate(config.copy(trainingSchedules = config.trainingSchedules.filter { it != schedule }))
                    }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun AddTeamDialog(onDismiss: () -> Unit, onConfirm: (Team) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nouvelle Équipe") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }) }, confirmButton = { Button(onClick = { onConfirm(Team(UUID.randomUUID().toString(), name, Color.Blue)) }) { Text("OK") } })
}

@Composable
fun AddTrainingScheduleDialog(teams: List<Team>, onDismiss: () -> Unit, onConfirm: (List<TrainingSchedule>) -> Unit) {
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }
    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id ?: "") }
    var startHour by remember { mutableStateOf("18") }
    var startMinute by remember { mutableStateOf("30") }
    var endHour by remember { mutableStateOf("20") }
    var endMinute by remember { mutableStateOf("00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveaux créneaux réguliers") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Équipe :", style = MaterialTheme.typography.labelSmall)
                teams.forEach { team ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedTeamId = team.id }) {
                        RadioButton(selected = selectedTeamId == team.id, onClick = { selectedTeamId = team.id })
                        Text(team.name)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Jours (sélectionnez-en plusieurs) :", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    DayOfWeek.entries.take(7).forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        Surface(
                            modifier = Modifier.size(32.dp).clickable { 
                                if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(day.getDisplayName(TextStyle.NARROW, Locale.FRENCH), color = if (isSelected) Color.White else Color.Unspecified)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Horaires communs :", style = MaterialTheme.typography.labelSmall)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Début : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                    OutlinedTextField(value = startHour, onValueChange = { if(it.length <= 2) startHour = it }, modifier = Modifier.width(65.dp), label = { Text("HH") })
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = startMinute, onValueChange = { if(it.length <= 2) startMinute = it }, modifier = Modifier.width(65.dp), label = { Text("mm") })
                }
                
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fin : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                    OutlinedTextField(value = endHour, onValueChange = { if(it.length <= 2) endHour = it }, modifier = Modifier.width(65.dp), label = { Text("HH") })
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = endMinute, onValueChange = { if(it.length <= 2) endMinute = it }, modifier = Modifier.width(65.dp), label = { Text("mm") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val startTime = LocalTime.of(startHour.toInt(), startMinute.toInt())
                        val endTime = LocalTime.of(endHour.toInt(), endMinute.toInt())
                        var duration = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
                        if (duration < 0) duration += 24 * 60
                        val schedules = selectedDays.map { TrainingSchedule(selectedTeamId, dayOfWeek = it, startTime = startTime, durationMinutes = duration) }
                        onConfirm(schedules)
                    } catch (e: Exception) {}
                }, 
                enabled = selectedTeamId.isNotBlank() && selectedDays.isNotEmpty()
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

fun importTestData(teamId: String, teamName: String, onUpdatePlayer: (Player) -> Unit) {
    val teamCategory = when {
        teamName.contains("M13", ignoreCase = true) -> "M13"
        teamName.contains("M15", ignoreCase = true) -> "M15"
        teamName.contains("M18", ignoreCase = true) -> "M18"
        teamName.contains("M21", ignoreCase = true) -> "M21"
        teamName.contains("Senior", ignoreCase = true) -> "Senior"
        else -> null
    }

    val json = """
    {
      "players": [
        { "firstName": "Léo", "lastName": "Martin", "number": 12, "license": "AUVR-2026-45821", "position": "Réceptionneur-Attaquant", "yearsOfPractice": 8, "birthYear": 2010, "category": "M18", "categoryYear": 1 },
        { "firstName": "Nolan", "lastName": "Dupuis", "number": 7, "license": "AUVR-2026-44712", "position": "Passeur", "yearsOfPractice": 6, "birthYear": 2009, "category": "M18", "categoryYear": 2 },
        { "firstName": "Mathis", "lastName": "Roche", "number": 3, "license": "AUVR-2026-46211", "position": "Pointu", "yearsOfPractice": 4, "birthYear": 2011, "category": "M15", "categoryYear": 1 },
        { "firstName": "Enzo", "lastName": "Bardet", "number": 9, "license": "AUVR-2026-44198", "position": "Central", "yearsOfPractice": 5, "birthYear": 2010, "category": "M18", "categoryYear": 1 },
        { "firstName": "Tom", "lastName": "Garnier", "number": 1, "license": "AUVR-2026-43012", "position": "Libéro", "yearsOfPractice": 7, "birthYear": 2007, "category": "M21", "categoryYear": 1 },
        { "firstName": "Axel", "lastName": "Morel", "number": 14, "license": "AUVR-2026-47812", "position": "Réceptionneur-Attaquant", "yearsOfPractice": 3, "birthYear": 2012, "category": "M15", "categoryYear": 2 },
        { "firstName": "Julien", "lastName": "Carrel", "number": 5, "license": "AUVR-2026-45571", "position": "Central", "yearsOfPractice": 9, "birthYear": 2004, "category": "Senior", "categoryYear": 1 },
        { "firstName": "Maxime", "lastName": "Leroux", "number": 11, "license": "AUVR-2026-49017", "position": "Passeur", "yearsOfPractice": 10, "birthYear": 2003, "category": "Senior", "categoryYear": 3 },
        { "firstName": "Evan", "lastName": "Rigal", "number": 4, "license": "AUVR-2026-47201", "position": "Libéro", "yearsOfPractice": 2, "birthYear": 2013, "category": "M13", "categoryYear": 1 },
        { "firstName": "Sacha", "lastName": "Bonnard", "number": 10, "license": "AUVR-2026-48214", "position": "Pointu", "yearsOfPractice": 3, "birthYear": 2012, "category": "M13", "categoryYear": 2 },
        { "firstName": "Hugo", "lastName": "Lambert", "number": 6, "license": "AUVR-2026-49321", "position": "Central", "yearsOfPractice": 6, "birthYear": 2009, "category": "M18", "categoryYear": 2 },
        { "firstName": "Théo", "lastName": "Giraud", "number": 8, "license": "AUVR-2026-49912", "position": "Réceptionneur-Attaquant", "yearsOfPractice": 5, "birthYear": 2010, "category": "M18", "categoryYear": 1 },
        { "firstName": "Lucas", "lastName": "Perrin", "number": 13, "license": "AUVR-2026-48871", "position": "Passeur", "yearsOfPractice": 4, "birthYear": 2011, "category": "M15", "categoryYear": 1 },
        { "firstName": "Eliott", "lastName": "Masson", "number": 2, "license": "AUVR-2026-47112", "position": "Libéro", "yearsOfPractice": 7, "birthYear": 2008, "category": "M21", "categoryYear": 2 },
        { "firstName": "Rayan", "lastName": "Collet", "number": 15, "license": "AUVR-2026-46512", "position": "Pointu", "yearsOfPractice": 1, "birthYear": 2013, "category": "M13", "categoryYear": 1 },
        { "firstName": "Maël", "lastName": "Durand", "number": 16, "license": "AUVR-2026-48012", "position": "Central", "yearsOfPractice": 8, "birthYear": 2007, "category": "M21", "categoryYear": 1 },
        { "firstName": "Antoine", "lastName": "Fabre", "number": 17, "license": "AUVR-2026-49211", "position": "Réceptionneur-Attaquant", "yearsOfPractice": 9, "birthYear": 2006, "category": "M21", "categoryYear": 2 },
        { "firstName": "Clément", "lastName": "Renaud", "number": 18, "license": "AUVR-2026-47712", "position": "Passeur", "yearsOfPractice": 11, "birthYear": 2004, "category": "Senior", "categoryYear": 1 },
        { "firstName": "Baptiste", "lastName": "Gros", "number": 19, "license": "AUVR-2026-48612", "position": "Libéro", "yearsOfPractice": 3, "birthYear": 2012, "category": "M15", "categoryYear": 2 },
        { "firstName": "Valentin", "lastName": "Chevalier", "number": 20, "license": "AUVR-2026-48912", "position": "Pointu", "yearsOfPractice": 6, "birthYear": 2009, "category": "M18", "categoryYear": 2 }
      ]
    }
    """.trimIndent()

    try {
        val obj = org.json.JSONObject(json)
        val arr = obj.getJSONArray("players")
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val pCategory = p.getString("category")
            
            val isEligible = when(teamCategory) {
                "M13" -> pCategory == "M13"
                "M15" -> pCategory == "M15" || pCategory == "M13"
                "M18" -> pCategory == "M18" || pCategory == "M15" || pCategory == "M13"
                "M21" -> pCategory == "M21" || pCategory == "M18" || pCategory == "M15"
                "Senior" -> true 
                else -> true 
            }

            if (isEligible) {
                onUpdatePlayer(Player(
                    id = UUID.randomUUID().toString(),
                    teamId = teamId,
                    firstName = p.getString("firstName"),
                    lastName = p.getString("lastName"),
                    number = p.getInt("number"),
                    position = p.getString("position"),
                    licenseNumber = p.getString("license"),
                    yearsOfPractice = p.getInt("yearsOfPractice"),
                    birthYear = p.getInt("birthYear"),
                    category = pCategory,
                    categoryYear = p.getInt("categoryYear")
                ))
            }
        }
    } catch (e: Exception) {}
}
