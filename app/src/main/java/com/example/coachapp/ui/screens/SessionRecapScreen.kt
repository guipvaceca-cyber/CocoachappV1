package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coachapp.data.PersistenceManager
import com.example.coachapp.data.TrainingSession
import com.example.coachapp.ui.components.AnalyseSeanceDrawer
import com.example.coachapp.ui.components.RadarChart
import com.example.coachapp.ui.training.AnalyseSeanceViewModel
import io.github.jan.supabase.SupabaseClient
import java.time.format.DateTimeFormatter

@Composable
fun SessionRecapScreen(
    session: TrainingSession,
    persistenceManager: PersistenceManager,
    coachId: String,
    supabase: SupabaseClient,
    onBack: () -> Unit,
    onRepeat: () -> Unit,
    onExportToLabo: () -> Unit,
    onUpdateSession: (TrainingSession) -> Unit
) {
    var futureNote by remember { mutableStateOf(session.noteForFutureMe) }
    val mockScores = remember { mapOf("pedagogie_flash" to 4.2, "rythme_flash" to 3.8, "climat_flash" to 4.5, "resultat_flash" to 3.5) }

    // ---- AJOUT : ViewModel analyse ----
    val analyseVm: AnalyseSeanceViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AnalyseSeanceViewModel(supabase, coachId) as T
            }
        }
    )
    val showDrawer by analyseVm.showDrawer.collectAsState()
    val analyseState by analyseVm.uiState.collectAsState()
    // ---- FIN AJOUT ----

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopAppBar(
                title = { Text("Bilan de Séance", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                )
            )

            LazyColumn(modifier = Modifier.padding(16.dp).weight(1f)) {
                item {
                    Text(
                        text = "${session.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))} • ${session.startTime}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = session.focusArea ?: "Thème Général",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PERFORMANCE FLASH", fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                                RadarChart(flashScores = mockScores, globalScores = null, modifier = Modifier.size(180.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Text("VÉCU TERRAIN (NOTES À CHAUD)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = session.liveFeedback.ifEmpty { "Aucune note prise pendant la séance." },
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Text("💡 NOTE POUR MON FUTUR MOI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE67E22))
                    Text("Ce conseil apparaîtra quand vous réitérerez cette séance.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    OutlinedTextField(
                        value = futureNote,
                        onValueChange = {
                            futureNote = it
                            onUpdateSession(session.copy(noteForFutureMe = it))
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text("Ex: Réduire la durée de l'exercice 2, trop cardio...") },
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    // ---- AJOUT : déclenchement analyse à la clôture ----
                    Button(
                        onClick = {
                            analyseVm.onSeanceClôturée(session.id)
                            onRepeat()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("RÉITÉRER CETTE SÉANCE", fontWeight = FontWeight.Bold)
                    }
                    // ---- FIN AJOUT ----

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onExportToLabo,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Science, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PARTAGER AU LABO DES COACHS")
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        // ---- AJOUT : drawer analyse en overlay ----
        if (showDrawer) {
            AnalyseSeanceDrawer(
                state = analyseState,
                onDismiss = { analyseVm.dismissDrawer() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // ---- FIN AJOUT ----
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    colors: TopAppBarColors,
) {
    CenterAlignedTopAppBar(title = title, navigationIcon = navigationIcon, colors = colors)
}