package com.example.coachapp.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.flashDiagnosticData
import com.example.coachapp.data.globalDiagnosticData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    flashScores: Map<String, Double>?,
    globalScores: Map<String, Double>?,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    // Combine labels from both diagnostics (8 axes)
    val flashDomains = flashDiagnosticData
    val globalDomains = globalDiagnosticData
    val allDomains = (flashDomains + globalDomains).distinctBy { it.id }
    val labels = allDomains.map { it.title }
    
    val numAxes = labels.size
    val angleBetweenAxes = 2 * PI / numAxes

    val colorFlash = Color(0xFF00B4D8) // Cyan/Blue
    val colorGlobal = Color(0xFFBD00FF) // Neon Purple
    val colorExpert = Color.White.copy(alpha = 0.15f)
    val colorLabel = Color.White
    val colorGrid = Color.White.copy(alpha = 0.1f)

    val textPaint = remember {
        Paint().apply {
            color = colorLabel.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f, 0f, 2f, Color.Black.copy(alpha = 0.5f).toArgb())
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 * 0.62f
            
            textPaint.textSize = 9.sp.toPx()

            // 1. Draw Expert Target (Back layer)
            drawRadarLayer(
                data = List(numAxes) { 0.9f }, // Target 4.5/5
                radius = radius,
                centerX = centerX,
                centerY = centerY,
                angleBetweenAxes = angleBetweenAxes,
                color = colorExpert,
                progress = 1f, // Static
                isDashed = true
            )

            // 2. Draw background grid (Concentric polygons)
            for (level in 1..5) {
                val levelRadius = radius * (level / 5f)
                val path = Path()
                for (i in 0 until numAxes) {
                    val angle = i * angleBetweenAxes - PI / 2
                    val x = centerX + levelRadius * cos(angle).toFloat()
                    val y = centerY + levelRadius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = colorGrid, style = Stroke(width = 1.dp.toPx()))
            }

            // 3. Draw axes and labels
            for (i in 0 until numAxes) {
                val angle = i * angleBetweenAxes - PI / 2
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                drawLine(color = colorGrid, start = Offset(centerX, centerY), end = Offset(x, y), strokeWidth = 1.dp.toPx())
                
                drawContext.canvas.nativeCanvas.apply {
                    val labelRadius = radius + 25.dp.toPx()
                    val labelX = centerX + labelRadius * cos(angle).toFloat()
                    val labelY = centerY + labelRadius * sin(angle).toFloat()
                    
                    // Adjust vertical alignment for top/bottom labels
                    val yOffset = if (angle > 0 && angle < PI) 10.dp.toPx() else -5.dp.toPx()
                    
                    drawText(labels[i].uppercase().take(12), labelX, labelY + yOffset, textPaint)
                }
            }

            // 4. GLOBAL LAYER (Purple)
            if (globalScores != null) {
                val globalData = allDomains.map { domain ->
                    val score = globalScores[domain.id] ?: 0.0
                    (score.toFloat() / 5f).coerceIn(0f, 1f)
                }
                drawRadarLayer(globalData, radius, centerX, centerY, angleBetweenAxes, colorGlobal, progress.value)
            }

            // 5. FLASH LAYER (Cyan)
            if (flashScores != null) {
                val flashData = allDomains.map { domain ->
                    val score = flashScores[domain.id] ?: 0.0
                    (score.toFloat() / 5f).coerceIn(0f, 1f)
                }
                drawRadarLayer(flashData, radius, centerX, centerY, angleBetweenAxes, colorFlash, progress.value)
            }
        }
        
        // Simple Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("SESSION", colorFlash)
            Spacer(Modifier.width(16.dp))
            LegendItem("GLOBAL", colorGlobal)
            Spacer(Modifier.width(16.dp))
            LegendItem("CIBLE", colorExpert, isDashed = true)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            if (isDashed) {
                drawRect(color = color, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)))
            } else {
                drawCircle(color = color)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadarLayer(
    data: List<Float>,
    radius: Float,
    centerX: Float,
    centerY: Float,
    angleBetweenAxes: Double,
    color: Color,
    progress: Float,
    isDashed: Boolean = false
) {
    val path = Path()
    val points = mutableListOf<Offset>()
    
    data.forEachIndexed { i, targetValue ->
        val value = targetValue * progress
        val angle = i * angleBetweenAxes - PI / 2
        val dataRadius = radius * value
        val x = centerX + dataRadius * cos(angle).toFloat()
        val y = centerY + dataRadius * sin(angle).toFloat()
        val offset = Offset(x, y)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        points.add(offset)
    }
    path.close()
    
    if (!isDashed) {
        // Gradient Fill
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f)),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            style = Fill
        )
        // Solid Border
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        
        // Dots at vertices
        points.forEach { point ->
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = point)
            drawCircle(color = color, radius = 2.dp.toPx(), center = point)
        }
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )
    }
}
