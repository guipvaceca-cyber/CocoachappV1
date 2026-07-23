package com.example.coachapp.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.Player

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatTerrainOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    stats: Map<String, Int>,
    onField: List<String?>,
    allPlayers: List<Player>,
    onStatAdd: (String) -> Unit,
    onStatRemove: (String) -> Unit,
    onReset: () -> Unit
) {
    if (isVisible) {
        val context = LocalContext.current
        
        // Force Landscape orientation while visible
        DisposableEffect(Unit) {
            val activity = context.findActivity()
            val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                activity?.requestedOrientation = originalOrientation
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = Modifier.fillMaxSize(),
            dragHandle = null,
            containerColor = Color(0xFF001529)
        ) {
            StatTerrainContent(
                onDismiss = onDismiss,
                stats = stats,
                onField = onField,
                allPlayers = allPlayers,
                onStatAdd = onStatAdd,
                onStatRemove = onStatRemove,
                onReset = onReset
            )
        }
    }
}

@Composable
fun StatTerrainContent(
    onDismiss: () -> Unit,
    stats: Map<String, Int>,
    onField: List<String?>,
    allPlayers: List<Player>,
    onStatAdd: (String) -> Unit,
    onStatRemove: (String) -> Unit,
    onReset: () -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("Service") }
    
    // Stack for Undo
    val actionHistory = remember { mutableStateListOf<String>() }

    val categoryColors = mapOf(
        "Service" to Color(0xFF2196F3),
        "Réception" to Color(0xFFFF9800),
        "Attaque" to Color(0xFFF44336),
        "Bloc" to Color(0xFF9C27B0),
        "Défense" to Color(0xFF4CAF50)
    )
    
    val activeColor = categoryColors[selectedCategory] ?: Color(0xFF00B4D8)
    val playersOnField = remember(onField, allPlayers) {
        onField.filterNotNull().mapNotNull { id -> allPlayers.find { it.id == id } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(12.dp)
    ) {
        // --- HEADER (TITLE CENTERED) ---
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                "STATISTIQUES TERRAIN",
                modifier = Modifier.align(Alignment.Center).padding(vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00B4D8),
                letterSpacing = 2.sp
            )
            
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                if (actionHistory.isNotEmpty()) {
                    IconButton(onClick = {
                        val lastKey = actionHistory.removeAt(actionHistory.size - 1)
                        onStatRemove(lastKey)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }
                IconButton(onClick = {
                    onReset()
                    actionHistory.clear()
                }) {
                    Icon(Icons.Default.DeleteSweep, null, tint = Color.Red.copy(alpha = 0.7f))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }

        // --- PLAYER SELECTION (BELOW TITLE, CENTERED) ---
        PlayerSelectionRow(
            players = playersOnField, 
            selectedId = selectedPlayerId, 
            onSelect = { selectedPlayerId = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        StatTerrainBody(
            selectedPlayerId = selectedPlayerId,
            categories = categoryColors.keys.toList(),
            categoryColors = categoryColors,
            selectedCategory = selectedCategory,
            onCategorySelect = { selectedCategory = it },
            stats = stats,
            activeColor = activeColor,
            onStatClick = { zoneId ->
                val key = "${selectedPlayerId ?: "ALL"}|$selectedCategory|$zoneId"
                actionHistory.add(key)
                onStatAdd(key)
            }
        )
    }
}

@Composable
fun ColumnScope.StatTerrainBody(
    selectedPlayerId: String?,
    categories: List<String>,
    categoryColors: Map<String, Color>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    stats: Map<String, Int>,
    activeColor: Color,
    onStatClick: (String) -> Unit
) {
    val filteredStats = remember(stats, selectedPlayerId, selectedCategory) {
        stats.filter { (key, _) -> 
            val parts = key.split("|")
            if (parts.size < 3) return@filter false
            val pId = parts[0]
            val cat = parts[1]
            (selectedPlayerId == null || pId == selectedPlayerId) && (cat == selectedCategory)
        }.mapKeys { (key, _) -> key.split("|")[2] }
    }

    val totalCount = remember(filteredStats) { filteredStats.values.sum() }

    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp), 
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                Text(
                    text = "$selectedCategory : $totalCount".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = activeColor.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                StatTerrainTool(
                    stats = filteredStats,
                    onStatClick = onStatClick,
                    modifier = Modifier.weight(1f),
                    activeColor = activeColor
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                CategoryButton(
                    label = cat,
                    isSelected = selectedCategory == cat,
                    activeColor = categoryColors[cat] ?: Color(0xFF00B4D8),
                    onClick = { onCategorySelect(cat) },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

@Composable
fun PlayerSelectionRow(
    players: List<Player>, 
    selectedId: String?, 
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                PlayerCircleItem(
                    number = "ALL",
                    isSelected = selectedId == null,
                    onClick = { onSelect(null) },
                    size = 36.dp
                )
            }
            items(players) { player ->
                PlayerCircleItem(
                    number = player.number.toString(),
                    isSelected = selectedId == player.id,
                    onClick = { onSelect(player.id) },
                    size = 36.dp
                )
            }
        }
    }
}

@Composable
fun PlayerCircleItem(number: String, isSelected: Boolean, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = if (isSelected) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                number, 
                fontWeight = FontWeight.Black, 
                color = Color.White, 
                fontSize = if (size < 40.dp) 11.sp else 14.sp
            )
        }
    }
}

@Composable
fun CategoryButton(
    label: String, 
    isSelected: Boolean, 
    activeColor: Color,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isSelected) activeColor else Color.White.copy(alpha = 0.1f)),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) activeColor else Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
