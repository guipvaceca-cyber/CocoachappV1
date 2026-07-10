package com.example.coachapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import com.example.coachapp.ui.screens.ElementEditDialog
import com.example.coachapp.ui.screens.PlayerTacticalIcon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalBoardOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    persistenceManager: PersistenceManager
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = Modifier.fillMaxHeight(0.95f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            TacticalBoardContent(persistenceManager)
        }
    }
}

@Composable
fun TacticalBoardContent(persistenceManager: PersistenceManager) {
    var boardMode by rememberSaveable { mutableStateOf("TRAINING") }
    val lines = remember { mutableStateListOf<BoardLine>() }
    var currentLinePoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color.Blue) }
    
    val elements = remember { mutableStateListOf<BoardElement>() }
    var selectedElementType by remember { mutableStateOf<ElementType?>(null) }
    
    var draggedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementForEdit by remember { mutableStateOf<BoardElement?>(null) }

    val courtColor = if (boardMode == "TRAINING") Color(0xFFE67E22) else Color(0xFF2980B9)
    val lineColor = Color.White
    val density = LocalDensity.current
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadList by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Plaquette Tactique", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Row {
                IconButton(onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) }) { Icon(Icons.Default.Undo, null) }
                IconButton(onClick = { showSaveDialog = true }) { Icon(Icons.Default.Save, null) }
                IconButton(onClick = { showLoadList = true }) { Icon(Icons.Default.FolderOpen, null) }
                IconButton(onClick = { lines.clear(); elements.clear() }) { Icon(Icons.Default.DeleteForever, null, tint = Color.Red) }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(selected = boardMode == "TRAINING", onClick = { boardMode = "TRAINING" }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                Text("Entraînement")
            }
            SegmentedButton(selected = boardMode == "MATCH", onClick = { boardMode = "MATCH" }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                Text("Match")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Color.Blue, Color.Red, Color.Black, Color.Green).forEach { color ->
                Box(
                    modifier = Modifier.size(32.dp).background(color, CircleShape)
                        .border(if (selectedColor == color && selectedElementType == null) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { selectedColor = color; selectedElementType = null }
                )
            }
            VerticalDivider(modifier = Modifier.height(24.dp))
            ElementType.entries.forEach { type ->
                IconButton(
                    onClick = { selectedElementType = type },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if (selectedElementType == type) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                ) {
                    Icon(type.icon, null, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxSize().weight(1f).background(courtColor, RoundedCornerShape(12.dp)).border(2.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                .pointerInput(selectedElementType) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val hitElement = elements.find { (it.position - offset).getDistance() < 100f }
                            if (hitElement != null) { draggedElementId = hitElement.id; selectedElementType = null } 
                            else if (selectedElementType != null) {
                                val newEl = BoardElement(UUID.randomUUID().toString(), selectedElementType!!, offset)
                                elements.add(newEl); draggedElementId = newEl.id; selectedElementType = null
                            } else {
                                draggedElementId = null; currentLinePoints = mutableStateListOf(offset)
                                lines.add(BoardLine(currentLinePoints.toList(), selectedColor))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (draggedElementId != null) {
                                val index = elements.indexOfFirst { it.id == draggedElementId }
                                if (index != -1) elements[index] = elements[index].copy(position = change.position)
                            } else if (selectedElementType == null && lines.isNotEmpty()) {
                                currentLinePoints.add(change.position)
                                lines[lines.size - 1] = lines.last().copy(points = currentLinePoints.toList())
                            }
                        },
                        onDragEnd = { draggedElementId = null }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                drawLine(lineColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 8f)
                val attackLineOffset = h * 0.15f
                drawLine(lineColor, Offset(0f, h / 2 - attackLineOffset), Offset(w, h / 2 - attackLineOffset), strokeWidth = 4f)
                drawLine(lineColor, Offset(0f, h / 2 + attackLineOffset), Offset(w, h / 2 + attackLineOffset), strokeWidth = 4f)
                drawRect(lineColor, style = Stroke(width = 6f))

                lines.forEach { line ->
                    for (i in 0 until line.points.size - 1) {
                        drawLine(color = line.color, start = line.points[i], end = line.points[i + 1], strokeWidth = 8f, cap = StrokeCap.Round)
                    }
                }
            }
            elements.forEach { element ->
                val xDp = with(density) { element.position.x.toDp() } - 24.dp
                val yDp = with(density) { element.position.y.toDp() } - 24.dp
                Box(modifier = Modifier.offset(xDp, yDp).clickable { selectedElementForEdit = element }) {
                    if (element.type == ElementType.PLAYER) PlayerTacticalIcon(element)
                    else Icon(imageVector = element.type.icon, contentDescription = null, tint = element.type.color, modifier = Modifier.size(40.dp).background(Color.White.copy(0.7f), CircleShape).padding(4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (selectedElementForEdit != null) {
        ElementEditDialog(
            element = selectedElementForEdit!!,
            onDismiss = { selectedElementForEdit = null },
            onUpdate = { updated ->
                val idx = elements.indexOfFirst { it.id == updated.id }
                if (idx != -1) elements[idx] = updated
                selectedElementForEdit = null
            },
            onDelete = { elements.removeAll { it.id == selectedElementForEdit!!.id }; selectedElementForEdit = null }
        )
    }

    if (showSaveDialog) {
        var boardName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Sauvegarder") },
            text = { OutlinedTextField(value = boardName, onValueChange = { boardName = it }, label = { Text("Nom") }) },
            confirmButton = {
                Button(onClick = {
                    persistenceManager.saveTacticalBoard(boardName, lines.toList(), elements.toList())
                    showSaveDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Annuler") } }
        )
    }

    if (showLoadList) {
        val boards = persistenceManager.loadTacticalBoards()
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
        AlertDialog(
            onDismissRequest = { showLoadList = false },
            title = { Text("Mes Planches") },
            text = {
                if (boards.isEmpty()) Text("Vide.")
                else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(boards) { board ->
                            ListItem(
                                headlineContent = { Text(board.name) },
                                supportingContent = { Text(dateFormatter.format(Date(board.date))) },
                                modifier = Modifier.clickable {
                                    lines.clear(); lines.addAll(board.lines)
                                    elements.clear(); elements.addAll(board.elements)
                                    showLoadList = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLoadList = false }) { Text("Fermer") } }
        )
    }
}
