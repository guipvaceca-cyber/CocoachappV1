package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StageActivity(
    val time: String,
    val title: String,
    val subtitle: String? = null,
    val color: Color,
    val details: String? = null
)

data class StageDay(
    val name: String,
    val targetCategories: List<String>,
    val activities: List<StageActivity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagePlanningScreen(
    targetCategories: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val allDays = getStagePlanningData()
    
    // Filtering logic: a day is visible if its targetCategories overlap with the coach's targetCategories
    val visibleDays = remember(targetCategories) {
        if (targetCategories.isEmpty()) allDays 
        else allDays.filter { day -> 
            day.targetCategories.any { dayCat -> 
                targetCategories.any { coachCat -> dayCat.startsWith(coachCat.take(3)) } 
            }
        }
    }

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    
    // Reset index if visible days change
    LaunchedEffect(visibleDays) {
        selectedDayIndex = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planning Stage Printemps", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF001529),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF001529)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (visibleDays.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedDayIndex,
                    containerColor = Color(0xFF001529),
                    contentColor = Color(0xFF00B4D8),
                    divider = {}
                ) {
                    visibleDays.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDayIndex == index,
                            onClick = { selectedDayIndex = index },
                            text = { Text(day.name, fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleDays[selectedDayIndex].activities) { activity ->
                        ActivityCard(activity)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun planning disponible pour vos catégories.", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ActivityCard(activity: StageActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = activity.color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, activity.color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                Icon(Icons.Default.AccessTime, null, tint = activity.color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = activity.time,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = activity.color
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (activity.subtitle != null) {
                    Text(
                        text = activity.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                if (activity.details != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = activity.details,
                        style = MaterialTheme.typography.labelSmall,
                        color = activity.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getStagePlanningData(): List<StageDay> {
    val grey = Color.Gray
    val orange = Color(0xFFFF9800)
    val red = Color(0xFFF44336)
    val green = Color(0xFF4CAF50)
    val purple = Color(0xFF9C27B0)
    val skyBlue = Color(0xFF03A9F4)

    return listOf(
        StageDay("Lundi", listOf("M15", "M18"), listOf(
            StageActivity("8h00", "Arrivée des cadres", "Au gymnase", grey),
            StageActivity("8h30", "Accueil des stagiaires", null, orange),
            StageActivity("9h00", "Présentation stage", "Objectifs, contenus", grey),
            StageActivity("10h00", "Entraînement des sélections", "M15 M18", red, "M15F Patk Pas, M18F Aur, Gui P, M15G St, M18G Gui K, Jo"),
            StageActivity("12h00", "Départ gymnase", null, grey),
            StageActivity("12h15", "Repas et installation internat", null, grey),
            StageActivity("14h00", "Entraînement des sélections", "M15 M18", red, "M15F Patk Pas, M18F Aur, Gui P, M15G St, M18G Gui K, Jo"),
            StageActivity("16h00", "Goûter", null, green),
            StageActivity("16h30", "Entraînement des sélections", "M15 M18", red, "M15F Patk Pas, M18F Aur, Gui P, M15G St, M18G Gui K, Jo"),
            StageActivity("18h45", "Départ gymnase", null, grey),
            StageActivity("19h00", "REPAS", null, grey),
            StageActivity("20h00", "Tournoi / Soirée à thème", null, orange, "Patk, Gui P, Aur, Jo"),
            StageActivity("21h30", "Départ gymnase", null, grey),
            StageActivity("22h00", "Douches", null, grey),
            StageActivity("22h30", "Extinction des feux", null, grey)
        )),
        StageDay("Mardi", listOf("M15", "M18"), listOf(
            StageActivity("8h30", "Arrivée cadres / P'tit déj", "Groupes sélection", grey, "Patk, Gui P, Jo, Aur"),
            StageActivity("9h00", "Accueil stagiaires M18 éq 2", "Nettoyage rangement", orange),
            StageActivity("9h30", "Ent M18 éq 2 et perf M15 M18", null, orange, "Gui K, St, Pas, Patk, Mat"),
            StageActivity("10h45", "Entr sel M15 M18 éq 1", null, red, "M15F Patk, M18F éq1 Gui P, M15G St, M18G éq 1 Gui K, Jo"),
            StageActivity("12h00", "Départ gymnase", null, grey),
            StageActivity("12h15", "REPAS Cantine", null, grey, "St, Gui K, Gui P, Patk, Jo"),
            StageActivity("12h45", "Ent M18 éq 2 et perf M15 M18", "Fin ent 14h45", orange, "Pascal, Patk, St, Mat, Aur"),
            StageActivity("14h00", "Temps libre sel M15 M18", null, grey),
            StageActivity("14h45", "Ent sel M15 M18", "Fin temps libre M18 éq 2", red, "M15F Pat, M18F Gui P, M15G St, M18G Gui K, Jo"),
            StageActivity("16h00", "Goûter", null, green),
            StageActivity("17h00", "Ent M18 éq 2 et perf M15 M18", null, orange, "St, Gui P, Patk, Pas, Mat, Aur"),
            StageActivity("18h45", "Départ gymnase", null, grey),
            StageActivity("19h00", "REPAS", null, grey),
            StageActivity("20h00", "Tournoi / Soirée à thème", null, orange, "Patk, Gui P, Aur, Mat"),
            StageActivity("21h30", "Départ gymnase", null, grey),
            StageActivity("22h00", "Douches", null, grey),
            StageActivity("22h30", "Extinction des feux", null, grey)
        )),
        StageDay("Mercredi", listOf("M15", "M18"), listOf(
            StageActivity("8h30", "P'tit déj perf M15 M18 et M18 éq 2", "Nettoyage rangement", grey, "Patk, Gui P, Aur, Mat"),
            StageActivity("10h00", "Entraînement perf M15 M18 et M18 éq 2", null, orange, "Patk, Gui P, Aur, Pas, St, Mat"),
            StageActivity("12h00", "Départ gymnase", null, grey),
            StageActivity("12h15", "REPAS Cantine", null, grey),
            StageActivity("13h30", "Entraînement perf M15 M18 et M18 éq 2", null, orange, "Patk, Gui P, Aur, Pas, St, Mat"),
            StageActivity("15h30", "Goûter", null, green),
            StageActivity("16h00", "Entraînement perf M15 M18 et M18 éq 2", null, orange, "Patk, Gui P, Aur, Pas, St, Mat"),
            StageActivity("16h45", "Rangement, bilan", null, purple),
            StageActivity("17h00", "Départ", null, purple)
        )),
        StageDay("Jeudi", listOf("M11", "M13"), listOf(
            StageActivity("8h30", "Arrivée des cadres", "Au gymnase", grey),
            StageActivity("9h00", "Accueil des stagiaires", null, skyBlue),
            StageActivity("9h30", "Présentation stage", "Objectifs, contenus", grey),
            StageActivity("10h00", "Manip balle, tournoi 2x2 ou 3x3", null, skyBlue, "Gui P"),
            StageActivity("10h30", "Perf M11 M13 / Sel M12 M13", "Entraînement vs Internat", skyBlue, "Benj Math (Perf) / Man, Aur, Sab, Cel, Gui P (Sel)"),
            StageActivity("11h45", "REPAS (pique nique)", "Sel M12 M13", grey, "Man, Aur, Sab, Cel, Gui P"),
            StageActivity("12h00", "REPAS (pique nique)", "Perf M11 M13", grey, "Benj Math"),
            StageActivity("13h00", "Installation internat / Sel M12 M13", "Entraînement", red, "Man, Aur, Sab, Cel, Gui P"),
            StageActivity("14h30", "Perf M11 M13 / Perf M11 M13", "Entraînement vs Temps libre", skyBlue, "Benj Math (Perf) / Man, Aur, Sab, Cel, Gui P (Temps libre)"),
            StageActivity("15h30", "Goûter", "Sel M12 M13", green),
            StageActivity("16h00", "Goûter / Sel M12 M13", "Perf M11 M13 vs Entraînement", red, "Man, Aur, Sab, Cel, Gui P (Sel)"),
            StageActivity("16h30", "Perf M11 M13", "Temps libre", skyBlue, "Benj Math"),
            StageActivity("17h30", "Départ gymnase", "Sel M12 M13", grey),
            StageActivity("18h30", "REPAS Cantine", null, grey),
            StageActivity("19h30", "Tournoi ou ???", null, purple, "Aur, Sab, Cel, Man, Patk, Gui P"),
            StageActivity("21h00", "Départ gymnase", null, grey),
            StageActivity("21h30", "Douches", null, grey)
        )),
        StageDay("Vendredi", listOf("M11", "M13"), listOf(
            StageActivity("8h00", "P'tit déj Sel M12 M13", null, grey),
            StageActivity("8h30", "P'tit déj perf M11 M13", "Nettoyage rangement", grey, "Benj, Gui P"),
            StageActivity("9h00", "Sélections M12 M13", "Entraînement", red, "Aur, Sab, Cel, Patk, Man, St"),
            StageActivity("10h30", "Nettoyage rangement / Perf M11 M13", "Entraînement", skyBlue, "Benj, Gui P"),
            StageActivity("11h30", "REPAS CANTINE", "Sel M12 M13", grey),
            StageActivity("12h00", "Départ gymnase / REPAS CANTINE", "Perf M11 M13", grey),
            StageActivity("12h45", "Temps libre / Selections M12 M13", null, grey, "Aur, Sab, Cel, Patk, Man, St"),
            StageActivity("13h30", "Sélections M12 M13", "Match amical M18F vs M13F et M13G", red, "2 terrains pour match Sel vs M18. Aur, Patk, St. Reste 2 T à partager avec Perf M11 M13. Man, Sab, Cel"),
            StageActivity("14h15", "Perf M11 M13", "Entraînement", skyBlue, "Benj, Gui P. Reste 2 T à partager avec Selec M12 F et M12G"),
            StageActivity("15h30", "Tournoi 3c3 sur 3T", "avec Selec M12 F et M12G", purple),
            StageActivity("16h30", "Goûter", null, green),
            StageActivity("16h45", "Bilan stage démontage sélections", "TI M12 M13", purple),
            StageActivity("17h00", "Départ", null, purple)
        ))
    )
}
