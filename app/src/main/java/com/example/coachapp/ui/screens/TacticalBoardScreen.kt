package com.example.coachapp.ui.screens

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
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun TacticalBoardScreen(
    modifier: Modifier = Modifier,
    persistenceManager: PersistenceManager
) {
    var boardMode by rememberSaveable { mutableStateOf("TRAINING") }
    val lines = remember { mutableStateListOf<BoardLine>() }
    var currentLinePoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color.Blue) }
    
    val elements = remember { mutableStateListOf<BoardElement>() }
    var selectedElementType by remember { mutableStateOf<ElementType?>(null) }
    
    // UI state
    var draggedElementId by remember { mutableStateOf<String?>(null) }
    var selectedElementForEdit by remember { mutableStateOf<BoardElement?>(null) }

    val courtColor = if (boardMode == "TRAINING") Color(0xFFE67E22) else Color(0xFF2980B9)
    val lineColor = Color.White
    
    val density = LocalDensity.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadList by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // --- TOP BAR ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(selected = boardMode == "TRAINING", onClick = { boardMode = "TRAINING" }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                    Text("Entraînement")
                }
                SegmentedButton(selected = boardMode == "MATCH", onClick = { boardMode = "MATCH" }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                    Text("Match")
                }
            }
            
            Row {
                IconButton(onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) }) {
                    Icon(Icons.Default.Undo, "Annuler")
                }
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Default.Save, "Sauvegarder")
                }
                IconButton(onClick = { showLoadList = true }) {
                    Icon(Icons.Default.FolderOpen, "Ouvrir")
                }
                IconButton(onClick = { lines.clear(); elements.clear() }) {
                    Icon(Icons.Default.DeleteForever, "Effacer")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- TOOLBAR ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Color.Blue, Color.Red, Color.Black, Color.Green).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(if (selectedColor == color && selectedElementType == null) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { selectedColor = color; selectedElementType = null }
                )
            }
            
            VerticalDivider(modifier = Modifier.height(32.dp))
            
            ElementType.entries.forEach { type ->
                IconButton(
                    onClick = { selectedElementType = type },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (selectedElementType == type) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(type.icon, null, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BOARD CANVAS ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(courtColor, RoundedCornerShape(8.dp))
                .border(2.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                .pointerInput(selectedElementType) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val hitElement = elements.find { (it.position - offset).getDistance() < 100f }
                            if (hitElement != null) {
                                draggedElementId = hitElement.id
                                selectedElementType = null 
                            } 
                            else if (selectedElementType != null) {
                                val newEl = BoardElement(UUID.randomUUID().toString(), selectedElementType!!, offset)
                                elements.add(newEl)
                                draggedElementId = newEl.id
                                selectedElementType = null // Switch to move mode immediately
                            } 
                            else {
                                draggedElementId = null
                                currentLinePoints = mutableStateListOf(offset)
                                lines.add(BoardLine(currentLinePoints.toList(), selectedColor))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (draggedElementId != null) {
                                val index = elements.indexOfFirst { it.id == draggedElementId }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = change.position)
                                }
                            } else if (selectedElementType == null && lines.isNotEmpty()) {
                                currentLinePoints.add(change.position)
                                lines[lines.size - 1] = lines.last().copy(points = currentLinePoints.toList())
                            }
                        },
                        onDragEnd = {
                            draggedElementId = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                drawLine(lineColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 8f)
                val attackLineOffset = h * 0.15f
                drawLine(lineColor, Offset(0f, h / 2 - attackLineOffset), Offset(w, h / 2 - attackLineOffset), strokeWidth = 4f)
                drawLine(lineColor, Offset(0f, h / 2 + attackLineOffset), Offset(w, h / 2 + attackLineOffset), strokeWidth = 4f)
                drawRect(lineColor, style = Stroke(width = 6f))

                lines.forEach { line ->
                    for (i in 0 until line.points.size - 1) {
                        drawLine(
                            color = line.color,
                            start = line.points[i],
                            end = line.points[i + 1],
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            elements.forEach { element ->
                val xDp = with(density) { element.position.x.toDp() } - 24.dp
                val yDp = with(density) { element.position.y.toDp() } - 24.dp
                
                Box(modifier = Modifier.offset(xDp, yDp).clickable { selectedElementForEdit = element }) {
                    if (element.type == ElementType.PLAYER) {
                        PlayerTacticalIcon(element)
                    } else {
                        Icon(
                            imageVector = element.type.icon,
                            contentDescription = null,
                            tint = element.type.color,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(0.7f), CircleShape)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
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
            onDelete = {
                elements.removeAll { it.id == selectedElementForEdit!!.id }
                selectedElementForEdit = null
            }
        )
    }

    if (showSaveDialog) {
        var boardName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Sauvegarder la planche") },
            text = { OutlinedTextField(value = boardName, onValueChange = { boardName = it }, label = { Text("Nom de la planche") }) },
            confirmButton = {
                Button(onClick = {
                    persistenceManager.saveTacticalBoard(boardName, lines.toList(), elements.toList())
                    showSaveDialog = false
                }) { Text("Sauvegarder") }
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
                if (boards.isEmpty()) {
                    Text("Aucune planche sauvegardée.")
                } else {
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

@Composable
fun PlayerTacticalIcon(element: BoardElement) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp).rotate(element.rotation)) {
                val r = size.minDimension / 2
                // Circle body
                drawCircle(Color.White, radius = r)
                drawCircle(element.type.color, radius = r, style = Stroke(width = 2.dp.toPx()))
                
                // Head
                drawCircle(element.type.color, radius = r * 0.3f, center = Offset(center.x, center.y))
                
                // Open arms to show orientation
                val armPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - r * 0.6f, center.y + r * 0.2f)
                    quadraticTo(center.x, center.y - r * 0.1f, center.x + r * 0.6f, center.y + r * 0.2f)
                }
                drawPath(armPath, color = element.type.color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                
                // Small dot to indicate "front"
                drawCircle(Color.Red, radius = 3.dp.toPx(), center = Offset(center.x, center.y - r * 0.7f))
            }
            if (element.label.isNotEmpty()) {
                Text(
                    text = element.label,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = element.type.color,
                    modifier = Modifier.background(Color.White.copy(0.8f), RoundedCornerShape(2.dp)).padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ElementEditDialog(
    element: BoardElement,
    onDismiss: () -> Unit,
    onUpdate: (BoardElement) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(element.label) }
    var rotation by remember { mutableFloatStateOf(element.rotation) }
    val roles = listOf("Pa", "PO", "C", "R4", "RA", "L", "")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paramètres") },
        text = {
            Column {
                if (element.type == ElementType.PLAYER) {
                    Text("Poste :", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        roles.take(4).forEach { role ->
                            FilterChip(selected = label == role, onClick = { label = role }, label = { Text(role) })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        roles.drop(4).forEach { role ->
                            FilterChip(selected = label == role, onClick = { label = role }, label = { Text(role.ifEmpty { "N/A" }) })
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Orientation :", style = MaterialTheme.typography.labelSmall)
                    Slider(value = rotation, onValueChange = { rotation = it }, valueRange = 0f..360f)
                }
                
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null)
                    Text("Supprimer l'objet")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onUpdate(element.copy(label = label, rotation = rotation)) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
