package com.example.coachapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import com.example.coachapp.ui.components.TacticalBoardOverlay
import com.example.coachapp.ui.util.PdfLauncher
import java.util.*

@Composable
fun ResourcesScreen(
    modifier: Modifier = Modifier,
    viewModel: com.example.coachapp.ui.CoachViewModel,
    onResourceClick: (LaboResource) -> Unit
) {
    var showCreateGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshLabo()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Le Labo des Coachs", 
            style = MaterialTheme.typography.headlineLarge, 
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        
        Spacer(Modifier.height(20.dp))

        // --- TABS ---
        TabRow(
            selectedTabIndex = viewModel.laboTab.ordinal, 
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = {}
        ) {
            Tab(
                selected = viewModel.laboTab == LaboTab.CORPUS, 
                onClick = { viewModel.laboTab = LaboTab.CORPUS },
                unselectedContentColor = Color.White.copy(alpha = 0.6f)
            ) {
                Text("Corpus Collaboratif", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(
                selected = viewModel.laboTab == LaboTab.EXTERNAL, 
                onClick = { viewModel.laboTab = LaboTab.EXTERNAL },
                unselectedContentColor = Color.White.copy(alpha = 0.6f)
            ) {
                Text("Ressources FFVB", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (viewModel.laboTab == LaboTab.CORPUS) {
            CorpusView(
                resources = viewModel.laboResources,
                onResourceClick = onResourceClick, 
                onCreateClick = { showCreateGuide = true }
            )
        } else {
            ExternalResourcesView()
        }
    }

    if (showCreateGuide) {
        CreateSituationGuide(
            viewModel = viewModel,
            onDismiss = { showCreateGuide = false }
        )
    }
}

@Composable
fun CorpusView(
    resources: List<LaboResource>,
    onResourceClick: (LaboResource) -> Unit, 
    onCreateClick: () -> Unit
) {
    Column {
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("PROPOSER UNE SITUATION", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(resources) { res ->
                LaboResourceCard(res, onClick = { onResourceClick(res) })
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun ExternalResourcesView() {
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- SECTION RÈGLEMENTS ---
        item {
            Text(
                "RÈGLEMENTS OFFICIELS", 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF00B4D8)
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            RuleCard(
                title = "RGED (Départemental)",
                subtitle = "Drôme-Ardèche - Édition 2024/2025",
                icon = Icons.Default.Gite,
                color = Color(0xFF4CAF50),
                onClick = { PdfLauncher.openLocalPdf(context, "rules/rged_26_07.pdf") }
            )
        }

        item {
            RuleCard(
                title = "RGER (Régional)",
                subtitle = "Ligue AURA - Édition 2024/2025",
                icon = Icons.Default.LocationCity,
                color = Color(0xFF2196F3),
                onClick = { PdfLauncher.openLocalPdf(context, "rules/rger_aura.pdf") }
            )
        }

        item {
            RuleCard(
                title = "FIVB (National/Intl)",
                subtitle = "Règles Officielles du Volley-Ball",
                icon = Icons.Default.Public,
                color = Color(0xFFF44336),
                onClick = { PdfLauncher.openRemotePdf(context, "https://www.ffvb.org/data/reglements/reglements-officiels/regles-du-jeu-du-volleyball-2021-2024.pdf") }
            )
        }

        // --- AUTRES RESSOURCES ---
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "AUTRES RESSOURCES", 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold, 
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            ExternalResourceRow(
                title = "Fiches Techniques FFVB",
                subtitle = "Fondamentaux par catégorie",
                onClick = { PdfLauncher.openRemotePdf(context, "https://www.ffvb.org/le-volley-ball/le-guide-du-volleyeur/technique/") }
            )
        }

        item {
            ExternalResourceRow(
                title = "Vidéos Pédagogiques",
                subtitle = "Gestuelles et placements FFVB",
                onClick = { PdfLauncher.openRemotePdf(context, "https://www.youtube.com/@FFvolleyball") }
            )
        }
    }
}

@Composable
fun RuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun ExternalResourceRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LibraryBooks, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun CreateSituationGuide(
    viewModel: com.example.coachapp.ui.CoachViewModel, 
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var showBoard by remember { mutableStateOf(false) }

    // Form State
    var title by remember { mutableStateOf("") }
    var intention by remember { mutableStateOf("") }
    var setupDescription by remember { mutableStateOf("") }
    var variables by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(LaboCategory.TECHNIQUE) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val persistenceManager = remember { PersistenceManager(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        containerColor = Color(0xFF002147).copy(alpha = 0.95f), // Very dark navy glass
        tonalElevation = 8.dp,
        title = { 
            Text(
                "CONCEVOIR UNE SITUATION", 
                fontWeight = FontWeight.Black, 
                color = Color.White,
                fontSize = 20.sp
            ) 
        },
        text = {
            Column(modifier = Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                LinearProgressIndicator(
                    progress = { step / 4f }, 
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF00B4D8),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(24.dp))

                when (step) {
                    1 -> {
                        GuideStepHeader("1. TITRE & CATÉGORIE")
                        GuideTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Nom de la situation..."
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Catégorie :", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        // Category Grid 2x2
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LaboCategory.entries.chunked(2).forEach { rowEntries ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowEntries.forEach { cat ->
                                        CategorySelectionCard(
                                            category = cat,
                                            isSelected = selectedCategory == cat,
                                            onSelect = { selectedCategory = cat },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        GuideStepHeader("INTENTION MAJEURE")
                        Text("Que voulez-vous que l'élève apprenne ? Timing, Posture, ou Prise d'info ?", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(12.dp))
                        GuideTextField(
                            value = intention,
                            onValueChange = { intention = it },
                            placeholder = "Ex: Maîtriser le pied de pivot en attaque"
                        )
                    }
                    2 -> {
                        GuideStepHeader("2. MISE EN PLACE")
                        Text("Détaillez le 'Quoi'. Placements, lanceurs, contraintes et circuit du ballon.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(16.dp))
                        GuideTextField(
                            value = setupDescription,
                            onValueChange = { setupDescription = it },
                            placeholder = "Description de la structure...", 
                            isLong = true
                        )
                    }
                    3 -> {
                        GuideStepHeader("3. VARIABLES DIDACTIQUES")
                        Text("Paliers de complexité : Comment simplifier si échec ? Comment complexifier si réussite ?", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(16.dp))
                        GuideTextField(
                            value = variables,
                            onValueChange = { variables = it },
                            placeholder = "Simplifications et complexifications...", 
                            isLong = true
                        )
                    }
                    4 -> {
                        GuideStepHeader("4. SUPPORT VISUEL")
                        Text("Un schéma clarifie le circuit. Utilisez l'ardoise tactique pour votre proposition.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { showBoard = true }, 
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Ouvrir l'ardoise tactique", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isReady = step == 4 && title.isNotBlank() && intention.isNotBlank() && setupDescription.isNotBlank()
            Button(
                onClick = { 
                    if (step < 4) {
                        step++ 
                    } else {
                        viewModel.proposeSituation(
                            title = title,
                            description = setupDescription,
                            focalPoints = listOf(FocalPoint("Principal", intention), FocalPoint("Variables", variables)),
                            category = selectedCategory,
                            onSuccess = { onDismiss() },
                            onError = { /* Log error if needed */ }
                        )
                    }
                },
                enabled = if (step == 4) isReady else true,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) { 
                Text(if (step < 4) "Suivant" else "Proposer au Labo", fontWeight = FontWeight.ExtraBold) 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Annuler", color = Color.White.copy(alpha = 0.6f)) 
            }
        }
    )

    TacticalBoardOverlay(isVisible = showBoard, onDismiss = { showBoard = false }, persistenceManager = persistenceManager)
}

@Composable
fun CategorySelectionCard(
    category: LaboCategory,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (category) {
        LaboCategory.TECHNIQUE -> Icons.Default.SportsVolleyball
        LaboCategory.TACTIQUE -> Icons.Default.GridView // More reliable if Schema is missing
        LaboCategory.PHYSIQUE -> Icons.Default.FitnessCenter
        LaboCategory.MENTAL -> Icons.Default.Psychology
    }

    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF00B4D8).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.label.split(" ").first(), // Short label or full depending on space
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GuideStepHeader(text: String) {
    Text(text, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00B4D8), fontSize = 14.sp, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun GuideTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String, 
    isLong: Boolean = false
) {
    OutlinedTextField(
        value = value, 
        onValueChange = onValueChange, 
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) }, 
        modifier = Modifier.fillMaxWidth().then(if (isLong) Modifier.height(120.dp) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00B4D8),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
fun LaboResourceCard(res: LaboResource, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.1f), 
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        res.category.label.uppercase(), 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.width(4.dp))
                    Text("v${res.versionsCount}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(res.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Text("Par ${res.authorNickname}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            
            Spacer(Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Science, null, modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(6.dp))
                        Text("${res.crashTests.size} CRASH-TESTS", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
