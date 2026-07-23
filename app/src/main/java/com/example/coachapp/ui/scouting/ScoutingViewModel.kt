package com.example.coachapp.ui.scouting

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.coachapp.data.*
import kotlin.math.roundToInt

class ScoutingViewModel : ViewModel() {
    var log = mutableStateListOf<ScoutingLogEntry>()
    var rally = mutableStateOf(1)
    
    // Score local pour l'overlay
    var scoreUs = mutableStateOf(0)
    var scoreThem = mutableStateOf(0)
    var setNumber = mutableStateOf(1)
    var setsHistory = mutableStateListOf<String>()

    fun addEntry(player: Player, action: ScoutingAction, grade: ScoutingGrade, x: Float? = null, y: Float? = null) {
        log.add(0, ScoutingLogEntry(
            playerId = player.id,
            playerNumber = player.number,
            playerName = player.fullName,
            action = action,
            grade = grade,
            rally = rally.value,
            x = x,
            y = y
        ))
        rally.value++
    }

    fun undoLast() {
        if (log.isNotEmpty()) {
            log.removeAt(0)
            if (rally.value > 1) rally.value--
        }
    }

    fun calculateEfficiency(entries: List<ScoutingLogEntry>): Int {
        if (entries.isEmpty()) return 0
        val sumScores = entries.sumOf { it.grade.score }.toDouble()
        val maxPossible = entries.size * 4.0
        return ((sumScores / maxPossible) * 100).roundToInt()
    }

    fun getStats(allPlayers: List<Player>): List<PlayerStats> {
        return allPlayers.map { player ->
            val playerEntries = log.filter { it.playerId == player.id }
            val statsByAction = ScoutingAction.entries.associateWith { action ->
                val actionEntries = playerEntries.filter { it.action == action }
                val distribution = ScoutingGrade.entries.associateWith { grade ->
                    actionEntries.count { it.grade == grade }
                }
                ActionStats(
                    total = actionEntries.size,
                    efficiency = calculateEfficiency(actionEntries),
                    gradeDistribution = distribution
                )
            }
            PlayerStats(
                player = player,
                totalActions = playerEntries.size,
                globalEfficiency = calculateEfficiency(playerEntries),
                statsByAction = statsByAction
            )
        }
    }

    fun resetMatch() {
        log.clear()
        rally.value = 1
        scoreUs.value = 0
        scoreThem.value = 0
        setNumber.value = 1
        setsHistory.clear()
    }
}
