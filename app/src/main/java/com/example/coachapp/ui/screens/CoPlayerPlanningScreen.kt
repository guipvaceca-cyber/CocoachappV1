package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.coachapp.ui.player.PlayerViewModel
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoPlayerPlanningScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val myCollectifs by viewModel.myCollectifs.collectAsState()
    val selectedCollectifId by viewModel.selectedCollectifId.collectAsState()
    val currentPlanning by viewModel.currentPlanning.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var planningType by remember { mutableStateOf("TRAINING") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Mon Planning",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Retrouvez vos horaires et convocations.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SWITCH ENTRAINEMENT / MATCH ---
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = planningType == "TRAINING",
                onClick = { planningType = "TRAINING" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Entraînements")
            }
            SegmentedButton(
                selected = planningType == "MATCH",
                onClick = { planningType = "MATCH" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Matchs")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SWITCH COLLECTIF (Si plusieurs) ---
        if (myCollectifs.size > 1) {
            Text(
                text = "Équipe :",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(myCollectifs) { collectif ->
                    FilterChip(
                        selected = selectedCollectifId == collectif.id,
                        onClick = { viewModel.selectCollectif(collectif.id) },
                        label = { Text(collectif.nom) },
                        leadingIcon = {
                            if (selectedCollectifId == collectif.id) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- BANDEAU RÉCURSIVITÉ (Horaires habituels) ---
        if (planningType == "TRAINING") {
            val selectedTeam = myCollectifs.find { it.id == selectedCollectifId }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Horaires habituels - ${selectedTeam?.nom ?: ""}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else if (currentPlanning.isEmpty()) {
                        Text(
                            "Aucun horaire récurrent renseigné pour cette équipe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        // Group par heure si possible ou juste lister
                        currentPlanning.forEach { schedule ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.width(40.dp)
                                ) {
                                    Text(
                                        text = schedule.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH).uppercase(),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "${schedule.startTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "(${schedule.durationMinutes} min)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Placeholder pour les Matchs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Event, null, tint = Color.Gray)
                    Text("Consultez vos convocations de match ici.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ZONE CONVOCATIONS (Prochainement) ---
        Text(
            text = "Prochaines Convocations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Les séances et matchs programmés par vos coachs apparaîtront ici.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
