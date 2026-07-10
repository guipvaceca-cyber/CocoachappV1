package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.SeasonConfig
import java.time.LocalDate

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig
) {
    val themeStats = remember(seasonConfig.plannedTrainings) {
        val total = seasonConfig.plannedTrainings.count { it.focusArea != null }.coerceAtLeast(1)
        seasonConfig.plannedTrainings
            .filter { it.focusArea != null }
            .groupBy { it.focusArea!! }
            .mapValues { (it.value.size.toFloat() / total * 100).toInt() }
            .toList()
            .sortedByDescending { it.second }
    }

    val alerts = remember(themeStats, seasonConfig.players) {
        val list = mutableListOf<String>()
        // Simple heuristic: if a theme is > 50% and average tech score is < 3
        if (themeStats.any { it.first.contains("Service") && it.second > 50 }) {
            val avgTech = seasonConfig.players.map { it.techScore }.average()
            if (avgTech < 3.5) {
                list.add("Attention : Le Service/Réception occupe ${themeStats.first { it.first.contains("Service") }.second}% de vos séances, mais le niveau technique global reste moyen (%.1f/5). Votre méthode actuelle stagne peut-être.".format(avgTech))
            }
        }
        
        if (seasonConfig.plannedTrainings.size > 20) {
            val attendanceAvg = seasonConfig.players.map { p -> 
                val total = seasonConfig.plannedTrainings.count { it.teamId == p.teamId }
                val attended = seasonConfig.plannedTrainings.count { it.attendance.contains(p.id) }
                if (total > 0) attended.toFloat() / total else 1f
            }.average()
            if (attendanceAvg < 0.6) {
                list.add("Alerte Assiduité : Avec seulement ${(attendanceAvg * 100).toInt()}% de présence moyenne, la progression collective est mathématiquement freinée. Priorité : Engagement du groupe.")
            }
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("CoCoach Insights", style = MaterialTheme.typography.headlineMedium)
            Text("Analyse de performance et pertinence", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- THEME REPARTITION ---
        item {
            Text("RÉPARTITION DES THÈMES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (themeStats.isEmpty()) {
                        Text("Planifiez des séances avec thèmes pour voir l'analyse.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        themeStats.forEach { (theme, percentage) ->
                            ThemeProgressRow(theme, percentage)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- VIGILANCE ALERTS ---
        item {
            Text("POINTS DE VIGILANCE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (alerts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(12.dp))
                        Text("Aucune anomalie détectée. Votre planification semble équilibrée.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                Card(
                    modifier = Modifier.padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(alert, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // --- INSTITUTIONAL LINKS ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("RESSOURCES & ACCOMPAGNEMENT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            
            ActionCard(
                title = "Contacter le CTD",
                subtitle = "Besoin d'un regard extérieur sur vos M13 ?",
                icon = Icons.Default.ContactSupport,
                color = MaterialTheme.colorScheme.primary
            )
            
            ActionCard(
                title = "Le Vestiaire Anonyme",
                subtitle = "Partagez vos doutes avec la communauté.",
                icon = Icons.Default.Forum,
                color = MaterialTheme.colorScheme.secondary
            )
            
            ActionCard(
                title = "Formations CD26-07",
                subtitle = "Calendrier des prochains stages DRE.",
                icon = Icons.Default.School,
                color = Color(0xFF4CAF50)
            )
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ThemeProgressRow(label: String, percentage: Int) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$percentage%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { /* Navigate or Open Link */ }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
