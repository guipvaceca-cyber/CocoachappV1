package com.example.coachapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import com.example.coachapp.ui.screens.ElementEditDialog
import com.example.coachapp.ui.screens.PlayerTacticalIcon
import java.text.SimpleDateFormat
import java.util.*

// Helper to rotate Offset
fun Offset.rotate(degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val cos = Math.cos(rad)
    val sin = Math.sin(rad)
    return Offset(
        (x * cos - y * sin).toFloat(),
        (x * sin + y * cos).toFloat()
    )
}

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
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color(0xFF001529) // Theme Cockpit
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
    var selectedColor by remember { mutableStateOf(Color(0xFF2196F3)) }
    
    val elements = remember { mutableStateListOf<BoardElement>() }
    var selectedElementType by remember { mutableStateOf<ElementType?>(null) }
    
    var draggedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementForEdit by remember { mutableStateOf<BoardElement?>(null) }
    var isRotating by remember { mutableStateOf(false) }

    val courtColor = if (boardMode == "TRAINING") Color(0xFFE67E22) else Color(0xFF0C447C)
    val lineColor = Color.White
    val density = LocalDensity.current
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadList by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Plaquette Tactique", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Row {
                IconButton(onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) }) { 
                    Icon(Icons.Default.Undo, null, tint = Color.White) 
                }
                IconButton(onClick = { showSaveDialog = true }) { 
                    Icon(Icons.Default.Save, null, tint = Color.White) 
                }
                IconButton(onClick = { showLoadList = true }) { 
                    Icon(Icons.Default.FolderOpen, null, tint = Color.White) 
                }
                IconButton(onClick = { lines.clear(); elements.clear() }) { 
                    Icon(Icons.Default.DeleteForever, null, tint = Color.Red.copy(alpha = 0.8f)) 
                }
            }
        }

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                val colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Color(0xFF00B4D8),
                    activeContentColor = Color.White,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = Color.White.copy(alpha = 0.6f)
                )
                SegmentedButton(
                    selected = boardMode == "TRAINING", 
                    onClick = { boardMode = "TRAINING" }, 
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = colors
                ) {
                    Text("Entraînement", fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = boardMode == "MATCH", 
                    onClick = { boardMode = "MATCH" }, 
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = colors
                ) {
                    Text("Match", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(), 
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(Color(0xFF2196F3), Color(0xFFF44336), Color.Black, Color(0xFF4CAF50)).forEach { color ->
                    Box(
                        modifier = Modifier.size(32.dp).background(color, CircleShape)
                            .border(if (selectedColor == color && selectedElementType == null) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { selectedColor = color; selectedElementType = null }
                    )
                }
                VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.2f))
                ElementType.entries.forEach { type ->
                    IconButton(
                        onClick = { selectedElementType = type },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (selectedElementType == type) Color(0xFF00B4D8).copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (selectedElementType == type) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(type.icon, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxSize().weight(1f).background(courtColor, RoundedCornerShape(12.dp)).border(2.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                .pointerInput(selectedElementType) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val hitElement = elements.find { (it.position - offset).getDistance() < 60f }
                            
                            // Check for rotation handle hit
                            if (selectedElementId != null) {
                                val selected = elements.find { it.id == selectedElementId }
                                if (selected != null) {
                                    val handlePos = selected.position + Offset(0f, -80f).rotate(selected.rotation)
                                    if ((handlePos - offset).getDistance() < 40f) {
                                        isRotating = true
                                        return@detectDragGestures
                                    }
                                }
                            }

                            if (hitElement != null) { 
                                draggedElementId = hitElement.id
                                selectedElementId = hitElement.id
                                selectedElementType = null 
                                isRotating = false
                            } 
                            else if (selectedElementType != null) {
                                val newEl = BoardElement(UUID.randomUUID().toString(), selectedElementType!!, offset)
                                elements.add(newEl)
                                draggedElementId = newEl.id
                                selectedElementId = newEl.id
                                selectedElementType = null
                                isRotating = false
                            } else {
                                draggedElementId = null
                                selectedElementId = null
                                isRotating = false
                                currentLinePoints = mutableStateListOf(offset)
                                lines.add(BoardLine(currentLinePoints.toList(), selectedColor))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (isRotating && selectedElementId != null) {
                                val index = elements.indexOfFirst { it.id == selectedElementId }
                                if (index != -1) {
                                    val element = elements[index]
                                    val dx = change.position.x - element.position.x
                                    val dy = change.position.y - element.position.y
                                    val angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                    elements[index] = element.copy(rotation = angle)
                                }
                            } else if (draggedElementId != null) {
                                val index = elements.indexOfFirst { it.id == draggedElementId }
                                if (index != -1) elements[index] = elements[index].copy(position = change.position)
                            } else if (selectedElementType == null && lines.isNotEmpty()) {
                                currentLinePoints.add(change.position)
                                lines[lines.size - 1] = lines.last().copy(points = currentLinePoints.toList())
                            }
                        },
                        onDragEnd = { 
                            draggedElementId = null 
                            isRotating = false
                        }
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
                val isSelected = element.id == selectedElementId

                // Rotation Handle
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .offset {
                                val rotOffset = Offset(0f, -80f).rotate(element.rotation)
                                IntOffset(
                                    (element.position.x + rotOffset.x).toInt() - with(density) { 12.dp.roundToPx() },
                                    (element.position.y + rotOffset.y).toInt() - with(density) { 12.dp.roundToPx() }
                                )
                            }
                    ) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00B4D8)),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = Color(0xFF00B4D8))
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                element.position.x.toInt() - with(density) { 24.dp.roundToPx() },
                                element.position.y.toInt() - with(density) { 24.dp.roundToPx() }
                            )
                        }
                        .graphicsLayer {
                            rotationZ = element.rotation
                        }
                        .pointerInput(element.id) {
                            detectTapGestures(
                                onTap = { selectedElementId = element.id },
                                onDoubleTap = { selectedElementForEdit = element }
                            )
                        }
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.dp, Color(0xFF00B4D8).copy(alpha = 0.5f), CircleShape)
                        )
                    }

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
