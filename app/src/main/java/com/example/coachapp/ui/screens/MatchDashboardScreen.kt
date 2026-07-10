package com.example.coachapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import com.example.coachapp.ui.components.ScoreColumn
import com.example.coachapp.ui.components.TacticalBoardOverlay
import java.util.*

@Composable
fun MatchDashboardScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig,
    persistenceManager: PersistenceManager,
    onUpdatePlayer: (Player) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(seasonConfig.teams.firstOrNull()?.id) }
    val selectedTeam = remember(selectedTeamId, seasonConfig.teams) {
        seasonConfig.teams.find { it.id == selectedTeamId }
    }
    
    var showScorer by rememberSaveable { mutableStateOf(true) }
    var showRotation by rememberSaveable { mutableStateOf(true) }
    var showTacticalBoard by remember { mutableStateOf(false) }

    // --- SCANNER ANIMATION STATE ---
    var showScanner by remember { mutableStateOf(true) }
    val scannerProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scannerProgress.animateTo(
            targetValue = 1.2f, 
            animationSpec = tween(1200, easing = LinearEasing)
        )
        showScanner = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Match Dashboard", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                
                IconButton(onClick = { showTacticalBoard = true }) {
                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                }

                if (seasonConfig.teams.size > 1) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(selectedTeam?.name ?: "Sélectionner équipe")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            seasonConfig.teams.forEach { team ->
                                DropdownMenuItem(text = { Text(team.name) }, onClick = { selectedTeamId = team.id; expanded = false })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTeam != null) {
                ExpandableSection(
                    title = "Scoreur",
                    isExpanded = showScorer,
                    onToggle = { showScorer = !showScorer },
                    modifier = Modifier.weight(
                        if (showScorer && !showRotation) 1f 
                        else if (showScorer) 0.42f 
                        else 0.08f
                    )
                ) {
                    VolleyScorer(team = selectedTeam)
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExpandableSection(
                    title = "Rotations (${selectedTeam.format.label})",
                    isExpanded = showRotation,
                    onToggle = { showRotation = !showRotation },
                    modifier = Modifier.weight(
                        if (showRotation && !showScorer) 1f 
                        else if (showRotation) 0.58f 
                        else 0.08f
                    )
                ) {
                    RotationManager(team = selectedTeam, seasonConfig = seasonConfig, onUpdatePlayer = onUpdatePlayer)
                }
            }
        }

        TacticalBoardOverlay(
            isVisible = showTacticalBoard,
            onDismiss = { showTacticalBoard = false },
            persistenceManager = persistenceManager
        )

        // --- SCANNER OVERLAY ---
        if (showScanner) {
            val scannerColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * scannerProgress.value
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, scannerColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = y - 100f,
                        endY = y + 100f
                    )
                )
                drawLine(
                    color = scannerColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (isExpanded) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline, null)
                Spacer(Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (isExpanded) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun VolleyScorer(team: Team) {
    var scoreA by rememberSaveable { mutableIntStateOf(0) }
    var scoreB by rememberSaveable { mutableIntStateOf(0) }
    var setA by rememberSaveable { mutableIntStateOf(0) }
    var setB by rememberSaveable { mutableIntStateOf(0) }
    val setsHistory = remember { mutableStateListOf<String>() }

    val isTieBreak = (setA + setB) == 4
    val targetPoints = if (isTieBreak) 15 else 25
    
    val isSetFinished = (scoreA >= targetPoints || scoreB >= targetPoints) && kotlin.math.abs(scoreA - scoreB) >= 2

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        // --- Rappel des scores des sets précédents ---
        if (setsHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIQUE : ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                setsHistory.forEachIndexed { index, score ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = score,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ScoreColumn(team.name, scoreA, setA, { if (!isSetFinished) scoreA++ }, { if (scoreA > 0) scoreA-- })
            ScoreColumn("Adversaire", scoreB, setB, { if (!isSetFinished) scoreB++ }, { if (scoreB > 0) scoreB-- })
        }

        if (isSetFinished) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Set Terminé !") },
                text = { Text("Équipe ${if (scoreA > scoreB) team.name else "Adverse"} remporte le set ($scoreA - $scoreB)") },
                confirmButton = {
                    Button(onClick = {
                        setsHistory.add("$scoreA-$scoreB")
                        if (scoreA > scoreB) setA++ else setB++
                        scoreA = 0; scoreB = 0
                    }) { Text("Set Suivant") }
                }
            )
        }
    }
}

@Composable
fun RotationManager(team: Team, seasonConfig: SeasonConfig, onUpdatePlayer: (Player) -> Unit) {
    // Local override for the match format
    var matchFormat by remember { mutableStateOf(if (team.name.contains("M13", ignoreCase = true)) TeamFormat.FOUR_FOUR else team.format) }
    
    // Auto-adaptation logic
    LaunchedEffect(team.id) {
        matchFormat = if (team.name.contains("M13", ignoreCase = true)) TeamFormat.FOUR_FOUR else team.format
    }

    val isSmallFormat = matchFormat == TeamFormat.TWO_TWO || matchFormat == TeamFormat.THREE_THREE
    
    val teamPlayers = remember(team.id, seasonConfig.players) {
        seasonConfig.players.filter { it.teamId == team.id }
    }
    
    val positionsCount = matchFormat.playerCount
    var rotationOffset by rememberSaveable { mutableIntStateOf(0) }
    
    val onField = remember(matchFormat) { mutableStateListOf<String?>().apply { 
        repeat(positionsCount) { add(null) } 
    } }
    
    var selectedPositionIndex by remember { mutableIntStateOf(-1) }
    var selectedTargetRole by remember { mutableStateOf("") }
    var showActionDialog by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        // --- FORMAT SELECTOR (M15 and above, except M13) ---
        val canSwitchFormat = (team.name.contains("M15") || team.name.contains("M18") || 
                               team.name.contains("M21") || team.name.contains("Sénior") || 
                               team.name.contains("Senior")) && !team.name.contains("M13")

        if (canSwitchFormat) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(bottom = 12.dp)) {
                SegmentedButton(
                    selected = matchFormat == TeamFormat.FOUR_FOUR,
                    onClick = { matchFormat = TeamFormat.FOUR_FOUR },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("4x4") }
                SegmentedButton(
                    selected = matchFormat == TeamFormat.SIX_SIX,
                    onClick = { matchFormat = TeamFormat.SIX_SIX },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("6x6") }
            }
        }

        if (isSmallFormat) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFE67E22).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Format ${matchFormat.label}\nPas de rotations arrière à gérer.", 
                    textAlign = TextAlign.Center, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            Box(modifier = Modifier.height(220.dp).aspectRatio(0.85f).background(Color(0xFFE67E22), RoundedCornerShape(8.dp)).border(2.dp, Color.White, RoundedCornerShape(8.dp))) {
                val alignments = when(matchFormat) {
                    TeamFormat.SIX_SIX -> listOf(Alignment.BottomEnd, Alignment.TopEnd, Alignment.TopCenter, Alignment.TopStart, Alignment.BottomStart, Alignment.BottomCenter)
                    TeamFormat.FOUR_FOUR -> listOf(Alignment.BottomEnd, Alignment.TopEnd, Alignment.TopStart, Alignment.BottomStart)
                    else -> emptyList()
                }
                
                val defaultRoles = if (matchFormat == TeamFormat.SIX_SIX) 
                    listOf("P1", "P2", "P3", "P4", "P5", "P6")
                else 
                    listOf("Ar-D", "Av-D", "Av-G", "Ar-G")

                alignments.forEachIndexed { index, alignment ->
                    val actualIndex = (index + rotationOffset) % positionsCount
                    val playerId = onField.getOrNull(actualIndex)
                    val player = teamPlayers.find { it.id == playerId }
                    val roleLabel = player?.position?.take(2) ?: defaultRoles.getOrNull(index) ?: "?"
                    
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = alignment) {
                        PlayerCircle(
                            number = player?.number?.toString() ?: "?",
                            role = roleLabel,
                            isSelected = selectedPositionIndex == actualIndex,
                            onClick = { 
                                selectedPositionIndex = actualIndex
                                selectedTargetRole = player?.position ?: defaultRoles.getOrNull(index) ?: ""
                                showActionDialog = true
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = { rotationOffset = (rotationOffset + 1) % positionsCount }) {
                    Text("Rotation P${(rotationOffset % positionsCount) + 1}")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { 
                    onField.indices.forEach { onField[it] = null }
                    rotationOffset = 0
                }) {
                    Text("Vider")
                }
            }
        }
    }

    if (showActionDialog) {
        val currentPlayer = teamPlayers.find { it.id == onField[selectedPositionIndex] }
        val playersOnFieldIds = onField.filterNotNull()
        val benchPlayers = teamPlayers.filter { it.id !in playersOnFieldIds || it.id == currentPlayer?.id }

        PlayerActionDialog(
            currentPlayer = currentPlayer,
            targetRole = selectedTargetRole,
            benchPlayers = benchPlayers,
            onDismiss = { showActionDialog = false },
            onSubstitute = { newPlayerId ->
                onField[selectedPositionIndex] = newPlayerId
                showActionDialog = false
            },
            onUpdatePlayer = onUpdatePlayer
        )
    }
}

@Composable
fun PlayerCircle(number: String, role: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = number, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black)
            }
        }
        if (role.isNotEmpty()) {
            Text(text = role, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
fun PlayerActionDialog(
    currentPlayer: Player?,
    targetRole: String,
    benchPlayers: List<Player>,
    onDismiss: () -> Unit,
    onSubstitute: (String?) -> Unit,
    onUpdatePlayer: (Player) -> Unit
) {
    var showPositionEdit by remember { mutableStateOf(false) }

    val sortedBench = remember(benchPlayers, currentPlayer, targetRole) {
        val roleToMatch = currentPlayer?.position ?: targetRole
        benchPlayers.sortedWith(
            compareByDescending<Player> { 
                it.position.equals(roleToMatch, ignoreCase = true) || 
                it.position.startsWith(roleToMatch.take(3), ignoreCase = true) 
            }
            .thenBy { it.position }
            .thenBy { it.fullName }
        )
    }

    if (showPositionEdit && currentPlayer != null) {
        val positions = listOf("Passeur", "Pointu", "Central", "Réceptionneur-Attaquant", "Libero")
        AlertDialog(
            onDismissRequest = { showPositionEdit = false },
            title = { Text("Changer le poste de ${currentPlayer.fullName}") },
            text = {
                Column {
                    positions.forEach { pos ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                            onUpdatePlayer(currentPlayer.copy(position = pos))
                            showPositionEdit = false
                            onDismiss()
                        }.padding(12.dp)) {
                            RadioButton(selected = currentPlayer.position == pos, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(pos)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPositionEdit = false }) { Text("Annuler") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(currentPlayer?.fullName ?: "Sélectionner un titulaire") },
            text = {
                Column {
                    if (currentPlayer != null) {
                        Button(onClick = { showPositionEdit = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Changer le Poste (${currentPlayer.position})")
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    Text(
                        text = if (currentPlayer != null) "Remplacer par :" else "Placer sur le terrain :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(sortedBench) { player ->
                            val isSamePos = (currentPlayer?.position ?: targetRole).let { role ->
                                player.position.equals(role, ignoreCase = true) || 
                                player.position.startsWith(role.take(3), ignoreCase = true)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSamePos) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable { onSubstitute(player.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSamePos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("#${player.number}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.fullName, style = MaterialTheme.typography.bodyMedium)
                                    Text(player.position, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                
                                if (player.id == currentPlayer?.id) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        if (currentPlayer != null) {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                TextButton(onClick = { onSubstitute(null) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Sortir le joueur (Position vide)", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
        )
    }
}
