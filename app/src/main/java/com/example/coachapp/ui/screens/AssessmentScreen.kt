package com.example.coachapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coachapp.data.*
import com.example.coachapp.ui.components.QuestionItem
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
    
    var isRecording by remember { mutableStateOf(false) }
    var coachNote by remember { mutableStateOf("") }
    var isProcessingAI by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (type == AssessmentType.FLASH) "Diagnostic Flash" else "Bilan de Compétences",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = if (type == AssessmentType.FLASH) "Évaluation rapide de votre intervention terrain." else "Analyse globale de votre posture d'entraîneur.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            // --- VOICE NOTE SECTION (ONLY FOR FLASH) ---
            if (type == AssessmentType.FLASH) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🎙️ Débriefing Vocal IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            
                            if (isProcessingAI) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("L'IA analyse votre débriefing...", style = MaterialTheme.typography.labelSmall)
                            } else {
                                OutlinedTextField(
                                    value = coachNote,
                                    onValueChange = { coachNote = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    placeholder = { Text("Appuyez sur le micro pour parler...") },
                                    label = { Text("Synthèse & Ressenti") }
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    if (isRecording) {
                                        isRecording = false
                                        scope.launch {
                                            isProcessingAI = true
                                            delay(2000)
                                            coachNote = "SYNTHÈSE IA : Séance intense. Points forts : Engagement. À travailler : Précision des passes en transition."
                                            isProcessingAI = false
                                        }
                                    } else {
                                        isRecording = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isRecording) "Arrêter" else "Démarrer le débriefing")
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
                Spacer(modifier = Modifier.height(24.dp))
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = allComplete,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (allComplete) "Valider le diagnostic" else "Répondre à toutes les questions")
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DomainHeader(title: String, isExpanded: Boolean, isComplete: Boolean, progressText: String, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) Color(0xFFE8F5E9) else if (isExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                contentDescription = null,
                tint = if (isComplete) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(text = progressText, style = MaterialTheme.typography.labelSmall, color = if (isComplete) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
