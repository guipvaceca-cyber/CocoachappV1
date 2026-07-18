package com.example.coachapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.R
import com.example.coachapp.data.*
import com.example.coachapp.ui.theme.CoachAppTheme
import java.time.LocalDate
import java.time.LocalTime

@Preview(showBackground = true, backgroundColor = 0xFF001529)
@Composable
fun DashboardCurrentPreview() {
    CoachAppTheme {
        DashboardScreen(
            modifier = Modifier.fillMaxSize(),
            flashResults = mapOf(
                "Pédagogie" to 4.2,
                "Rythme" to 3.8,
                "Climat" to 4.5,
                "Technique" to 3.5
            ),
            globalResults = mapOf(
                "Pédagogie" to 3.5,
                "Rythme" to 3.2,
                "Climat" to 4.0,
                "Technique" to 3.0
            ),
            seasonConfig = getMockSeasonConfig()
        )
    }
}

/**
 * Version "Remaniée" proposée par l'utilisateur (adaptée à Material 3)
 */
@Preview(showBackground = true, backgroundColor = 0xFF001529)
@Composable
fun ModernDashboardPreview() {
    CoachAppTheme {
        ModernHomeScreen()
    }
}

@Composable
fun ModernHomeScreen() {
    val backgroundColor = Color(0xFF0A111F) // Bleu très foncé
    val cardBg = Color(0xFF162133).copy(alpha = 0.8f) // Glassmorphism
    val accentCyan = Color(0xFF00D2FF)
    val accentPink = Color(0xFFFC2E7F)

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = { /* Ton BottomNav ici */ }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo réduit
                Image(
                    painter = painterResource(id = R.drawable.comitda), 
                    contentDescription = "Logo",
                    modifier = Modifier.height(40.dp)
                )
                // Badge Utilisateur
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- CARTE SAISON (VOTRE SAISON) ---
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("VOTRE SAISON", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Consultez vos statistiques de progression et vos ressources.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Voir mes Analyses", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- ÉVÉNEMENTS (CARROUSEL) ---
            Text("ÉVÉNEMENTS CLUB", color = accentCyan, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(3) { // Mocking 3 cards
                    Card(
                        modifier = Modifier.width(280.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Yellow))
                                Spacer(Modifier.width(8.dp))
                                Text("TOURNOI", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Text("Tournoi de Duras", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("📍 Duras", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- SESSION TERRAIN ---
            Text("PRÊT POUR LE TERRAIN", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainingCardUI(title = "Systèmes de jeu", color = accentPink, modifier = Modifier.weight(1f))
                TrainingCardUI(title = "Session Générale", color = Color(0xFF4ADE80), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TrainingCardUI(title: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart))
            // Bouton LANCER moderne
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                    .padding(4.dp)
            )
        }
    }
}

fun getMockSeasonConfig(): SeasonConfig {
    val teamId = "team_m15"
    val today = LocalDate.now()
    
    return SeasonConfig(
        coachProfile = CoachProfile(firstName = "Patrick", lastName = "Robin", clubName = "VALENCE VOLLEY"),
        teams = listOf(
            Team(id = teamId, name = "M15 Masculins", color = Color(0xFF2196F3))
        ),
        plannedTrainings = listOf(
            TrainingSession(
                id = "session_1",
                teamId = teamId,
                date = today,
                startTime = LocalTime.of(18, 30),
                durationMinutes = 90,
                focusArea = "Réception & Relance",
                isValidated = true,
                warmup = "Échauffement russe",
                drills = "Passes à 3",
                smallGroupSituations = "4x4 défense",
                collectiveGame = "6x6 match"
            )
        ),
        clubEvents = listOf(
            ClubEvent(
                id = "event_1",
                clubId = "club_1",
                title = "Tournoi de Noël",
                type = ClubEventType.TOURNOI,
                scope = ClubEventScope.CLUB_ENTIER,
                date = today.plusDays(5),
                startTime = LocalTime.of(9, 0),
                location = "Halle des Sports"
            ),
            ClubEvent(
                id = "event_2",
                clubId = "club_1",
                title = "Réunion Technique",
                type = ClubEventType.RÉUNION,
                scope = ClubEventScope.COACHS_CIBLÉS,
                date = today.plusDays(2),
                startTime = LocalTime.of(20, 0),
                location = "Bureau Club"
            )
        )
    )
}
