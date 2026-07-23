package com.example.coachapp.data

import androidx.compose.ui.graphics.Color
import com.example.coachapp.ui.theme.*

enum class ScoutingGrade(val symbol: String, val label: String, val color: Color, val score: Int) {
    ERROR("/", "Faute", GradeError, 0),
    BAD("−", "Mauvais", GradeBad, 1),
    OK("!", "Moyen", GradeOK, 2),
    GOOD("+", "Bien", GradeGood, 3),
    PERFECT("#", "Parfait", GradePerfect, 4)
}

enum class ScoutingAction(val label: String, val short: String, val color: Color) {
    SERVICE("Service", "SRV", ActionService),
    RECEPTION("Réception", "RCP", ActionReception),
    ATTAQUE("Attaque", "ATT", ActionAttaque),
    BLOC("Bloc", "BLC", ActionBloc),
    DEFENSE("Défense", "DEF", ActionDefense)
}

data class ScoutingLogEntry(
    val id: Long = System.currentTimeMillis(),
    val playerId: String,
    val playerNumber: Int,
    val playerName: String,
    val action: ScoutingAction,
    val grade: ScoutingGrade,
    val rally: Int,
    val x: Float? = null,
    val y: Float? = null
)

data class PlayerStats(
    val player: Player,
    val totalActions: Int,
    val globalEfficiency: Int,
    val statsByAction: Map<ScoutingAction, ActionStats>
)

data class ActionStats(
    val total: Int,
    val efficiency: Int,
    val gradeDistribution: Map<ScoutingGrade, Int>
)
