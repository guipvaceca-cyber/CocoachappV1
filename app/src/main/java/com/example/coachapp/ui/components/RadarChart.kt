package com.example.coachapp.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) { animationPlayed = true }

    // Combine labels from both diagnostics (8 axes)
    val flashDomains = flashDiagnosticData
    val globalDomains = globalDiagnosticData
    val allDomains = flashDomains + globalDomains
    val labels = allDomains.map { it.title }
    
    val numAxes = labels.size
    val angleBetweenAxes = 2 * PI / numAxes

    val colorFlash = Color(0xFF2196F3) // Blue
    val colorGlobal = Color(0xFF9C27B0) // Purple
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val colorOutline = MaterialTheme.colorScheme.outlineVariant

    val textPaint = remember(colorOnSurface) {
        Paint().apply {
            color = colorOnSurface.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.65f
        
        textPaint.textSize = 8.sp.toPx()

        // Draw background grid
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
            drawPath(path, color = colorOutline, style = Stroke(width = 0.5.dp.toPx()))
        }

        // Draw axes and labels
        for (i in 0 until numAxes) {
            val angle = i * angleBetweenAxes - PI / 2
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            drawLine(color = colorOutline, start = androidx.compose.ui.geometry.Offset(centerX, centerY), end = androidx.compose.ui.geometry.Offset(x, y), strokeWidth = 0.5.dp.toPx())
            
            drawContext.canvas.nativeCanvas.apply {
                val labelRadius = radius + 20.dp.toPx()
                val labelX = centerX + labelRadius * cos(angle).toFloat()
                val labelY = centerY + labelRadius * sin(angle).toFloat()
                drawText(labels[i].take(8), labelX, labelY, textPaint)
            }
        }

        // --- GLOBAL LAYER (BACK) ---
        if (globalScores != null) {
            val globalData = allDomains.map { domain ->
                if (globalDomains.any { it.id == domain.id }) (globalScores[domain.id] ?: 0.0).toFloat() / 5f else 0f
            }
            drawRadarLayer(globalData, radius, centerX, centerY, angleBetweenAxes, colorGlobal, animationPlayed)
        }

        // --- FLASH LAYER (FRONT) ---
        if (flashScores != null) {
            val flashData = allDomains.map { domain ->
                if (flashDomains.any { it.id == domain.id }) (flashScores[domain.id] ?: 0.0).toFloat() / 5f else 0f
            }
            drawRadarLayer(flashData, radius, centerX, centerY, angleBetweenAxes, colorFlash, animationPlayed)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadarLayer(
    data: List<Float>,
    radius: Float,
    centerX: Float,
    centerY: Float,
    angleBetweenAxes: Double,
    color: Color,
    animate: Boolean
) {
    val path = Path()
    data.forEachIndexed { i, targetValue ->
        val value = if (animate) targetValue else 0f
        val angle = i * angleBetweenAxes - PI / 2
        val dataRadius = radius * value
        val x = centerX + dataRadius * cos(angle).toFloat()
        val y = centerY + dataRadius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    
    drawPath(path, color = color.copy(alpha = 0.2f), style = Fill)
    drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
}
