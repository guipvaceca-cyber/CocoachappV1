package com.example.coachapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.coachapp.data.SeasonConfig
import com.example.coachapp.data.Team
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// ----------------------------------------------------------------
// Shared Models & Constants
// ----------------------------------------------------------------

data class SeasonCycle(
    val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val label: String,
    val theme: String,
    val dateDebut: LocalDate,
    val dateFin: LocalDate,
    val notes: String = ""
)

val THEMES_VOLLEY = listOf(
    Triple("fondamentaux",  "Fondamentaux",     Color(0xFF85B7EB)),
    Triple("service_recep", "Service / Récep",  Color(0xFF5DCAA5)),
    Triple("systeme_jeu",   "Systèmes de jeu",  Color(0xFFEF9F27)),
    Triple("bloc_attaque",  "Bloc / Attaque",   Color(0xFFED93B1)),
    Triple("jeu_reduit",    "Jeu réduit",       Color(0xFF97C459)),
    Triple("evaluation",    "Évaluation",       Color(0xFFAFA9EC)),
)

fun themeColor(theme: String): Color =
    THEMES_VOLLEY.find { it.first == theme }?.third ?: Color.Gray

fun themeLabel(theme: String): String =
    THEMES_VOLLEY.find { it.first == theme }?.second ?: theme

// ----------------------------------------------------------------
// Season Planner View (Gantt)
// ----------------------------------------------------------------

@Composable
fun SeasonPlannerView(
    config: SeasonConfig,
    selectedTeamId: String?,
    cycles: List<SeasonCycle>,
    onCyclesUpdated: (List<SeasonCycle>) -> Unit
) {
    var localCycles by remember(selectedTeamId, cycles) { mutableStateOf(cycles) }
    var showAddCycle by remember { mutableStateOf(false) }
    var cycleAEditer by remember { mutableStateOf<SeasonCycle?>(null) }

    val debutSaison = remember { 
        LocalDate.of(LocalDate.now().year, 9, 1)
            .let { if (LocalDate.now().monthValue < 9) it.minusYears(1) else it } 
    }
    val finSaison = remember { debutSaison.plusMonths(10) }

    val semaines = remember(debutSaison, finSaison) {
        val list = mutableListOf<LocalDate>()
        var cur = debutSaison
        while (!cur.isAfter(finSaison)) {
            list.add(cur)
            cur = cur.plusWeeks(1)
        }
        list
    }

    val cyclesFiltres = if (selectedTeamId == null) localCycles
    else localCycles.filter { it.teamId == selectedTeamId }

    val seancesValidees = remember(config, selectedTeamId) {
        config.plannedTrainings.filter {
            (selectedTeamId == null || it.teamId == selectedTeamId) && it.isValidated
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Planification de saison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "${debutSaison.year} → ${finSaison.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Button(
                onClick = { showAddCycle = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nouveau cycle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LegendThemes()

        Spacer(Modifier.height(20.dp))

        // Gantt Chart Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            val scrollState = rememberScrollState()
            val semWidth = 38.dp
            val labelWidth = 72.dp

            Column(modifier = Modifier.horizontalScroll(scrollState)) {
                Row {
                    Spacer(Modifier.width(labelWidth))
                    MoisHeader(semaines, semWidth)
                }

                Spacer(Modifier.height(8.dp))

                GanttRow("Cycles", labelWidth, semWidth, semaines) { sem: LocalDate ->
                    cyclesFiltres.find { !sem.isBefore(it.dateDebut) && !sem.isAfter(it.dateFin) }?.let { cycle ->
                        val isStart = sem == semaines.firstOrNull { !it.isBefore(cycle.dateDebut) && !it.isAfter(cycle.dateFin) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .clickable { cycleAEditer = cycle },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isStart) {
                                Text(cycle.label, fontSize = 8.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                GanttRow("Thèmes", labelWidth, semWidth, semaines) { sem ->
                    cyclesFiltres.find { !sem.isBefore(it.dateDebut) && !sem.isAfter(it.dateFin) }?.let { cycle ->
                        val color = themeColor(cycle.theme)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                GanttRow("Séances", labelWidth, semWidth, semaines) { sem ->
                    val sSem = seancesValidees.filter { !it.date.isBefore(sem) && !it.date.isAfter(sem.plusDays(6)) }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(minOf(sSem.size, 3)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF00B4D8), CircleShape))
                            Spacer(Modifier.width(2.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        if (cyclesFiltres.isNotEmpty()) StatsEquilibre(cyclesFiltres, semaines)
        Spacer(Modifier.height(100.dp))
    }

    if (showAddCycle || cycleAEditer != null) {
        CycleDialog(
            cycle = cycleAEditer,
            teams = config.teams,
            selectedTeamId = selectedTeamId,
            onDismiss = { showAddCycle = false; cycleAEditer = null },
            onConfirm = { nouveau ->
                localCycles = if (cycleAEditer != null) {
                    localCycles.map { if (it.id == nouveau.id) nouveau else it }
                } else {
                    localCycles + nouveau
                }
                onCyclesUpdated(localCycles)
                showAddCycle = false
                cycleAEditer = null
            },
            onDelete = { id ->
                localCycles = localCycles.filter { it.id != id }
                onCyclesUpdated(localCycles)
                cycleAEditer = null
            }
        )
    }
}

@Composable
fun MoisHeader(semaines: List<LocalDate>, semWidth: androidx.compose.ui.unit.Dp) {
    var dernierMois = -1
    Row {
        semaines.forEach { sem ->
            if (sem.monthValue != dernierMois) {
                Box(modifier = Modifier.width(semWidth).height(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        sem.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)).uppercase(),
                        fontSize = 10.sp,
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.Black
                    )
                }
                dernierMois = sem.monthValue
            } else {
                Spacer(Modifier.width(semWidth))
            }
        }
    }
}

@Composable
fun GanttRow(
    label: String,
    labelWidth: androidx.compose.ui.unit.Dp,
    semWidth: androidx.compose.ui.unit.Dp,
    semaines: List<LocalDate>,
    cellContent: @Composable (LocalDate) -> Unit
) {
    Row(modifier = Modifier.height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(labelWidth),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        semaines.forEach {
            Box(modifier = Modifier.width(semWidth).fillMaxHeight()) {
                cellContent(it)
            }
        }
    }
}

@Composable
fun LegendThemes() {
    THEMES_VOLLEY.chunked(3).forEach { ligne ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ligne.forEach { (_, label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(10.dp).background(color.copy(alpha = 0.6f), RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                }
            }
            repeat(3 - ligne.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun StatsEquilibre(cycles: List<SeasonCycle>, semaines: List<LocalDate>) {
    val score = mutableMapOf<String, Int>()
    semaines.forEach { sem ->
        cycles.find { !sem.isBefore(it.dateDebut) && !sem.isAfter(it.dateFin) }?.let {
            score[it.theme] = (score[it.theme] ?: 0) + 1
        }
    }
    val total = score.values.sum().takeIf { it > 0 } ?: 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("ÉQUILIBRE THÉMATIQUE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
            Spacer(Modifier.height(20.dp))
            score.entries.sortedByDescending { it.value }.forEach { (theme, count) ->
                val pct = count * 100 / total
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(themeLabel(theme), fontSize = 12.sp, modifier = Modifier.width(120.dp), color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                        color = themeColor(theme),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Text("$pct%", fontSize = 11.sp, modifier = Modifier.width(40.dp).padding(start = 8.dp), color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CycleDialog(
    cycle: SeasonCycle?,
    teams: List<Team>,
    selectedTeamId: String?,
    onDismiss: () -> Unit,
    onConfirm: (SeasonCycle) -> Unit,
    onDelete: (String) -> Unit
) {
    var label by remember { mutableStateOf(cycle?.label ?: "") }
    var theme by remember { mutableStateOf(cycle?.theme ?: THEMES_VOLLEY.first().first) }
    var teamId by remember { mutableStateOf(cycle?.teamId ?: selectedTeamId ?: teams.firstOrNull()?.id ?: "") }
    var dateDebut by remember { mutableStateOf(cycle?.dateDebut ?: LocalDate.now()) }
    var dateFin by remember { mutableStateOf(cycle?.dateFin ?: LocalDate.now().plusWeeks(3)) }
    var notes by remember { mutableStateOf(cycle?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF002147)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (cycle != null) "MODIFIER LE CYCLE" else "NOUVEAU CYCLE",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom du cycle", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00B4D8), unfocusedBorderColor = Color.White.copy(alpha = 0.2f))
                )
                Spacer(Modifier.height(20.dp))
                Text("THÈME DU CYCLE", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                THEMES_VOLLEY.forEach { (key, lbl, col) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { theme = key }.padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = theme == key, onClick = { theme = key }, colors = RadioButtonDefaults.colors(selectedColor = col, unselectedColor = Color.White.copy(alpha = 0.3f)))
                        Box(modifier = Modifier.size(12.dp).background(col, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(lbl, color = Color.White, fontSize = 15.sp, fontWeight = if (theme == key) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (cycle != null) {
                        TextButton(onClick = { onDelete(cycle.id) }) { Text("Supprimer", color = Color.Red, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Annuler", color = Color.White.copy(alpha = 0.6f)) }
                    Button(
                        onClick = {
                            onConfirm(SeasonCycle(id = cycle?.id ?: UUID.randomUUID().toString(), teamId = teamId, label = label, theme = theme, dateDebut = dateDebut, dateFin = dateFin, notes = notes))
                        },
                        enabled = label.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
