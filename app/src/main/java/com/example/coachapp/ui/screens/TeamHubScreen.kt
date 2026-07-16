package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
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
import com.example.coachapp.data.SeasonConfig
import com.example.coachapp.data.TrainingSchedule
import com.example.coachapp.data.model.Collectif
import com.example.coachapp.ui.president.PresidentViewModel
import com.example.coachapp.ui.president.PresidentUiState
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale
import java.time.format.TextStyle
import java.util.*

@Composable
fun TeamHubScreen(
    modifier: Modifier = Modifier,
    viewModel: PresidentViewModel,
    seasonConfig: SeasonConfig,
    onUpdateConfig: (SeasonConfig) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clubPlanning by viewModel.clubPlanning.collectAsState()
    var selectedCollectifForEffectif by remember { mutableStateOf<Collectif?>(null) }
    var selectedCollectifForSchedule by remember { mutableStateOf<Collectif?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (selectedCollectifForEffectif != null) {
        android.util.Log.d("DIAG_PRESIDENT", "Navigation vers collectif: clubCode=${viewModel.clubCode}")
        CollectifDetailScreen(
            collectifId = selectedCollectifForEffectif!!.id,
            collectifNom = selectedCollectifForEffectif!!.nom,
            collectifFormat = selectedCollectifForEffectif!!.format,
            collectifSexe = selectedCollectifForEffectif!!.sexe,
            categorieCoach = selectedCollectifForEffectif!!.categorie,
            clubId = selectedCollectifForEffectif!!.clubId,
            clubCode = viewModel.clubCode ?: "",
            viewModel = viewModel,
            onBack = { 
                selectedCollectifForEffectif = null 
                viewModel.chargerCollectifs() 
            }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Mes Collectifs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Gérez les effectifs et plannings de vos équipes.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))

                when (val state = uiState) {
                    is PresidentUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PresidentUiState.Error -> {
                        Text("Erreur : ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                    is PresidentUiState.Success -> {
                        val userId = com.example.coachapp.data.SupabaseManager.auth.currentUserOrNull()?.id
                        val mesCollectifs = state.collectifs.filter { detail ->
                            detail.rattachements.any { it.coachId == userId }
                        }

                        if (mesCollectifs.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Aucun collectif rattaché.", style = MaterialTheme.typography.titleMedium)
                                    Text("Demandez à votre président ou rattachiez-vous depuis le Hub Président.", 
                                        textAlign = TextAlign.Center, 
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(mesCollectifs) { detail ->
                                    // On utilise le clubPlanning synchronisé avec Supabase pour l'affichage
                                    val schedules = clubPlanning.filter { it.teamId == detail.collectif.id }
                                    
                                    // Calcul couleur état (Rouge, Orange, Vert, Bleu)
                                    val count = detail.joueurs.size
                                    // TODO: Récupérer le vrai seuil min via formatLimite, en attendant on met 6 par défaut
                                    val statusColor = when {
                                        detail.collectif.statut == com.example.coachapp.data.model.CollectifStatut.EN_ATTENTE_CT -> Color(0xFF2196F3) // Bleu
                                        count >= 6 -> Color(0xFF4CAF50) // Vert (Quota atteint théorique)
                                        count > 0 -> Color(0xFFFF9800) // Orange (En cours)
                                        else -> Color(0xFFF44336) // Rouge (Vide)
                                    }

                                    Column {
                                        CollectifCoachCard(
                                            collectif = detail.collectif,
                                            statusColor = statusColor,
                                            onClick = { selectedCollectifForEffectif = detail.collectif }
                                        )
                                        
                                        if (schedules.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                schedules.forEach { s ->
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            "${s.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.FRENCH)} ${s.startTime}",
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        TextButton(
                                            onClick = { selectedCollectifForSchedule = detail.collectif },
                                            modifier = Modifier.padding(top = 2.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Indiquer les horaires et jours récurrents d'entrainement",
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedCollectifForSchedule != null) {
        val currentSchedules = seasonConfig.trainingSchedules.filter { it.teamId == selectedCollectifForSchedule!!.id }
        
        AddTrainingScheduleDialog(
            collectifNom = selectedCollectifForSchedule!!.nom,
            currentSchedules = currentSchedules,
            clubPlanning = clubPlanning,
            onDismiss = { selectedCollectifForSchedule = null },
            onConfirm = { newSchedules ->
                val teamId = selectedCollectifForSchedule!!.id
                
                // 1. Gérer les suppressions (Jours décochés)
                val newDays = newSchedules.map { it.dayOfWeek }.toSet()
                val schedulesToDelete = currentSchedules.filter { it.dayOfWeek !in newDays }
                
                schedulesToDelete.forEach { s ->
                    s.id?.let { id ->
                        viewModel.supprimerPlanning(
                            scheduleId = id,
                            onSuccess = {},
                            onError = { scope.launch { snackbarHostState.showSnackbar("Erreur suppression: $it") } }
                        )
                    }
                }

                // 2. Gérer les ajouts et mises à jour
                val finalSchedulesToSave = newSchedules.map { ns ->
                    // On cherche si ce jour existait déjà pour garder l'ID Supabase
                    val existing = currentSchedules.find { it.dayOfWeek == ns.dayOfWeek }
                    ns.copy(id = existing?.id, teamId = teamId)
                }

                finalSchedulesToSave.forEach { s ->
                    viewModel.enregistrerPlanning(
                        schedule = s,
                        onSuccess = {},
                        onError = { scope.launch { snackbarHostState.showSnackbar("Erreur sauvegarde: $it") } }
                    )
                }

                // 3. Mise à jour locale (Source de vérité pour l'affichage immédiat)
                val otherTeamsSchedules = seasonConfig.trainingSchedules.filter { it.teamId != teamId }
                onUpdateConfig(seasonConfig.copy(trainingSchedules = otherTeamsSchedules + finalSchedulesToSave))
                
                selectedCollectifForSchedule = null
            }
        )
    }
}

@Composable
fun CollectifCoachCard(
    collectif: Collectif,
    statusColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Bandeau de titre colorisé
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${collectif.categorie} ${if(collectif.sexe == "M") "Masculin" else "Féminin"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(collectif.nom, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }
            // On pourrait ajouter un petit récap ici si besoin, 
            // mais l'utilisateur a demandé à garder la structure actuelle
        }
    }
}

@Composable
fun AddTrainingScheduleDialog(
    collectifNom: String,
    currentSchedules: List<TrainingSchedule>,
    clubPlanning: List<TrainingSchedule> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<TrainingSchedule>) -> Unit
) {
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }
    var startHour by remember { mutableStateOf("18") }
    var startMinute by remember { mutableStateOf("30") }
    var endHour by remember { mutableStateOf("20") }
    var endMinute by remember { mutableStateOf("00") }
    var selectedTerrain by remember { mutableStateOf("Terrain 1") }
    val terrains = listOf("Terrain 1", "Terrain 2", "Terrain 3", "Central")

    LaunchedEffect(currentSchedules) {
        if (currentSchedules.isNotEmpty()) {
            selectedDays.clear()
            currentSchedules.forEach { selectedDays.add(it.dayOfWeek) }
            val first = currentSchedules.first()
            startHour = first.startTime.hour.toString().padStart(2, '0')
            startMinute = first.startTime.minute.toString().padStart(2, '0')
            selectedTerrain = first.terrain ?: "Terrain 1"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Horaires récurrents : $collectifNom") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Terrain :", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    terrains.forEach { t ->
                        FilterChip(
                            selected = selectedTerrain == t,
                            onClick = { selectedTerrain = t },
                            label = { Text(t, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Sélectionnez les jours d'entraînement :", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    DayOfWeek.entries.take(7).forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        
                        // Détection de conflit simple
                        val hasConflict = clubPlanning.any { 
                            it.dayOfWeek == day && it.terrain == selectedTerrain
                        }

                        Surface(
                            modifier = Modifier.size(36.dp).clickable { 
                                if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else if (hasConflict) Color(0xFFFFEBEE)
                                    else Color.Transparent,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (hasConflict) Color.Red.copy(alpha = 0.5f) else Color.Gray)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    day.getDisplayName(TextStyle.NARROW, Locale.FRENCH), 
                                    color = if (isSelected) Color.White else if (hasConflict) Color.Red else Color.Unspecified,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                if (clubPlanning.isNotEmpty()) {
                    Text("Les jours en rouge sont déjà occupés sur ce terrain par d'autres équipes.", 
                         style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                }
                
                Spacer(Modifier.height(24.dp))
                Text("Horaires habituels :", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Début : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                    OutlinedTextField(value = startHour, onValueChange = { if(it.length <= 2) startHour = it }, modifier = Modifier.width(70.dp), label = { Text("HH") })
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = startMinute, onValueChange = { if(it.length <= 2) startMinute = it }, modifier = Modifier.width(70.dp), label = { Text("mm") })
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fin : ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                    OutlinedTextField(value = endHour, onValueChange = { if(it.length <= 2) endHour = it }, modifier = Modifier.width(70.dp), label = { Text("HH") })
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = endMinute, onValueChange = { if(it.length <= 2) endMinute = it }, modifier = Modifier.width(70.dp), label = { Text("mm") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sH = startHour.toIntOrNull() ?: 18
                    val sM = startMinute.toIntOrNull() ?: 30
                    val eH = endHour.toIntOrNull() ?: 20
                    val eM = endMinute.toIntOrNull() ?: 0
                    
                    val startTime = LocalTime.of(sH, sM)
                    val endTime = LocalTime.of(eH, eM)
                    var duration = java.time.Duration.between(startTime, endTime).toMinutes().toInt()
                    if (duration < 0) duration += 24 * 60
                    
                    val schedules = selectedDays.map { day ->
                        TrainingSchedule(
                            teamId = "", 
                            dayOfWeek = day,
                            startTime = startTime,
                            durationMinutes = duration,
                            terrain = selectedTerrain
                        )
                    }
                    onConfirm(schedules)
                }, 
                enabled = selectedDays.isNotEmpty()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
