package com.example.coachapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*

// ============================================================
// Couleurs cyberpunk
// ============================================================
private val BgBlack     = Color(0xFF000000)
private val BgDark      = Color(0xFF080808)
private val BgCard      = Color(0xFF111111)
private val BorderDark  = Color(0xFF1E1E1E)
private val BorderMid   = Color(0xFF2A2A2A)
private val CyanPrimary = Color(0xFF00FFFF)
private val CyanDim     = Color(0xFF00AACC)
private val OrangeSoft  = Color(0xFFFF9900)
private val GreenDone   = Color(0xFF00FF66)
private val TextPrimary = Color(0xFFBBBBBB)
private val TextMuted   = Color(0xFF555555)
private val TextDim     = Color(0xFF444444)
private val DotWhite    = Color(0x46FFFFFF)

// ============================================================
// Modèle de données UI
// ============================================================
data class AnalyseSeanceUiState(
    val isLoading: Boolean = true,
    val resumeSeance: String = "",
    val motifScore: String = "",
    val scoreGlobal: Int = 0,
    val scoreCharge: Int = 0,
    val scoreProgression: Int = 0,
    val scoreContenu: Int = 0,
    val niveauAlerte: String = "vert",
    val titreSeance: String = "",
    val dureeMinutes: Int = 0,
    val dateLabel: String = "",
    val blocsTypes: List<String> = emptyList(),
    val nbPresents: Int = 0,
    val nbAbsents: Int = 0,
    val scoreMoyFenetre: Float = 0f,
    val deltaFenetre: Float = 0f,
)

// ============================================================
// Drawer principal
// ============================================================
@Composable
fun AnalyseSeanceDrawer(
    state: AnalyseSeanceUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetX = remember { Animatable(-320f) }

    LaunchedEffect(Unit) {
        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss)
        )

        // Drawer
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .offset { IntOffset(offsetX.value.roundToInt().dp.roundToPx(), 0) }
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .background(BgDark)
                .border(
                    width = 0.5.dp,
                    color = BorderMid,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Zone canvas cyberpunk
                AnalyseVizZone(
                    isLoading = state.isLoading,
                    dateLabel = state.dateLabel,
                    dureeMinutes = state.dureeMinutes,
                    onClose = onDismiss,
                )

                // Contenu résultats
                if (!state.isLoading) {
                    AnalyseResultsZone(state = state)
                }
            }
        }
    }
}

// ============================================================
// Zone visualiseur canvas
// ============================================================
@Composable
private fun AnalyseVizZone(
    isLoading: Boolean,
    dateLabel: String,
    dureeMinutes: Int,
    onClose: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    )

    // Barres : hauteurs aléatoires stabilisées en Gauss quand done
    val barCount = 28
    val barHeights = remember { mutableStateListOf(*Array(barCount) { 4f + Math.random().toFloat() * 20f }) }
    val barTargets = remember { mutableStateListOf(*Array(barCount) { 4f + Math.random().toFloat() * 20f }) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                delay(120)
                for (i in 0 until barCount) {
                    if (abs(barHeights[i] - barTargets[i]) < 1f) {
                        barTargets[i] = 4f + Math.random().toFloat() * 55f
                    }
                    barHeights[i] += (barTargets[i] - barHeights[i]) * 0.15f
                }
            }
        } else {
            // Converge vers courbe de Gauss
            val center = barCount / 2f
            for (i in 0 until barCount) {
                val dist = abs(i - center) / (barCount / 2f)
                barTargets[i] = 15f + (1f - dist) * 45f + Math.random().toFloat() * 8f
            }
            while (true) {
                delay(30)
                var allDone = true
                for (i in 0 until barCount) {
                    if (abs(barHeights[i] - barTargets[i]) > 0.5f) {
                        barHeights[i] += (barTargets[i] - barHeights[i]) * 0.1f
                        allDone = false
                    }
                }
                if (allDone) break
            }
        }
    }

    // Pulse du dot status
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(BgBlack)
            .drawBehind {
                drawDotGrid(this)
                drawSpectrumBars(this, barHeights, isLoading, time)
            }
    ) {
        // Bouton fermer
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp),
        ) {
            Text("✕", color = TextMuted, fontSize = 14.sp)
        }

        // Status row
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isLoading) CyanPrimary.copy(alpha = dotAlpha)
                            else GreenDone
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isLoading) "Analyse en cours" else "Analyse terminée",
                    color = if (isLoading) CyanPrimary else GreenDone,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.04.sp,
                )
            }
            Text(
                text = "$dateLabel · ${dureeMinutes} min",
                color = TextDim,
                fontSize = 10.sp,
            )
        }
    }
}

// ============================================================
// Draw : grille de points
// ============================================================
private fun drawDotGrid(scope: DrawScope) {
    val cols = 30
    val rows = 12
    val gx = scope.size.width / cols
    val gy = (scope.size.height - 30f) / rows
    for (c in 0 until cols) {
        for (r in 0 until rows) {
            scope.drawCircle(
                color = DotWhite,
                radius = 1f,
                center = Offset((c + 0.5f) * gx, (r + 0.5f) * gy),
            )
        }
    }
}

// ============================================================
// Draw : barres spectre
// ============================================================
private fun drawSpectrumBars(
    scope: DrawScope,
    heights: List<Float>,
    isLoading: Boolean,
    time: Float,
) {
    val nb = heights.size
    val availW = scope.size.width - 16f
    val bw = (availW / nb) - 2f
    val baseY = scope.size.height - 30f

    heights.forEachIndexed { i, h ->
        val hh = h.coerceAtLeast(3f)
        val x = 8f + i * (availW / nb)
        val y = baseY - hh

        val color = if (isLoading) {
            val phase = ((time / 800f + i * 0.3f) % 1f)
            val intensity = 0.5f + 0.5f * sin(phase * PI.toFloat() * 2)
            Color(0f, 0.7f + 0.3f * intensity, 1f, 0.75f)
        } else {
            val ratio = (hh / 60f).coerceIn(0f, 1f)
            Color(1f - ratio, (0.6f + 0.4f * ratio).coerceAtMost(1f), 1f, 0.85f)
        }

        scope.drawRect(color = color, topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(bw, hh))
    }
}

// ============================================================
// Zone résultats
// ============================================================
@Composable
private fun AnalyseResultsZone(state: AnalyseSeanceUiState) {
    var detailOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Résumé
        AnimatedFadeBlock(delayMs = 100) {
            Text(
                text = state.resumeSeance,
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 19.sp,
            )
        }

        // Motif
        AnimatedFadeBlock(delayMs = 200) {
            DarkCard {
                Column {
                    Text("MOTIF", color = TextDim, fontSize = 10.sp, letterSpacing = 0.06.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(state.motifScore, color = Color(0xFF888888), fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
        }

        // Scores
        AnimatedFadeBlock(delayMs = 300) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ScoreRow("Charge", state.scoreCharge, delayMs = 500)
                ScoreRow("Progression", state.scoreProgression, delayMs = 650)
                ScoreRow("Contenu", state.scoreContenu, delayMs = 800)
            }
        }

        // Bouton détail
        AnimatedFadeBlock(delayMs = 500) {
            OutlinedButton(
                onClick = { detailOpen = !detailOpen },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                border = BorderStroke(0.5.dp, BorderMid),
            ) {
                Text(
                    text = if (detailOpen) "Réduire ↑" else "Voir le détail ↓",
                    fontSize = 11.sp,
                    letterSpacing = 0.04.sp,
                )
            }
        }

        // Détail expandable
        if (detailOpen) {
            AnimatedFadeBlock(delayMs = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    // Blocs
                    DarkCard {
                        Column {
                            Text("BLOCS", color = TextDim, fontSize = 10.sp, letterSpacing = 0.06.sp)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(state.blocsTypes)
                        }
                    }
                    // Présences
                    DarkCard {
                        Column {
                            Text("PRÉSENCES", color = TextDim, fontSize = 10.sp, letterSpacing = 0.06.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${state.nbPresents} présents · ${state.nbAbsents} absents", color = Color(0xFF888888), fontSize = 11.sp)
                        }
                    }
                    // Fenêtre glissante
                    DarkCard {
                        Column {
                            Text("FENÊTRE GLISSANTE", color = TextDim, fontSize = 10.sp, letterSpacing = 0.06.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${state.scoreMoyFenetre.roundToInt()}", color = CyanPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                val delta = state.deltaFenetre
                                val sign = if (delta >= 0) "+" else ""
                                val col = if (delta >= 0) Color(0xFF33BB66) else OrangeSoft
                                Text("${sign}${delta.roundToInt()} vs 4 dernières séances", color = col, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Composables utilitaires
// ============================================================
@Composable
private fun ScoreRow(label: String, score: Int, delayMs: Int) {
    val isWarn = score < 65
    val barColor = if (isWarn) OrangeSoft else CyanPrimary
    val animatedWidth by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 900, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "bar_$label",
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = TextMuted, fontSize = 11.sp)
            Text("$score", color = if (isWarn) OrangeSoft else CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun DarkCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(0.5.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(9.dp, 9.dp),
    ) { content() }
}

@Composable
private fun FlowRow(items: List<String>) {
    val cyan = listOf("Échauffement", "Situation", "Jeu", "Bloc")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            val isMissing = item.contains("✗")
            val bg = if (isMissing) Color(0x1AFF9900) else Color(0x1A00FFFF)
            val fg = if (isMissing) OrangeSoft else CyanPrimary
            val border = if (isMissing) Color(0x33FF9900) else Color(0x3300FFFF)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bg)
                    .border(0.5.dp, border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(item, color = fg, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AnimatedFadeBlock(delayMs: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "fade",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 8f,
        animationSpec = tween(400),
        label = "slide",
    )
    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha; translationY = offsetY }
    ) { content() }
}
