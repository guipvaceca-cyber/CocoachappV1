package com.example.coachapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

@Composable
fun StatTerrainTool(
    stats: Map<String, Int>,
    onStatClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00B4D8)
) {
    // Volleyball court: 18m x 9m. We draw the full court.
    // Opponent camp is at the top (Zones 1, 6, 5 back; 2, 3, 4 front).
    // Own camp is at the bottom.
    
    val zoneOrder = listOf(
        listOf("1", "6", "5"),
        listOf("2", "3", "4")
    )
    val subZoneLetters = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i")
    
    val scope = rememberCoroutineScope()
    val lastClickedZone = remember { mutableStateOf<String?>(null) }
    val flashAlpha = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(0.5f) // 9m width / 18m height (vertical court)
            .background(Color(0xFF0C2D48), RoundedCornerShape(4.dp))
            .border(2.dp, Color.White, RoundedCornerShape(4.dp))
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        val campHeight = height / 2
        val zoneWidth = width / 3
        
        // Front row is 3m deep, Back row is 6m deep. Total 9m.
        // So front row takes 1/3 of campHeight, back row takes 2/3.
        val frontRowHeight = campHeight * (3f / 9f)
        val backRowHeight = campHeight * (6f / 9f)
        
        val subZoneWidth = zoneWidth / 3
        val backSubZoneHeight = backRowHeight / 3
        val frontSubZoneHeight = frontRowHeight / 3

        val maxStats = stats.values.maxOrNull()?.coerceAtLeast(1) ?: 1

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Only interact with the top half (Opponent camp)
                        if (offset.y < campHeight) {
                            val col = (offset.x / zoneWidth).toInt().coerceIn(0, 2)
                            val isFrontRow = offset.y > backRowHeight
                            val row = if (isFrontRow) 1 else 0
                            val zoneNum = zoneOrder[row][col]

                            val localX = offset.x % zoneWidth
                            val localY = if (isFrontRow) offset.y - backRowHeight else offset.y
                            
                            val subCol = (localX / subZoneWidth).toInt().coerceIn(0, 2)
                            val subRowHeight = if (isFrontRow) frontSubZoneHeight else backSubZoneHeight
                            val subRow = (localY / subRowHeight).toInt().coerceIn(0, 2)
                            
                            val subIndex = subRow * 3 + subCol
                            val subLetter = subZoneLetters[subIndex]
                            
                            val zoneId = "$zoneNum$subLetter"
                            lastClickedZone.value = zoneId
                            scope.launch {
                                flashAlpha.snapTo(0.6f)
                                flashAlpha.animateTo(0f, tween(300))
                            }
                            onStatClick(zoneId)
                        }
                    }
                }
        ) {
            val lineCol = Color.White
            val netCol = Color.White.copy(alpha = 0.8f)

            // --- DRAW COURT LINES ---
            // Net (Center Line)
            drawLine(netCol, Offset(0f, campHeight), Offset(width, campHeight), strokeWidth = 8f)
            
            // Attack Lines (3m)
            // Opponent side
            drawLine(lineCol.copy(alpha = 0.6f), Offset(0f, backRowHeight), Offset(width, backRowHeight), strokeWidth = 3f)
            // Own side
            drawLine(lineCol.copy(alpha = 0.4f), Offset(0f, campHeight + frontRowHeight), Offset(width, campHeight + frontRowHeight), strokeWidth = 2f)

            // --- DRAW OPPONENT CAMP ZONES & STATS ---
            for (row in 0..1) {
                val zY = if (row == 0) 0f else backRowHeight
                val zH = if (row == 0) backRowHeight else frontRowHeight
                
                for (col in 0..2) {
                    val zoneNum = zoneOrder[row][col]
                    val zX = col * zoneWidth

                    // Sub-zones
                    for (subRow in 0..2) {
                        val sH = zH / 3
                        val sY = zY + subRow * sH
                        
                        for (subCol in 0..2) {
                            val sW = zoneWidth / 3
                            val sX = zX + subCol * sW
                            
                            val subIndex = subRow * 3 + subCol
                            val subLetter = subZoneLetters[subIndex]
                            val id = "$zoneNum$subLetter"
                            val count = stats[id] ?: 0

                            // Heatmap
                            if (count > 0) {
                                val intensity = (count.toFloat() / maxStats).coerceIn(0.1f, 1f)
                                drawRect(
                                    color = activeColor.copy(alpha = 0.2f + (intensity * 0.6f)),
                                    topLeft = Offset(sX, sY),
                                    size = Size(sW, sH)
                                )
                            }
                            
                            // Flash
                            if (lastClickedZone.value == id && flashAlpha.value > 0f) {
                                drawRect(
                                    color = Color.White.copy(alpha = flashAlpha.value),
                                    topLeft = Offset(sX, sY),
                                    size = Size(sW, sH)
                                )
                            }

                            // Sub-zone Border
                            drawRect(
                                color = Color.White.copy(alpha = 0.1f),
                                topLeft = Offset(sX, sY),
                                size = Size(sW, sH),
                                style = Stroke(width = 1f)
                            )

                            // Count text
                            if (count > 0) {
                                drawContext.canvas.nativeCanvas.apply {
                                    val paint = Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = (sH * 0.5f).coerceIn(10f, 28f)
                                        textAlign = Paint.Align.CENTER
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    }
                                    val text = count.toString()
                                    val bounds = Rect()
                                    paint.getTextBounds(text, 0, text.length, bounds)
                                    drawText(text, sX + sW / 2, sY + sH / 2 + bounds.height() / 2, paint)
                                }
                            }
                        }
                    }

                    // Zone Border
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(zX, zY),
                        size = Size(zoneWidth, zH),
                        style = Stroke(width = 2f)
                    )
                    
                    // Zone Num Label
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 40f
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            alpha = (255 * 0.15f).toInt()
                        }
                        drawText(zoneNum, zX + zoneWidth / 2, zY + zH / 2 + 15f, paint)
                    }
                }
            }
            
            // --- DRAW OWN CAMP (Context only) ---
            // Draw a subtle border for own camp zones just for perspective
            drawRect(
                color = Color.White.copy(alpha = 0.1f),
                topLeft = Offset(0f, campHeight),
                size = Size(width, campHeight),
                style = Stroke(width = 1f)
            )
            // Center own camp text
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                    alpha = (255 * 0.4f).toInt()
                }
                drawText("MON CAMP (CONTEXTE)", width / 2, campHeight + campHeight / 2, paint)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF001529)
@Composable
fun StatTerrainToolPreview() {
    val mockStats = mapOf(
        "1a" to 5,
        "6e" to 12,
        "4i" to 3,
        "3b" to 8
    )
    Box(modifier = Modifier.padding(16.dp)) {
        StatTerrainTool(
            stats = mockStats,
            onStatClick = {},
            modifier = Modifier.fillMaxWidth().height(300.dp)
        )
    }
}
