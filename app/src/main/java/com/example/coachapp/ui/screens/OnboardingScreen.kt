package com.example.coachapp.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import java.util.*

@Composable
fun OnboardingScreen(
    initialConfig: SeasonConfig,
    pendingInvitations: List<Team> = emptyList(),
    onCompleted: (SeasonConfig) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var config by remember { mutableStateOf(initialConfig) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { step / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 24.dp),
                strokeCap = StrokeCap.Round
            )

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "StepTransition"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> IdentityStep(config) { updated -> config = updated; step = 2 }
                        2 -> ProfessionalStep(config) { updated -> config = updated; step = 3 }
                        3 -> PersonaStep(config) { updated -> config = updated; step = 4 }
                        4 -> TeamsStep(config, pendingInvitations) { updated -> config = updated; step = 5 }
                        5 -> FinalStep(config) { onCompleted(config.copy(isOnboardingCompleted = true)) }
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityStep(config: SeasonConfig, onNext: (SeasonConfig) -> Unit) {
    var firstName by remember { mutableStateOf(config.coachProfile.firstName) }
    var lastName by remember { mutableStateOf(config.coachProfile.lastName) }
    var nickname by remember { mutableStateOf(config.coachProfile.nickname) }
    var clubName by remember { mutableStateOf(config.coachProfile.clubName) }
    var showPhotoPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        
        Box(modifier = Modifier.size(100.dp).clickable { showPhotoPicker = true }, contentAlignment = Alignment.BottomEnd) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.White) }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text("Bienvenue Coach !", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Créez votre carte d'identité.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Surnom (ex: Le Boss, Coach...)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = clubName, onValueChange = { clubName = it }, label = { Text("Votre Club") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.SportsVolleyball, null) })
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { onNext(config.copy(coachProfile = config.coachProfile.copy(firstName = firstName, lastName = lastName, nickname = nickname, clubName = clubName))) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = firstName.isNotBlank() && lastName.isNotBlank() && clubName.isNotBlank()
        ) {
            Text("Continuer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun ProfessionalStep(config: SeasonConfig, onNext: (SeasonConfig) -> Unit) {
    val levels = listOf("Novice", "AFJ", "DRE1", "ER", "DRE2", "EF", "EN", "Master Coach")
    var selectedLevel by remember { mutableStateOf(config.coachProfile.formationLevel) }
    var goalPersonal by remember { mutableStateOf(config.coachProfile.goalPersonal) }
    var goalCollective by remember { mutableStateOf(config.coachProfile.goalCollective) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text("Votre Parcours", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Définissez vos ambitions.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Diplôme actuel :", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            Column {
                levels.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { level ->
                            FilterChip(
                                selected = selectedLevel == level,
                                onClick = { selectedLevel = level },
                                label = { Text(level, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size < 3) Spacer(Modifier.weight((3 - row.size).toFloat()))
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Text("Projets de saison :", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = goalPersonal, onValueChange = { goalPersonal = it }, label = { Text("Objectif personnel") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = goalCollective, onValueChange = { goalCollective = it }, label = { Text("Objectif collectif") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onNext(config.copy(coachProfile = config.coachProfile.copy(formationLevel = selectedLevel, goalPersonal = goalPersonal, goalCollective = goalCollective))) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Suivant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun PersonaStep(config: SeasonConfig, onNext: (SeasonConfig) -> Unit) {
    var selectedPersona by remember { mutableStateOf(config.coachProfile.coachPersona) }
    val personas = listOf(
        "Le Tacticien" to "Analyse, schémas et stratégies.",
        "Le Pédagogue" to "Apprentissage, écoute et patience.",
        "Le Leader" to "Énergie, motivation et mental."
    )

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text("Quel Coach êtes-vous ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Choisissez votre profil dominant.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(Modifier.height(48.dp))

        Column(modifier = Modifier.weight(1f)) {
            personas.forEach { (title, desc) ->
                val isSelected = selectedPersona == title
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedPersona = title },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = { selectedPersona = title })
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onNext(config.copy(coachProfile = config.coachProfile.copy(coachPersona = selectedPersona))) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedPersona.isNotBlank()
        ) {
            Text("Continuer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun TeamsStep(config: SeasonConfig, pendingInvitations: List<Team>, onNext: (SeasonConfig) -> Unit) {
    var teams by remember { mutableStateOf(config.teams) }
    var showAddTeam by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(32.dp))
        Text("Vos Collectifs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Quels groupes encadrez-vous ?", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            // --- SECTION INVITATIONS ---
            if (pendingInvitations.isNotEmpty()) {
                item {
                    Text(
                        "INVITATIONS DE VOTRE CLUB",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(pendingInvitations) { team ->
                    val isAlreadyAdded = teams.any { it.id == team.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isAlreadyAdded) { teams = teams + team },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAlreadyAdded) Color(0xFFE1F5EE) 
                                             else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        border = if (!isAlreadyAdded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isAlreadyAdded) Icons.Default.CheckCircle else Icons.Default.AddCircle, null, tint = if (isAlreadyAdded) Color(0xFF085041) else MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(team.name, fontWeight = FontWeight.Bold)
                                Text("Assignation officielle • ${team.format.label}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!isAlreadyAdded) {
                                Text("AJOUTER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            // --- SECTION VOS ÉQUIPES ---
            item {
                Text(
                    "VOS GROUPES",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(teams) { team ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = team.color.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = team.color) {}
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(team.name, fontWeight = FontWeight.Bold)
                            Text("${team.format.label} • ${team.objective}", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { teams = teams.filter { it.id != team.id } }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showAddTeam = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Ajouter une catégorie")
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = { onNext(config.copy(teams = teams)) }, modifier = Modifier.fillMaxWidth().height(64.dp), enabled = teams.isNotEmpty(), shape = RoundedCornerShape(16.dp)) {
            Text("Continuer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }

    if (showAddTeam) {
        AddTeamOnboardingDialog(onDismiss = { showAddTeam = false }, onConfirm = { team -> teams = teams + team; showAddTeam = false })
    }
}

@Composable
fun FinalStep(config: SeasonConfig, onFinish: () -> Unit) {
    val coachName = if (config.coachProfile.nickname.isNotBlank()) config.coachProfile.nickname else config.coachProfile.firstName
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(80.dp)) }
        }
        Spacer(Modifier.height(32.dp))
        Text("C'est prêt, Coach $coachName !", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("Votre environnement CoCoach est configuré pour le club ${config.coachProfile.clubName}.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        Spacer(Modifier.height(64.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) {
            Text("Coup d'envoi !", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
    }
}

@Composable
fun AddTeamOnboardingDialog(onDismiss: () -> Unit, onConfirm: (Team) -> Unit) {
    var name by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var projectType by remember { mutableStateOf("Développement") }
    var selectedFormat by remember { mutableStateOf(TeamFormat.SIX_SIX) }
    val colors = listOf(Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0))
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Équipe") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom (M13, M15...)") })
                Spacer(Modifier.height(8.dp))
                Text("Format de jeu :", style = MaterialTheme.typography.labelSmall)
                TeamFormat.entries.forEach { format ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedFormat = format }) {
                        RadioButton(selected = selectedFormat == format, onClick = { selectedFormat = format })
                        Text(format.label)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("Objectif (ex: Top 10 France)") })
                Spacer(Modifier.height(16.dp))
                Text("Type de projet :", style = MaterialTheme.typography.labelSmall)
                Row {
                    listOf("Élite", "Développement", "Loisir").forEach { type ->
                        FilterChip(selected = projectType == type, onClick = { projectType = type }, label = { Text(type) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { color ->
                        Box(modifier = Modifier.size(32.dp).background(color, CircleShape).border(if (selectedColor == color) 2.dp else 0.dp, Color.Black, CircleShape).clickable { selectedColor = color })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(Team(UUID.randomUUID().toString(), name, selectedColor, projectType, objective, selectedFormat)) }, enabled = name.isNotBlank()) {
                Text("Ajouter")
            }
        }
    )
}
