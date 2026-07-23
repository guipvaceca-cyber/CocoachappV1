package com.example.coachapp.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coachapp.data.*
import com.example.coachapp.ui.scouting.ScoutingViewModel
import com.example.coachapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoutingOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    allPlayers: List<Player>,
    onField: List<String?>,
    scoutingViewModel: ScoutingViewModel = viewModel()
) {
    if (!isVisible) return

    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Force Immersive Full Screen for Dialog
        val view = LocalView.current
        val window = (view.context as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                it.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
                // Remove all system insets/margins
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    it.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }

        val scope = rememberCoroutineScope()

        // Force Landscape orientation
        DisposableEffect(Unit) {
            val activity = context as? Activity
            val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                activity?.requestedOrientation = originalOrientation
            }
        }

        BackHandler { onDismiss() }

        // Dummy players for demo if list is empty
        val effectivePlayers = remember(allPlayers) {
            if (allPlayers.isEmpty()) {
                listOf(
                    Player("d1", "demo", "Demo", "Passeur", 1, "Passeur"),
                    Player("d2", "demo", "Demo", "RA1", 2, "Réceptionneur-Attaquant"),
                    Player("d3", "demo", "Demo", "Central1", 3, "Central"),
                    Player("d4", "demo", "Demo", "Pointu", 4, "Pointu"),
                    Player("d5", "demo", "Demo", "RA2", 5, "Réceptionneur-Attaquant"),
                    Player("d6", "demo", "Demo", "Libero", 6, "Libero")
                )
            } else allPlayers
        }

        var currentTab by remember { mutableStateOf("terrain") }
        
        // Auto-select first player if none selected
        var selectedPlayerId by remember(effectivePlayers) { 
            mutableStateOf<String?>(onField.firstOrNull { it != null } ?: effectivePlayers.firstOrNull()?.id) 
        }
        var selectedAction by remember { mutableStateOf<ScoutingAction?>(null) }
        var pendingPosition by remember { mutableStateOf<Offset?>(null) }
        var flashGradeColor by remember { mutableStateOf<Color?>(null) }

        val selectedPlayer = remember(selectedPlayerId, effectivePlayers) {
            effectivePlayers.find { it.id == selectedPlayerId }
        }

        LaunchedEffect(flashGradeColor) {
            if (flashGradeColor != null) {
                delay(400)
                flashGradeColor = null
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ScoutingBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Flash Effect Overlay
                AnimatedVisibility(
                    visible = flashGradeColor != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(flashGradeColor?.copy(alpha = 0.2f) ?: Color.Transparent))
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar (Score & Status)
                    ScoutingTopBar(
                        viewModel = scoutingViewModel,
                        onClose = onDismiss
                    )

                    Row(modifier = Modifier.weight(1f)) {
                        if (currentTab == "terrain") {
                            // Left Side: Court
                            Box(
                                modifier = Modifier
                                    .weight(0.65f)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                VolleyballCourtScouting(
                                    selectedZone = selectedPlayer?.let { getZoneFromPosition(it.position) } ?: 0,
                                    onPositionClick = { pos -> 
                                        pendingPosition = pos 
                                        // Visual feedback if needed
                                    },
                                    lastPosition = pendingPosition,
                                    modifier = Modifier.aspectRatio(2.1f) // Slightly wider for better fit
                                )
                            }

                            // Right Side: Controls
                            Column(
                                modifier = Modifier
                                    .weight(0.35f)
                                    .fillMaxHeight()
                                    .drawBehind {
                                        drawLine(
                                            Color.White.copy(alpha = 0.05f),
                                            Offset(0f, 0f),
                                            Offset(0f, this.size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                            ) {
                                // Player Selection
                                PlayerSelectionGrid(
                                    players = remember(onField, effectivePlayers) { 
                                        val onFieldIds = onField.filterNotNull()
                                        if (onFieldIds.isNotEmpty()) {
                                            effectivePlayers.filter { it.id in onFieldIds }
                                        } else {
                                            effectivePlayers
                                        }
                                    },
                                    selectedId = selectedPlayerId,
                                    onSelect = { selectedPlayerId = it; pendingPosition = null }
                                )

                                // Action Selection
                                ActionSelectionRow(
                                    selectedAction = selectedAction,
                                    onSelect = { selectedAction = if (selectedAction == it) null else it },
                                    isEnabled = selectedPlayerId != null
                                )

                                // Grade Selection
                                GradeSelectionRow(
                                    isEnabled = selectedPlayerId != null && selectedAction != null,
                                    onGrade = { grade ->
                                        selectedPlayer?.let { p ->
                                            selectedAction?.let { a ->
                                                scoutingViewModel.addEntry(p, a, grade, pendingPosition?.x, pendingPosition?.y)
                                                flashGradeColor = grade.color
                                                selectedAction = null
                                                pendingPosition = null
                                            }
                                        }
                                    }
                                )

                                // History Mini
                                RecentLogList(
                                    log = scoutingViewModel.log,
                                    onUndo = { scoutingViewModel.undoLast() }
                                )
                            }
                        } else {
                            // Stats Tab
                            StatsTabView(
                                allPlayers = effectivePlayers,
                                viewModel = scoutingViewModel
                            )
                        }
                    }

                    // Bottom Selectors (Compact)
                    ScoutingBottomNav(
                        currentTab = currentTab,
                        onTabSelect = { currentTab = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ScoutingTopBar(
    viewModel: ScoutingViewModel,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Centered Content
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SET ${viewModel.setNumber.value} · ÉCH. ${viewModel.rally.value}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                Text(
                    "MATCH LIVE",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.width(32.dp))

            // Score Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreBox(label = "NOUS", score = viewModel.scoreUs.value, color = VolleyCyan, onAdd = { viewModel.scoreUs.value++ }, onSub = { if(viewModel.scoreUs.value > 0) viewModel.scoreUs.value-- })
                Text(":", color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                ScoreBox(label = "EUX", score = viewModel.scoreThem.value, color = Color.White.copy(alpha = 0.4f), onAdd = { viewModel.scoreThem.value++ }, onSub = { if(viewModel.scoreThem.value > 0) viewModel.scoreThem.value-- })
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ScoreBox(label: String, score: Int, color: Color, onAdd: () -> Unit, onSub: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAdd() }) {
        Text(label, fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold)
        Text(score.toString(), fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PlayerSelectionGrid(
    players: List<Player>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("JOUEUR", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 4.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            players.forEach { player ->
                val isSelected = player.id == selectedId
                val glowAlpha = remember { Animatable(0f) }
                val scope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(width = 84.dp, height = 54.dp)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) VolleyCyan else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) VolleyCyan.copy(alpha = 0.25f) else ScoutingCard)
                        .clickable {
                            scope.launch {
                                glowAlpha.snapTo(0.6f)
                                glowAlpha.animateTo(0f, animationSpec = tween(250))
                            }
                            onSelect(player.id)
                        }
                ) {
                    // Glow Overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = glowAlpha.value)))

                    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Text(player.fullName, fontSize = 10.sp, fontWeight = if(isSelected) FontWeight.ExtraBold else FontWeight.Bold, color = if(isSelected) Color.White else Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("#${player.number} · ${player.position.take(3)}", fontSize = 8.sp, color = if(isSelected) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun ActionSelectionRow(
    selectedAction: ScoutingAction?,
    onSelect: (ScoutingAction) -> Unit,
    isEnabled: Boolean
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("ACTION", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScoutingAction.entries.forEach { action ->
                val isSelected = selectedAction == action
                val glowAlpha = remember { Animatable(0f) }
                val scope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(52.dp)
                        .alpha(if (isEnabled) 1f else 0.4f)
                        .border(
                            width = if (isSelected) 4.dp else 1.dp,
                            color = if (isSelected) action.color else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) action.color.copy(alpha = 0.3f) else ScoutingCard)
                        .clickable(enabled = isEnabled) {
                            scope.launch {
                                glowAlpha.snapTo(0.6f)
                                glowAlpha.animateTo(0f, animationSpec = tween(250))
                            }
                            onSelect(action)
                        }
                ) {
                    // Glow Overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = glowAlpha.value)))
                    
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            action.short, 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 15.sp, 
                            color = if(isSelected) Color.White else action.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeSelectionRow(
    isEnabled: Boolean,
    onGrade: (ScoutingGrade) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("QUALITÉ", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScoutingGrade.entries.forEach { grade ->
                val glowAlpha = remember { Animatable(0f) }
                val scope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .width(75.dp)
                        .height(65.dp)
                        .alpha(if (isEnabled) 1f else 0.3f)
                        .border(
                            width = 2.5.dp,
                            color = if (isEnabled) grade.color.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isEnabled) grade.color.copy(alpha = 0.25f) else ScoutingCard)
                        .clickable(enabled = isEnabled) {
                            scope.launch {
                                glowAlpha.snapTo(0.6f)
                                glowAlpha.animateTo(0f, animationSpec = tween(250))
                            }
                            onGrade(grade)
                        }
                ) {
                    // Glow Overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = glowAlpha.value)))

                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(grade.symbol, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = if(isEnabled) Color.White else grade.color.copy(alpha = 0.5f))
                        Text(grade.label, fontSize = 10.sp, color = if(isEnabled) Color.White.copy(alpha = 0.9f) else grade.color.copy(alpha = 0.4f), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecentLogList(
    log: List<ScoutingLogEntry>,
    onUndo: () -> Unit
) {
    Column(modifier = Modifier.padding(8.dp).fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("HISTORIQUE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.weight(1f))
            if (log.isNotEmpty()) {
                TextButton(onClick = onUndo, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Annuler", fontSize = 10.sp)
                }
            }
        }
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(log.take(5)) { index, entry ->
                Surface(
                    color = if (index == 0) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).background(VolleyCyan.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(entry.playerNumber.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VolleyCyan)
                        }
                        Text(entry.playerName, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp).weight(1f), maxLines = 1)
                        Text(entry.action.short, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = entry.action.color, modifier = Modifier.background(entry.action.color.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                        Text(entry.grade.symbol, fontSize = 18.sp, fontWeight = FontWeight.Black, color = entry.grade.color, modifier = Modifier.padding(start = 8.dp).width(20.dp), textAlign = TextAlign.Center)
                    }
                }
            }
            if (log.isEmpty()) {
                item { Text("Aucune action enregistrée", fontSize = 10.sp, color = Color.White.copy(alpha = 0.1f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) }
            }
        }
    }
}

@Composable
fun ScoutingBottomNav(
    currentTab: String,
    onTabSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.Black.copy(alpha = 0.3f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("terrain" to "TERRAIN", "stats" to "STATS").forEach { (id, label) ->
            val active = currentTab == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelect(id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, 
                    fontSize = 11.sp, 
                    color = if (active) VolleyCyan else Color.White.copy(alpha = 0.4f), 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun VolleyballCourtScouting(
    selectedZone: Int,
    onPositionClick: (Offset) -> Unit,
    lastPosition: Offset? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onPositionClick(Offset(offset.x / size.width.toFloat(), offset.y / size.height.toFloat()))
                }
            }
        ) {
            val width = size.width
            val height = size.height
            
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFF0B3118), Color(0xFF14512A), Color(0xFF0B3118))),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f)
            )

            val margin = 30f
            val courtW = width - (margin * 2)
            val courtH = height - (margin * 2)
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(margin, margin),
                size = Size(courtW, courtH),
                style = Stroke(width = 3f)
            )

            val netX = width / 2
            drawRect(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(netX - 4f, margin), size = Size(8f, courtH))
            drawLine(Color.White, Offset(netX - 4f, margin), Offset(netX - 4f, height - margin), strokeWidth = 2f)
            drawLine(Color.White, Offset(netX + 4f, margin), Offset(netX + 4f, height - margin), strokeWidth = 2f)

            val attackOffset = courtW / 6f
            drawLine(Color.White.copy(alpha = 0.5f), Offset(netX - attackOffset, margin), Offset(netX - attackOffset, height - margin), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.5f), Offset(netX + attackOffset, margin), Offset(netX + attackOffset, height - margin), strokeWidth = 1.5f)

            val cellW = (courtW / 2) / 3
            val cellH = courtH / 2
            val colBack = margin
            val colMid = margin + cellW
            val colFront = margin + cellW * 2
            val rowTop = margin
            val rowBottom = margin + cellH

            val zones = mapOf(
                4 to Offset(colFront + cellW/2, rowTop + cellH/2),
                3 to Offset(colMid + cellW/2, rowTop + cellH/2),
                2 to Offset(colBack + cellW/2, rowTop + cellH/2),
                5 to Offset(colFront + cellW/2, rowBottom + cellH/2),
                6 to Offset(colMid + cellW/2, rowBottom + cellH/2),
                1 to Offset(colBack + cellW/2, rowBottom + cellH/2)
            )

            zones.forEach { (zone, pos) ->
                val isSelected = zone == selectedZone
                if (isSelected) {
                    drawCircle(VolleyCyan.copy(alpha = 0.25f), radius = 40f, center = pos)
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    zone.toString(),
                    pos.x,
                    pos.y + 15f,
                    android.graphics.Paint().apply {
                        color = if (isSelected) android.graphics.Color.CYAN else android.graphics.Color.argb(60, 255, 255, 255)
                        textSize = 45f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }

            lastPosition?.let { pos ->
                drawCircle(color = Color.Yellow, radius = 8f, center = Offset(pos.x * width, pos.y * height))
                drawCircle(color = Color.Yellow.copy(alpha = 0.3f), radius = 20f, center = Offset(pos.x * width, pos.y * height), style = Stroke(width = 2f))
            }
        }
    }
}

@Composable
fun StatsTabView(
    allPlayers: List<Player>,
    viewModel: ScoutingViewModel
) {
    val stats = remember(viewModel.log.size) { viewModel.getStats(allPlayers) }
    val pointsCount = viewModel.log.count { it.x != null }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(16.dp),
            color = ScoutingCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                VolleyballCourtHeatmap(log = viewModel.log)
                Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Text(
                        "HEATMAP GLOBALE", 
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "$pointsCount points enregistrés", 
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(stats.filter { it.totalActions > 0 }.sortedByDescending { it.globalEfficiency }) { _, playerStat ->
                Surface(
                    color = ScoutingCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.width(100.dp)) {
                            Text(playerStat.player.fullName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("#${playerStat.player.number} · ${playerStat.player.position}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                        }

                        Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { playerStat.globalEfficiency / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = getEfficiencyColor(playerStat.globalEfficiency),
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                            Text("${playerStat.globalEfficiency}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        Row(modifier = Modifier.weight(1f).padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoutingAction.entries.forEach { action ->
                                val actionStat = playerStat.statsByAction[action]
                                if (actionStat != null && actionStat.total > 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(action.short, fontSize = 7.sp, color = action.color, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))) {
                                            Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                                Box(modifier = Modifier.fillMaxWidth().weight(actionStat.efficiency / 100f).background(action.color.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                                            }
                                            Text(actionStat.total.toString(), fontSize = 10.sp, color = Color.White, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolleyballCourtHeatmap(log: List<ScoutingLogEntry>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Dark background
        drawRoundRect(
            color = Color(0xFF0B3118), 
            size = Size(width, height), 
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
        )
        
        val margin = 20f
        val courtW = width - (margin * 2)
        val courtH = height - (margin * 2)
        
        // Draw Boundary
        drawRect(
            color = Color.White.copy(alpha = 0.4f), 
            topLeft = Offset(margin, margin), 
            size = Size(courtW, courtH), 
            style = Stroke(width = 1.5f)
        )
        
        // Draw Net
        drawLine(
            Color.White.copy(alpha = 0.6f), 
            Offset(width/2, margin), 
            Offset(width/2, height - margin), 
            strokeWidth = 2f
        )
        
        // Draw Points with slight scaling adjustment to fit margin
        log.forEach { entry ->
            val px = entry.x
            val py = entry.y
            if (px != null && py != null) {
                // Ensure points are within court boundaries including margin
                val drawX = margin + (px * courtW)
                val drawY = margin + (py * courtH)
                
                drawCircle(
                    color = entry.grade.color, 
                    radius = 5f, 
                    center = Offset(drawX, drawY)
                )
                // Small glow
                drawCircle(
                    color = entry.grade.color.copy(alpha = 0.3f), 
                    radius = 10f, 
                    center = Offset(drawX, drawY)
                )
            }
        }
    }
}

fun getEfficiencyColor(eff: Int): Color {
    return when {
        eff >= 70 -> GradeGood
        eff >= 40 -> GradeOK
        else -> GradeError
    }
}

fun getZoneFromPosition(pos: String): Int {
    return when (pos.lowercase()) {
        "passeur" -> 1
        "pointu" -> 2
        "central" -> 3
        "réceptionneur-attaquant" -> 4
        "libero" -> 6
        else -> 0
    }
}
