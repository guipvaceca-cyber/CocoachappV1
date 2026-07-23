package com.example.coachapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.coachapp.data.LocalVoiceManager
import com.example.coachapp.data.PersistenceManager
import com.example.coachapp.data.SeasonConfig
import com.example.coachapp.data.TrainingSession
import com.example.coachapp.ui.components.TacticalBoardOverlay
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun SessionCompanionScreen(
    modifier: Modifier = Modifier,
    session: TrainingSession,
    teamName: String,
    persistenceManager: PersistenceManager,
    onUpdateSession: (TrainingSession) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onPushSession: (TrainingSession) -> Unit = {}
) {
    var activePhaseIndex by remember { mutableIntStateOf(0) }
    var timeLeftSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showTacticalBoard by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH) }
    
    val phases = remember(session) {
        listOf(
            Triple("🤸 Échauffement", session.warmup, session.warmupDuration),
            Triple("🎯 Gammes", session.drills, session.drillsDuration),
            Triple("👥 Situations réduites", session.smallGroupSituations, session.smallGroupDuration),
            Triple("🎮 Jeu collectif", session.collectiveGame, session.collectiveDuration)
        )
    }

    val isLastPhase = activePhaseIndex == phases.size - 1

    LaunchedEffect(activePhaseIndex) {
        timeLeftSeconds = phases[activePhaseIndex].third * 60
        isTimerRunning = false
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timeLeftSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeftSeconds--
            if (timeLeftSeconds == 0) isTimerRunning = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val voiceManager = remember { LocalVoiceManager(context) }
    val isRecording by voiceManager.isRecording.collectAsState()
    val partialText by voiceManager.partialText.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFF001529))) {
                // --- TOP BAR ---
                Surface(
                    modifier = Modifier.fillMaxWidth(), 
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.date.format(dateFormatter).uppercase(),
                                color = Color(0xFF00B4D8),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$teamName : ${session.focusArea ?: "Général"}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (isLastPhase) {
                            Button(
                                onClick = { 
                                    onPushSession(session)
                                    onFinish() 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text("FINIR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Scoreur shortcut
                        IconButton(onClick = { showTacticalBoard = true }) {
                            Icon(Icons.Default.MenuBook, null, tint = Color(0xFF00B4D8))
                        }
                    }
                }
                
                TacticalBoardOverlay(
                    isVisible = showTacticalBoard,
                    onDismiss = { showTacticalBoard = false },
                    persistenceManager = persistenceManager
                )

                Row(modifier = Modifier.weight(1f)) {
                    // --- LEFT TIMELINE ---
                    Column(modifier = Modifier.width(70.dp).fillMaxHeight().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        phases.forEachIndexed { index, _ ->
                            val isActive = index == activePhaseIndex
                            val isPast = index < activePhaseIndex
                            
                            Surface(
                                modifier = Modifier.size(36.dp).clickable { activePhaseIndex = index },
                                shape = CircleShape,
                                color = if (isActive) Color(0xFF00B4D8) else if (isPast) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                                border = if (isActive) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isPast) Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = Color(0xFF001529))
                                    else Text("${index + 1}", color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index < phases.size - 1) {
                                Box(modifier = Modifier.width(2.dp).height(60.dp).background(if (isPast) Color(0xFF00B4D8).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)))
                            }
                        }
                        
                        // Final Checkmark icon
                        if (isLastPhase && timeLeftSeconds == 0) {
                            IconButton(onClick = onFinish) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
                            }
                        }
                    }

                    // --- MAIN CONTENT (ACTIVE PHASE) ---
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        val current = phases[activePhaseIndex]
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(24.dp), 
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(current.first, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = current.second.ifEmpty { "Aucun détail saisi." }, 
                                    textAlign = TextAlign.Center, 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                
                                Spacer(Modifier.height(32.dp))

                                // --- TIMER CIRCLE ---
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                                    CircularProgressIndicator(
                                        progress = { timeLeftSeconds.toFloat() / (current.third * 60) },
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 10.dp,
                                        color = if (isLastPhase && timeLeftSeconds == 0) Color(0xFF4CAF50) else Color(0xFF00B4D8),
                                        trackColor = Color.White.copy(alpha = 0.1f),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    val mins = timeLeftSeconds / 60
                                    val secs = timeLeftSeconds % 60
                                    Text(
                                        text = "%02d:%02d".format(mins, secs), 
                                        style = MaterialTheme.typography.displayMedium, 
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                Spacer(Modifier.height(32.dp))

                                // --- CONTROLS ---
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                    IconButton(
                                        onClick = { timeLeftSeconds += 300 }, 
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White)
                                    }
                                    
                                    FloatingActionButton(
                                        onClick = { isTimerRunning = !isTimerRunning },
                                        containerColor = if (isTimerRunning) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                        contentColor = Color.White,
                                        shape = CircleShape,
                                        modifier = Modifier.size(72.dp),
                                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                                    ) {
                                        Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, modifier = Modifier.size(36.dp), contentDescription = null)
                                    }

                                    IconButton(
                                        onClick = { 
                                            if (activePhaseIndex < phases.size - 1) activePhaseIndex++ 
                                            else onFinish() // Finish on last skip
                                        }, 
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(if (isLastPhase) Icons.Default.DoneAll else Icons.Default.SkipNext, null, tint = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // --- LIVE FEEDBACK ---
                        LiveFeedbackInput(
                            currentValue = session.liveFeedback,
                            isRecording = isRecording,
                            partialText = partialText,
                            scrollState = scrollState,
                            onUpdate = { onUpdateSession(session.copy(liveFeedback = it)) },
                            onToggleVoice = {
                                if (hasAudioPermission) {
                                    try {
                                        if (isRecording) {
                                            voiceManager.stopListeningAndRecording()
                                        } else {
                                            voiceManager.startListeningAndRecording(onlyTranscription = true)
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Erreur micro: ${e.message}")
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveFeedbackInput(
    currentValue: String,
    isRecording: Boolean,
    partialText: String,
    scrollState: androidx.compose.foundation.ScrollState,
    onUpdate: (String) -> Unit,
    onToggleVoice: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(partialText) {
        if (partialText.isNotEmpty()) {
            text = partialText
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(20.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(0.5.dp, if (isRecording) Color(0xFFD32F2F) else Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier.size(32.dp).background(
                        if (isRecording) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        Icons.Default.Mic, 
                        null, 
                        modifier = Modifier.size(18.dp), 
                        tint = if (isRecording) Color(0xFFD32F2F).copy(alpha = pulseAlpha) else Color(0xFF00B4D8)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isRecording) "ENREGISTREMENT..." else "NOTES À CHAUD", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isRecording) Color(0xFFD32F2F) else Color(0xFF00B4D8)
                )
            }
            if (currentValue.isNotEmpty()) {
                Text(currentValue, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 8.dp))
            }
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(if (isRecording) "Parlez..." else "Tapez une observation...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    // Scroll to the very bottom when keyboard appears
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = if (isRecording) Color(0xFFD32F2F) else Color(0xFF00B4D8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                IconButton(
                    onClick = { 
                        val update = if (currentValue.isEmpty()) text else "$currentValue | $text"
                        onUpdate(update)
                        text = ""
                    }, 
                    enabled = text.isNotBlank() && !isRecording,
                    modifier = Modifier.background(
                        if(text.isNotBlank() && !isRecording) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.1f), 
                        CircleShape
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = if(text.isNotBlank() && !isRecording) Color.White else Color.White.copy(alpha = 0.4f))
                }
            }
        }
    }
}
