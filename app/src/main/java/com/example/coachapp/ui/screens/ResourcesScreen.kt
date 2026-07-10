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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import com.example.coachapp.ui.components.TacticalBoardOverlay
import java.util.*

@Composable
fun ResourcesScreen(
    modifier: Modifier = Modifier,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    onResourceClick: (LaboResource) -> Unit
) {
    var showCreateGuide by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Le Labo des Coachs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        
        Spacer(Modifier.height(16.dp))

        // --- TABS ---
        TabRow(selectedTabIndex = viewModel.laboTab.ordinal, containerColor = Color.Transparent) {
            Tab(selected = viewModel.laboTab == LaboTab.CORPUS, onClick = { viewModel.laboTab = LaboTab.CORPUS }) {
                Text("Corpus Collaboratif", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = viewModel.laboTab == LaboTab.EXTERNAL, onClick = { viewModel.laboTab = LaboTab.EXTERNAL }) {
                Text("Ressources FFVB", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        if (viewModel.laboTab == LaboTab.CORPUS) {
            CorpusView(onResourceClick, onCreateClick = { showCreateGuide = true })
        } else {
            ExternalResourcesView()
        }
    }

    if (showCreateGuide) {
        CreateSituationGuide(
            persistenceManager = PersistenceManager(androidx.compose.ui.platform.LocalContext.current),
            onDismiss = { showCreateGuide = false }
        )
    }
}

@Composable
fun CorpusView(onResourceClick: (LaboResource) -> Unit, onCreateClick: () -> Unit) {
    Column {
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("PROPOSER UNE SITUATION (GUIDE)", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(laboCorpus) { res ->
                LaboResourceCard(res, onClick = { onResourceClick(res) })
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun ExternalResourcesView() {
    val externalMocks = listOf(
        "Fiches Techniques FFVB (PDF)" to "Ensemble des fondamentaux par catégorie.",
        "Vidéos Pédagogiques Volley" to "Gestuelles et placements en mouvement.",
        "Règlements Officiels" to "Mise à jour des règles de jeu 2024/2025."
    )
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(externalMocks) { item ->
            Card(modifier = Modifier.fillMaxWidth().clickable { /* Link to external */ }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryBooks, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(item.first, fontWeight = FontWeight.Bold)
                        Text(item.second, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CreateSituationGuide(persistenceManager: PersistenceManager, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var showBoard by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Laboratoire : Concevoir une situation", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.height(400.dp).verticalScroll(rememberScrollState())) {
                LinearProgressIndicator(progress = { step / 4f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))

                when (step) {
                    1 -> {
                        Text("1. L'INTENTION MAJEURE", fontWeight = FontWeight.Bold)
                        Text("Que voulez-vous que l'élève apprenne ? Ne cherchez pas la complexité, cherchez l'angle (Timing, Posture, ou Prise d'info ?).", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Ex: Maîtriser le pied de pivot en attaque") }, modifier = Modifier.fillMaxWidth())
                    }
                    2 -> {
                        Text("2. DÉCONSTRUIRE LA MISE EN PLACE", fontWeight = FontWeight.Bold)
                        Text("Détaillez le 'Quoi'. Comment se placent les joueurs ? Qui lance le ballon ? Quelles sont les contraintes ?", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth().height(100.dp))
                    }
                    3 -> {
                        Text("3. DISSÉQUER LES ÉTAPES", fontWeight = FontWeight.Bold)
                        Text("Prévoyez des paliers : Comment rendre l'exercice plus simple s'ils échouent ? Comment le complexifier s'ils réussissent trop vite ?", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth().height(100.dp))
                    }
                    4 -> {
                        Text("4. SUPPORT VISUEL", fontWeight = FontWeight.Bold)
                        Text("Un schéma vaut mille mots. Utilisez l'ardoise pour clarifier le circuit du ballon et le placement.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showBoard = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ouvrir l'ardoise tactique")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step < 4) {
                Button(onClick = { step++ }) { Text("Étape suivante") }
            } else {
                Button(onClick = onDismiss) { Text("Finaliser ma proposition") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )

    TacticalBoardOverlay(isVisible = showBoard, onDismiss = { showBoard = false }, persistenceManager = persistenceManager)
}

@Composable
fun LaboResourceCard(res: LaboResource, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(res.category.label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(" v${res.versionsCount}", fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(12.dp))
            Text(res.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Par ${res.authorNickname}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            val successCount = res.crashTests.count { it.result == "Succès" }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Science, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                Text("${res.crashTests.size} tests terrain", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}
