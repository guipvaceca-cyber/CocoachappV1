package com.example.coachapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.compose.AsyncImage
import com.example.coachapp.R
import com.example.coachapp.data.*
import java.util.*

@Composable
fun OnboardingScreen(
    initialConfig: SeasonConfig,
    pendingInvitations: List<Team> = emptyList(),
    onCompleted: (SeasonConfig, List<String>) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var config by remember { mutableStateOf(initialConfig) }
    val intentions = remember { mutableStateListOf<String>() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF001529))) {
        // Decorative Blur Blobs
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = 100.dp)
                .size(300.dp)
                .background(Color(0xFF00D2FF).copy(alpha = 0.12f), CircleShape)
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .background(Color(0xFFFC2E7F).copy(alpha = 0.12f), CircleShape)
                .blur(100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { step / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(bottom = 24.dp)
                    .clip(CircleShape),
                color = Color(0xFF00B4D8),
                trackColor = Color.White.copy(alpha = 0.1f),
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
                        1 -> IdentityStep(config, pendingInvitations) { updated -> config = updated; step = 2 }
                        2 -> ProfessionalStep(config) { updated, ints -> 
                            config = updated
                            intentions.clear()
                            intentions.addAll(ints)
                            step = 3 
                        }
                        3 -> PersonaStep(config) { updated -> config = updated; step = 4 }
                        4 -> TeamsStep(config, pendingInvitations) { updated -> config = updated; step = 5 }
                        5 -> FinalStep(config) { onCompleted(config.copy(isOnboardingCompleted = true), intentions.toList()) }
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityStep(config: SeasonConfig, pendingInvitations: List<Team> = emptyList(), onNext: (SeasonConfig) -> Unit) {
    var firstName by remember { mutableStateOf(config.coachProfile.firstName) }
    var lastName by remember { mutableStateOf(config.coachProfile.lastName) }
    var nickname by remember { mutableStateOf(config.coachProfile.nickname) }
    var clubName by remember { mutableStateOf(config.coachProfile.clubName) }
    var profilePictureUri by remember { mutableStateOf(config.coachProfile.profilePictureUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        profilePictureUri = uri?.toString()
    }

    val isOfficialClub = pendingInvitations.isNotEmpty() || clubName.isNotBlank()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF00B4D8),
        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
        focusedLabelColor = Color(0xFF00B4D8),
        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
    )

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        
        Box(modifier = Modifier.size(100.dp).clickable { photoPickerLauncher.launch("image/*") }, contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.fillMaxSize(), 
                shape = CircleShape, 
                color = Color.White.copy(alpha = 0.08f), 
                border = BorderStroke(2.dp, Color(0xFF00B4D8).copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (profilePictureUri != null) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Photo de profil",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(40.dp), tint = Color(0xFF00B4D8))
                    }
                }
            }
            Surface(
                modifier = Modifier.size(28.dp), 
                shape = CircleShape, 
                color = Color(0xFF00B4D8), 
                border = BorderStroke(2.dp, Color(0xFF001529))
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(if (profilePictureUri == null) Icons.Default.Add else Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.White) }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Bienvenue Coach !", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        Text("Créez votre carte d'identité.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(32.dp))
        
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Surnom (ex: Le Boss, Coach...)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors)
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = clubName, 
                onValueChange = { clubName = it }, 
                label = { Text("Votre Club") }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(12.dp), 
                leadingIcon = { Icon(Icons.Default.SportsVolleyball, null, tint = Color(0xFF00B4D8)) },
                colors = fieldColors,
                supportingText = {
                    if (isOfficialClub && clubName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Club reconnu par VolleyConnect", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { onNext(config.copy(coachProfile = config.coachProfile.copy(
                firstName = firstName, 
                lastName = lastName, 
                nickname = nickname, 
                clubName = clubName,
                profilePictureUri = profilePictureUri
            ))) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            enabled = firstName.isNotBlank() && lastName.isNotBlank() && clubName.isNotBlank()
        ) {
            Text("Continuer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun ProfessionalStep(config: SeasonConfig, onNext: (SeasonConfig, List<String>) -> Unit) {
    var selectedDiploma by remember { mutableStateOf<Diploma?>(null) }
    val selectedModules = remember { 
        mutableStateMapOf<String, Boolean>().apply { 
            config.coachProfile.acquiredModules.forEach { put(it, true) }
        }
    }
    
    // Pour les novices
    var wantsJAPS by remember { mutableStateOf(false) }
    var wantsAFJ by remember { mutableStateOf(false) }

    var goalPersonal by remember { mutableStateOf(config.coachProfile.goalPersonal) }
    var goalCollective by remember { mutableStateOf(config.coachProfile.goalCollective) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF00B4D8),
        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
        focusedLabelColor = Color(0xFF00B4D8),
        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
    )

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text("Votre Parcours", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        Text("Définissez vos acquis et ambitions.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("DIPLÔMES ET MODULES ACQUIS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color(0xFF00B4D8), letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            
            ffvbDiplomas.forEach { diploma ->
                DiplomaExpandableCard(
                    diploma = diploma,
                    isExpanded = selectedDiploma?.id == diploma.id,
                    onClick = { selectedDiploma = if (selectedDiploma?.id == diploma.id) null else diploma },
                    selectedModules = selectedModules
                )
                Spacer(Modifier.height(12.dp))
            }
            
            val isNovice = selectedModules.none { it.value }
            if (isNovice) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFC2E7F).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.5.dp, Color(0xFFFC2E7F).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFC2E7F), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Accompagnement Novice", fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Tu n'as pas encore de diplôme ? On t'aide à démarrer !", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(16.dp))
                        
                        NoviceOption(
                            label = "M'inscrire à la JAPS (Valence, 08 sept.)", 
                            checked = wantsJAPS, 
                            onCheckedChange = { wantsJAPS = it }
                        )
                        Spacer(Modifier.height(8.dp))
                        NoviceOption(
                            label = "M'inscrire à l'AFJ (M7 à M13)", 
                            checked = wantsAFJ, 
                            onCheckedChange = { wantsAFJ = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("PROJETS DE SAISON", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color(0xFF00B4D8), letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = goalPersonal, onValueChange = { goalPersonal = it }, label = { Text("Objectif personnel") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = goalCollective, onValueChange = { goalCollective = it }, label = { Text("Objectif collectif") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = fieldColors)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { 
                val acquired = selectedModules.filter { it.value }.keys.toList()
                val level = ffvbDiplomas.lastOrNull { d -> d.modules.any { m -> selectedModules[m.id] == true } }?.name ?: "Novice"
                
                val currentIntentions = mutableListOf<String>()
                if (wantsJAPS) currentIntentions.add("JAPS")
                if (wantsAFJ) currentIntentions.add("AFJ")

                onNext(
                    config.copy(coachProfile = config.coachProfile.copy(
                        formationLevel = level,
                        acquiredModules = acquired,
                        goalPersonal = goalPersonal, 
                        goalCollective = goalCollective
                    )),
                    currentIntentions
                ) 
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("Suivant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun NoviceOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFC2E7F),
                uncheckedColor = Color.White.copy(alpha = 0.4f),
                checkmarkColor = Color.White
            )
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
fun DiplomaExpandableCard(
    diploma: Diploma,
    isExpanded: Boolean,
    onClick: () -> Unit,
    selectedModules: MutableMap<String, Boolean>
) {
    val acquiredCount = diploma.modules.count { selectedModules[it.id] == true }
    val accentColor = Color(0xFF00B4D8)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color.White.copy(alpha = 0.12f) 
                             else Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(
            0.5.dp, 
            if (isExpanded) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(diploma.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        if (acquiredCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = accentColor,
                                shape = CircleShape,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (acquiredCount == diploma.modules.size) "✓" else "$acquiredCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                    Text(diploma.level.uppercase(), style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
                diploma.modules.forEach { module ->
                    val isSelected = selectedModules[module.id] == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { 
                            selectedModules[module.id] = !(selectedModules[module.id] ?: false)
                        }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { selectedModules[module.id] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = Color.White.copy(alpha = 0.3f),
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            module.label, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
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
        Text("Quel Coach êtes-vous ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        Text("Choisissez votre profil dominant.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(48.dp))

        Column(modifier = Modifier.weight(1f)) {
            personas.forEach { (title, desc) ->
                val isSelected = selectedPersona == title
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedPersona = title },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF00B4D8).copy(alpha = 0.15f) 
                                         else Color.White.copy(alpha = 0.06f)
                    ),
                    border = BorderStroke(
                        0.5.dp, 
                        if (isSelected) Color(0xFF00B4D8).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected, 
                            onClick = { selectedPersona = title },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF00B4D8),
                                unselectedColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onNext(config.copy(coachProfile = config.coachProfile.copy(coachPersona = selectedPersona))) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
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
        Text("Vos Collectifs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        Text("Quels groupes encadrez-vous ?", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            // --- SECTION INVITATIONS ---
            if (pendingInvitations.isNotEmpty()) {
                item {
                    Text(
                        "INVITATIONS OFFICIELLES",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(pendingInvitations) { team ->
                    val isAlreadyAdded = teams.any { it.id == team.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = !isAlreadyAdded) { teams = teams + team },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAlreadyAdded) Color(0xFF1D9E75).copy(alpha = 0.15f) 
                                             else Color(0xFF00B4D8).copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(
                            0.5.dp, 
                            if (isAlreadyAdded) Color(0xFF1D9E75).copy(alpha = 0.4f) 
                            else Color(0xFF00B4D8).copy(alpha = 0.4f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAlreadyAdded) Icons.Default.CheckCircle else Icons.Default.AddCircle, 
                                contentDescription = null, 
                                tint = if (isAlreadyAdded) Color(0xFF4CAF50) else Color(0xFF00B4D8)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(team.name, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("Assignation club • ${team.format.label}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            }
                            if (!isAlreadyAdded) {
                                Text("AJOUTER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color(0xFF00B4D8))
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
                    color = Color(0xFF00B4D8),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (teams.isEmpty() && pendingInvitations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.3f))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Aucun groupe pour le moment.", 
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(teams) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = team.color) {}
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(team.name, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${team.format.label} • ${team.objective}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }
                        IconButton(onClick = { teams = teams.filter { it.id != team.id } }) { Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.4f)) }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showAddTeam = true }, 
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), 
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Ajouter manuellement")
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onNext(config.copy(teams = teams)) }, 
            modifier = Modifier.fillMaxWidth().height(60.dp), 
            enabled = teams.isNotEmpty(), 
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
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
        Surface(
            modifier = Modifier.size(120.dp), 
            shape = CircleShape, 
            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
            border = BorderStroke(2.dp, Color(0xFF4CAF50))
        ) {
            Box(contentAlignment = Alignment.Center) { 
                Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(70.dp)) 
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("C'est prêt, Coach $coachName !", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Votre environnement CoCoach est configuré pour le club ${config.coachProfile.clubName}.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onFinish, 
            modifier = Modifier.fillMaxWidth().height(64.dp), 
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
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
        containerColor = Color(0xFF002147),
        title = { Text("Nouvelle Équipe", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Nom (M13, M15...)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text("Format de jeu :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TeamFormat.entries.forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.fillMaxWidth().clickable { selectedFormat = format }
                    ) {
                        RadioButton(
                            selected = selectedFormat == format, 
                            onClick = { selectedFormat = format },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00B4D8), unselectedColor = Color.White.copy(alpha = 0.4f))
                        )
                        Text(format.label, color = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = objective, 
                    onValueChange = { objective = it }, 
                    label = { Text("Objectif (ex: Top 10 France)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Spacer(Modifier.height(20.dp))
                Text("Type de projet :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row {
                    listOf("Élite", "Développement", "Loisir").forEach { type ->
                        FilterChip(
                            selected = projectType == type, 
                            onClick = { projectType = type }, 
                            label = { Text(type) }, 
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00B4D8),
                                selectedLabelColor = Color.White,
                                labelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .border(if (selectedColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(Team(UUID.randomUUID().toString(), name, selectedColor, projectType, objective, selectedFormat)) }, 
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Ajouter", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Color.White.copy(alpha = 0.6f)) }
        }
    )
}
