package com.example.coachapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

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
    THEMES_VOLLEY.find { it.first == theme }?.third ?: Color(0xFFCCCCCC)

fun themeLabel(theme: String): String =
    THEMES_VOLLEY.find { it.first == theme }?.second ?: theme

@Composable
fun SeasonPlannerView(
    config: SeasonConfig,
    selectedTeamId: String?,
    cycles: List<SeasonCycle> = emptyList(),
    onCyclesUpdated: (List<SeasonCycle>) -> Unit = {}
) {
    var localCycles by remember(selectedTeamId) { mutableStateOf(cycles) }
    var showAddCycle by remember { mutableStateOf(false) }
    var cycleAEditer by remember { mutableStateOf<SeasonCycle?>(null) }

    val debutSaison = remember { LocalDate.of(LocalDate.now().year, 9, 1)
        .let { if (LocalDate.now().monthValue < 9) it.minusYears(1) else it } }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Planification de saison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${debutSaison.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH))} → " +
                            "${finSaison.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { showAddCycle = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nouveau cycle", fontSize = 13.sp)
            }
        }

        LegendThemes()
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            val scrollState = rememberScrollState()
            val semWidth = 36.dp
            val labelWidth = 72.dp

            Column(
                modifier = Modifier.horizontalScroll(scrollState)
            ) {
                Row {
                    Spacer(Modifier.width(labelWidth))
                    MoisHeader(semaines, semWidth)
                }

                Spacer(Modifier.height(4.dp))

                GanttRow(
                    label = "Cycles",
                    labelWidth = labelWidth,
                    semWidth = semWidth,
                    semaines = semaines
                ) { semaine ->
                    val cycle = cyclesFiltres.find {
                        !semaine.isBefore(it.dateDebut) && !semaine.isAfter(it.dateFin)
                    }
                    if (cycle != null) {
                        val estDebut = semaine == semaines.firstOrNull {
                            !it.isBefore(cycle.dateDebut) && !it.isAfter(cycle.dateFin)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { cycleAEditer = cycle },
                            contentAlignment = Alignment.Center
                        ) {
                            if (estDebut) {
                                Text(
                                    cycle.label,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(Color.Transparent)
                                .clickable { showAddCycle = true }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                GanttRow(
                    label = "Thèmes",
                    labelWidth = labelWidth,
                    semWidth = semWidth,
                    semaines = semaines
                ) { semaine ->
                    val cycle = cyclesFiltres.find {
                        !semaine.isBefore(it.dateDebut) && !semaine.isAfter(it.dateFin)
                    }
                    if (cycle != null) {
                        val color = themeColor(cycle.theme)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                GanttRow(
                    label = "Séances",
                    labelWidth = labelWidth,
                    semWidth = semWidth,
                    semaines = semaines
                ) { semaine ->
                    val finSemaine = semaine.plusDays(6)
                    val seancesSemaine = seancesValidees.filter {
                        !it.date.isBefore(semaine) && !it.date.isAfter(finSemaine)
                    }
                    val cycle = cyclesFiltres.find {
                        !semaine.isBefore(it.dateDebut) && !semaine.isAfter(it.dateFin)
                    }
                    val color = if (cycle != null) themeColor(cycle.theme) else Color(0xFF888888)

                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(minOf(seancesSemaine.size, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(color, RoundedCornerShape(50))
                            )
                            Spacer(Modifier.width(2.dp))
                        }
                        if (seancesSemaine.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        if (cyclesFiltres.isNotEmpty()) {
            StatsEquilibre(cyclesFiltres, semaines)
        }
    }

    if (showAddCycle || cycleAEditer != null) {
        CycleDialog(
            cycle = cycleAEditer,
            teams = config.teams,
            selectedTeamId = selectedTeamId,
            onDismiss = {
                showAddCycle = false
                cycleAEditer = null
            },
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
        semaines.forEach { semaine ->
            val mois = semaine.monthValue
            Box(
                modifier = Modifier.width(semWidth).height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (mois != dernierMois) {
                    Text(
                        semaine.format(DateTimeFormatter.ofPattern("MMM", Locale.FRENCH))
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    dernierMois = mois
                }
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
    Row(
        modifier = Modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        semaines.forEach { semaine ->
            Box(modifier = Modifier.width(semWidth).fillMaxHeight()) {
                cellContent(semaine)
            }
        }
    }
}

@Composable
fun LegendThemes() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        THEMES_VOLLEY.forEach { (_, label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(3.dp))
                Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatsEquilibre(cycles: List<SeasonCycle>, semaines: List<LocalDate>) {
    val scoreParTheme = mutableMapOf<String, Int>()
    semaines.forEach { semaine ->
        val cycle = cycles.find {
            !semaine.isBefore(it.dateDebut) && !semaine.isAfter(it.dateFin)
        }
        if (cycle != null) {
            scoreParTheme[cycle.theme] = (scoreParTheme[cycle.theme] ?: 0) + 1
        }
    }
    val total = scoreParTheme.values.sum().takeIf { it > 0 } ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Équilibre thématique",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            scoreParTheme.entries.sortedByDescending { it.value }.forEach { (theme, semaines) ->
                val pct = semaines * 100 / total
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        themeLabel(theme),
                        fontSize = 11.sp,
                        modifier = Modifier.width(110.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = themeColor(theme),
                        trackColor = themeColor(theme).copy(alpha = 0.15f)
                    )
                    Text(
                        "$pct%",
                        fontSize = 10.sp,
                        modifier = Modifier.width(32.dp).padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CycleDialog(
    cycle: SeasonCycle?,
    teams: List<com.example.coachapp.data.Team>,
    selectedTeamId: String?,
    onDismiss: () -> Unit,
    onConfirm: (SeasonCycle) -> Unit,
    onDelete: (String) -> Unit
) {
    val estEdition = cycle != null
    var label by remember { mutableStateOf(cycle?.label ?: "") }
    var theme by remember { mutableStateOf(cycle?.theme ?: THEMES_VOLLEY.first().first) }
    var teamId by remember { mutableStateOf(cycle?.teamId ?: selectedTeamId ?: teams.firstOrNull()?.id ?: "") }
    var dateDebut by remember { mutableStateOf(cycle?.dateDebut ?: LocalDate.now()) }
    var dateFin by remember { mutableStateOf(cycle?.dateFin ?: LocalDate.now().plusWeeks(3)) }
    var notes by remember { mutableStateOf(cycle?.notes ?: "") }

    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (estEdition) "Modifier le cycle" else "Nouveau cycle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom du cycle") },
                    placeholder = { Text("Ex : Fondations, Intensif…") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (selectedTeamId == null && teams.size > 1) {
                    Text("Équipe :", style = MaterialTheme.typography.labelSmall)
                    teams.forEach { team ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { teamId = team.id }
                        ) {
                            RadioButton(selected = teamId == team.id, onClick = { teamId = team.id })
                            Text(team.name, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text("Thème :", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                THEMES_VOLLEY.forEach { (key, label2, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (theme == key) color.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { theme = key }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(selected = theme == key, onClick = { theme = key })
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(label2, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateDebut.format(fmt),
                        onValueChange = { runCatching { dateDebut = LocalDate.parse(it, fmt) } },
                        label = { Text("Début") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dateFin.format(fmt),
                        onValueChange = { runCatching { dateFin = LocalDate.parse(it, fmt) } },
                        label = { Text("Fin") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (estEdition && cycle != null) {
                        TextButton(
                            onClick = { onDelete(cycle.id) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Supprimer") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Button(
                        onClick = {
                            if (label.isNotBlank() && teamId.isNotBlank()) {
                                onConfirm(
                                    SeasonCycle(
                                        id = cycle?.id ?: UUID.randomUUID().toString(),
                                        teamId = teamId,
                                        label = label,
                                        theme = theme,
                                        dateDebut = dateDebut,
                                        dateFin = dateFin,
                                        notes = notes
                                    )
                                )
                            }
                        },
                        enabled = label.isNotBlank()
                    ) { Text("Enregistrer") }
                }
            }
        }
    }
}
