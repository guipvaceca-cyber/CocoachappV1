package com.example.coachapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TerrainLaunchScreen(onAnimationFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    
    // -- Anim States --
    val ballScale = remember { Animatable(0f) }
    val ballRotate = remember { Animatable(-25f) }
    val flashAlpha = remember { Animatable(0f) }
    val textY = remember { Animatable(-130f) }
    val textRotateX = remember { Animatable(95f) }
    val textAlpha = remember { Animatable(0f) }
    val linesScale = remember { Animatable(0f) }
    val arcsProgress = remember { Animatable(0f) }
    val shockwaveScales = List(3) { remember { Animatable(1f) } }
    val shockwaveAlphas = List(3) { remember { Animatable(1f) } }

    // -- Lifecycle & Orchestration --
    LaunchedEffect(Unit) {
        // 0.2s : Burst & Ball start
        delay(200)
        scope.launch { 
            ballScale.animateTo(1.55f, tween(350, easing = EaseOut))
            ballScale.animateTo(1f, tween(300, easing = EaseOut))
        }
        scope.launch {
            ballRotate.animateTo(12f, tween(350))
            ballRotate.animateTo(0f, tween(300))
        }

        // 0.22s : Flash & Shockwaves
        delay(20)
        scope.launch {
            flashAlpha.animateTo(0.75f, tween(70))
            flashAlpha.animateTo(0f, tween(210))
        }
        
        val ringDelays = listOf(0L, 140L, 280L)
        shockwaveScales.forEachIndexed { i, anim ->
            scope.launch {
                delay(ringDelays[i])
                anim.animateTo(7f, tween(750, easing = EaseOut))
            }
            scope.launch {
                delay(ringDelays[i])
                shockwaveAlphas[i].animateTo(0f, tween(750, easing = EaseOut))
            }
        }

        // 0.68s : Letters start falling
        delay(440)
        scope.launch {
            textAlpha.animateTo(1f, tween(300))
            textY.animateTo(0f, tween(520, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)))
            textRotateX.animateTo(0f, tween(520, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)))
        }

        // 0.85s : Court lines
        delay(170)
        scope.launch { linesScale.animateTo(1f, tween(700, easing = EaseOut)) }
        scope.launch { arcsProgress.animateTo(1f, tween(600, easing = EaseOut)) }

        // Final exit
        delay(1500)
        onAnimationFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        // --- Court Grid ---
        CourtGrid(linesScale.value)

        // --- Shockwaves ---
        shockwaveScales.forEachIndexed { i, scale ->
            Canvas(modifier = Modifier.size(90.dp)) {
                drawCircle(
                    color = Color(0xFF1A56DB).copy(alpha = shockwaveAlphas[i].value * (0.8f - i * 0.2f)),
                    radius = (size.minDimension / 2) * scale.value,
                    style = Stroke(width = if (i == 0) 2.dp.toPx() else 1.5.dp.toPx())
                )
            }
        }

        // --- Volleyball ---
        Box(
            modifier = Modifier
                .offset(y = (-80).dp)
                .scale(ballScale.value)
                .graphicsLayer(rotationZ = ballRotate.value),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .alpha(0.35f * ballScale.value)
                    .background(Color(0xFF1A56DB), CircleShape)
                    .blur(40.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.cocoachiconvect),
                contentDescription = null,
                modifier = Modifier.size(148.dp),
                contentScale = ContentScale.Fit
            )
        }

        // --- "TERRAIN" ---
        Column(
            modifier = Modifier
                .offset(y = 10.dp)
                .alpha(textAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.graphicsLayer { 
                rotationX = textRotateX.value
                translationY = textY.value
                cameraDistance = 8 * density
            }) {
                Text(
                    text = "TERRAIN",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displayLarge.copy(
                        shadow = Shadow(
                            color = Color(0xFFF5C400).copy(alpha = 0.7f),
                            blurRadius = 40f
                        )
                    )
                )
            }
            
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(3.dp)
                    .width(260.dp * linesScale.value)
                    .background(Color(0xFFF5C400), CircleShape)
            )
        }

        // --- Flash ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = flashAlpha.value))
        )
    }
}

@Composable
fun CourtGrid(scale: Float) {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f * scale)) {
        val w = size.width
        val h = size.height
        drawLine(Color.White, Offset(0f, h * 0.42f), Offset(w, h * 0.42f), strokeWidth = 1.dp.toPx())
        drawLine(Color.White, Offset(w * 0.12f, h * 0.3f), Offset(w * 0.88f, h * 0.3f), strokeWidth = 1.dp.toPx())
        drawLine(Color.White, Offset(w * 0.12f, h * 0.54f), Offset(w * 0.88f, h * 0.54f), strokeWidth = 1.dp.toPx())
    }
}
