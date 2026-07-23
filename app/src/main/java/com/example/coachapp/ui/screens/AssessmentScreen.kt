package com.example.coachapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coachapp.data.*
import com.example.coachapp.ui.components.QuestionItem
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AssessmentScreen(
    modifier: Modifier = Modifier,
    type: AssessmentType = AssessmentType.FLASH,
    onResultCalculated: (AssessmentType, Map<String, Double>, String?) -> Unit = { _, _, _ -> }
) {
    val diagnosticData = if (type == AssessmentType.FLASH) flashDiagnosticData else globalDiagnosticData
    val scores = remember(type) { mutableStateMapOf<String, Int>() }
    var expandedDomainId by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val voiceManager = remember { LocalVoiceManager(context) }
    val isRecording by voiceManager.isRecording.collectAsState()
    val partialText by voiceManager.partialText.collectAsState()
    
    var coachNote by remember { mutableStateOf("") }

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

    LaunchedEffect(partialText) {
        if (partialText.isNotEmpty()) {
            coachNote = partialText
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF001529))
    ) {
        // Decorative Blur Blobs
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = 100.dp)
                .size(200.dp)
                .background(Color(0xFF9C27B0).copy(alpha = 0.12f), CircleShape)
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .size(250.dp)
                .background(Color(0xFF00B4D8).copy(alpha = 0.15f), CircleShape)
                .blur(80.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = if (type == AssessmentType.FLASH) "Diagnostic Flash" else "Bilan de Compétences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = if (type == AssessmentType.FLASH) "Évaluation rapide de votre intervention terrain." else "Analyse globale de votre posture d'entraîneur.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                // --- VOICE NOTE SECTION (ONLY FOR FLASH) ---
                if (type == AssessmentType.FLASH) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            border = BorderStroke(0.5.dp, if (isRecording) Color.Red else Color.White.copy(alpha = 0.15f))
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
                                    Icon(
                                        Icons.Default.Mic, 
                                        null, 
                                        tint = if (isRecording) Color.Red.copy(alpha = pulseAlpha) else Color(0xFF00B4D8)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        if (isRecording) "ENREGISTREMENT EN COURS..." else "DÉBRIEFING VOCAL", 
                                        style = MaterialTheme.typography.titleSmall, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRecording) Color.Red else Color.White
                                    )
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                OutlinedTextField(
                                    value = coachNote,
                                    onValueChange = { coachNote = it },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    placeholder = { Text("Votre ressenti à voix haute...", color = Color.White.copy(alpha = 0.3f)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00B4D8),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        if (hasAudioPermission) {
                                            if (isRecording) {
                                                voiceManager.stopListeningAndRecording()
                                            } else {
                                                voiceManager.startListeningAndRecording(onlyTranscription = true)
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRecording) Color.Red else Color.White,
                                        contentColor = if (isRecording) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isRecording) "Terminer" else "Commencer à parler")
                                }
                            }
                        }
                    }
                }

                diagnosticData.forEach { domain ->
                    val isExpanded = expandedDomainId == domain.id
                    val answeredCount = domain.questions.count { scores.containsKey(it.id) }
                    val isComplete = answeredCount == domain.questions.size
                    
                    item {
                        DomainHeader(
                            title = domain.title,
                            isExpanded = isExpanded,
                            isComplete = isComplete,
                            progressText = "$answeredCount / ${domain.questions.size}",
                            onToggle = { expandedDomainId = if (isExpanded) null else domain.id }
                        )
                    }
                    
                    if (isExpanded) {
                        items(domain.questions) { question ->
                            QuestionItem(
                                text = question.text,
                                currentScore = scores[question.id] ?: 0,
                                onScoreSelected = { scores[question.id] = it }
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    val totalQuestions = diagnosticData.sumOf { it.questions.size }
                    val totalAnswered = scores.size
                    val allComplete = totalAnswered == totalQuestions

                    Button(
                        onClick = {
                            val results = diagnosticData.associate { domain ->
                                val domainScores = domain.questions.map { scores[it.id] ?: 0 }
                                val average = if (domainScores.isNotEmpty()) domainScores.average() else 0.0
                                domain.id to average
                            }
                            onResultCalculated(type, results, coachNote)
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        enabled = allComplete,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (allComplete) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.1f),
                            contentColor = if (allComplete) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            if (allComplete) "VALIDER MON BILAN" else "RÉPONDRE À TOUT ($totalAnswered/$totalQuestions)",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun DomainHeader(title: String, isExpanded: Boolean, isComplete: Boolean, progressText: String, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) Color(0xFF4CAF50).copy(alpha = 0.15f) 
                           else if (isExpanded) Color.White.copy(alpha = 0.12f) 
                           else Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(
            0.5.dp, 
            if (isComplete) Color(0xFF4CAF50).copy(alpha = 0.4f) 
            else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isComplete) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = progressText, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                color = if (isComplete) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
