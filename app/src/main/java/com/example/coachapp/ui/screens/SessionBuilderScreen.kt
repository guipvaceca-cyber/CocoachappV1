package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import com.example.coachapp.data.LocalVoiceManager
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBuilderScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig,
    initialSessionId: String? = null,
    onUpdateSession: (TrainingSession) -> Unit,
    onPushSession: (TrainingSession) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val sortedSessions = remember(seasonConfig.plannedTrainings) {
        seasonConfig.plannedTrainings.sortedBy { it.date }
    }
    
    var selectedSessionId by remember { mutableStateOf(initialSessionId ?: sortedSessions.firstOrNull()?.id) }
    
    LaunchedEffect(initialSessionId) {
        if (initialSessionId != null) {
            selectedSessionId = initialSessionId
        }
    }
    val session = remember(selectedSessionId, seasonConfig.plannedTrainings) {
        seasonConfig.plannedTrainings.find { it.id == selectedSessionId }
    }

    val context = LocalContext.current
    val voiceManager = remember { LocalVoiceManager(context) }
    val isRecordingGlobal by voiceManager.isRecording.collectAsState()
    val partialTextGlobal by voiceManager.partialText.collectAsState()
    var activeRecordingLabel by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF001529),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                    Text(
                        "PRÉPARATEUR",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            if (sortedSessions.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dateStr = session?.date?.format(DateTimeFormatter.ofPattern("dd/MM")) ?: "Choisir une séance"
                            val teamName = seasonConfig.teams.find { it.id == session?.teamId }?.name ?: ""
                            Column {
                                Text("$dateStr - $teamName", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                                Text(session?.focusArea ?: "Thème non défini", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f).background(Color(0xFF001529)).border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        sortedSessions.forEach { s ->
                            val team = seasonConfig.teams.find { it.id == s.teamId }
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text("${s.date.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${team?.name}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B4D8))
                                        Text(s.focusArea ?: "Sans thème", color = Color.White)
                                    }
                                },
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
                        Text("MES INTENTIONS", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00B4D8), fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(12.dp))
                        
                        IntentionCard(
                            title = "Intention d'Entraîneur",
                            icon = Icons.Default.Search,
                            subtitle = "Aspect technique prioritaire",
                            value = session.trainerIntentions,
                            onValueChange = { onUpdateSession(session.copy(trainerIntentions = it)) }
                        )
                        
                        IntentionCard(
                            title = "Intention de Coach",
                            icon = Icons.Default.Psychology,
                            subtitle = "Pédagogie et comportement",
                            value = session.coachIntentions,
                            onValueChange = { onUpdateSession(session.copy(coachIntentions = it)) }
                        )
                        
                        Spacer(Modifier.height(24.dp))
                    }

                    if (session.noteForFutureMe.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE67E22).copy(alpha = 0.1f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE67E22).copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFE67E22))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("NOTE DE LA DERNIÈRE FOIS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFFE67E22))
                                        Text(session.noteForFutureMe, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("STRUCTURE & TIMING", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00B4D8), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), letterSpacing = 1.2.sp)
                            Surface(
                                color = if (isOverTime) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = if (isOverTime) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)) else null
                            ) {
                                Text(
                                    "${totalPlanned} / ${session.durationMinutes} min", 
                                    style = MaterialTheme.typography.labelMedium, 
                                    color = if (isOverTime) Color(0xFFD32F2F) else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isOverTime) {
                            Text("Attention : le cumul dépasse la durée prévue", color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    item { 
                        PhaseCard(
                            label = "Échauffement", 
                            icon = Icons.AutoMirrored.Filled.DirectionsRun, 
                            text = session.warmup, 
                            duration = session.warmupDuration,
                            isRecording = isRecordingGlobal && activeRecordingLabel == "Échauffement",
                            partialText = if (activeRecordingLabel == "Échauffement") partialTextGlobal else "",
                            onToggleRecording = {
                                if (isRecordingGlobal) {
                                    voiceManager.stopListeningAndRecording()
                                    activeRecordingLabel = null
                                } else {
                                    activeRecordingLabel = "Échauffement"
                                    voiceManager.startListeningAndRecording()
                                }
                            },
                            onUpdate = { text, dur -> onUpdateSession(session.copy(warmup = text, warmupDuration = dur)) }
                        ) 
                    }
                    item { 
                        PhaseCard(
                            label = "Gammes", 
                            icon = Icons.Default.TrackChanges, 
                            text = session.drills, 
                            duration = session.drillsDuration,
                            isRecording = isRecordingGlobal && activeRecordingLabel == "Gammes",
                            partialText = if (activeRecordingLabel == "Gammes") partialTextGlobal else "",
                            onToggleRecording = {
                                if (isRecordingGlobal) {
                                    voiceManager.stopListeningAndRecording()
                                    activeRecordingLabel = null
                                } else {
                                    activeRecordingLabel = "Gammes"
                                    voiceManager.startListeningAndRecording()
                                }
                            },
                            onUpdate = { text, dur -> onUpdateSession(session.copy(drills = text, drillsDuration = dur)) }
                        ) 
                    }
                    item { 
                        PhaseCard(
                            label = "Situations réduites", 
                            icon = Icons.Default.Groups, 
                            text = session.smallGroupSituations, 
                            duration = session.smallGroupDuration,
                            isRecording = isRecordingGlobal && activeRecordingLabel == "Situations réduites",
                            partialText = if (activeRecordingLabel == "Situations réduites") partialTextGlobal else "",
                            onToggleRecording = {
                                if (isRecordingGlobal) {
                                    voiceManager.stopListeningAndRecording()
                                    activeRecordingLabel = null
                                } else {
                                    activeRecordingLabel = "Situations réduites"
                                    voiceManager.startListeningAndRecording()
                                }
                            },
                            onUpdate = { text, dur -> onUpdateSession(session.copy(smallGroupSituations = text, smallGroupDuration = dur)) }
                        ) 
                    }
                    item { 
                        PhaseCard(
                            label = "Jeu collectif", 
                            icon = Icons.Default.SportsVolleyball, 
                            text = session.collectiveGame, 
                            duration = session.collectiveDuration,
                            isRecording = isRecordingGlobal && activeRecordingLabel == "Jeu collectif",
                            partialText = if (activeRecordingLabel == "Jeu collectif") partialTextGlobal else "",
                            onToggleRecording = {
                                if (isRecordingGlobal) {
                                    voiceManager.stopListeningAndRecording()
                                    activeRecordingLabel = null
                                } else {
                                    activeRecordingLabel = "Jeu collectif"
                                    voiceManager.startListeningAndRecording()
                                }
                            },
                            onUpdate = { text, dur -> onUpdateSession(session.copy(collectiveGame = text, collectiveDuration = dur)) }
                        ) 
                    }
                    
                    item {
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { 
                                val updated = session.copy(isValidated = true)
                                onUpdateSession(updated)
                                onPushSession(updated)
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (session.isValidated) Color(0xFF388E3C) else Color(0xFF00B4D8),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(if (session.isValidated) Icons.Default.CloudDone else Icons.Default.CloudUpload, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (session.isValidated) "SÉANCE SYNCHRONISÉE" else "VALIDER & PUSH CLOUD", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IntentionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String, value: String, onValueChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(start = 28.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().height(90.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                placeholder = { Text("Ex: Placement des pieds, communication...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00B4D8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun PhaseCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    duration: Int,
    isRecording: Boolean,
    partialText: String,
    onToggleRecording: () -> Unit,
    onUpdate: (String, Int) -> Unit
) {
    val context = LocalContext.current
    var baseText by remember { mutableStateOf("") }

    LaunchedEffect(partialText) {
        if (isRecording && partialText.isNotBlank()) {
            val combined = if (baseText.isBlank()) partialText else "$baseText\n$partialText"
            onUpdate(combined, duration)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            baseText = text
            onToggleRecording()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp
                )
                // Contrôle durée
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 4.dp)
                ) {
                    IconButton(
                        onClick = { if (duration > 5) onUpdate(text, duration - 5) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(
                        "$duration'",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.Black
                    )
                    IconButton(
                        onClick = { onUpdate(text, duration + 5) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { onUpdate(it, duration) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                placeholder = { Text("Détail de l'exercice...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00B4D8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.Transparent
                ),
                trailingIcon = {
                    if (isRecording) {
                        IconButton(onClick = onToggleRecording) {
                            Icon(
                                Icons.Default.Stop,
                                null,
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                baseText = text
                                onToggleRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                null,
                                tint = Color(0xFF00B4D8)
                            )
                        }
                    }
                }
            )

            // Indicateur d'enregistrement
            if (isRecording) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFD32F2F).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFD32F2F), CircleShape)
                    )
                    Text(
                        "Enregistrement vocal actif...",
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
