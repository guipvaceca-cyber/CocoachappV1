package com.example.coachapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerialName
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Serializable
data class AssessmentRecord(
    val date: Long,
    val scores: Map<String, Double>,
    @SerialName("note") val coachNote: String? = null
)

/**
 * NEW SEGMENTED PERSISTENCE MANAGER
 * Splitting data into distinct services for modularity and network readiness.
 */
class PersistenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("coach_app_prefs_v2", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    // --- SERVICE 1 : PROFILE & IDENTITY ---
    fun saveProfile(profile: CoachProfile) {
        val str = json.encodeToString(profile)
        prefs.edit().putString("p_identity", str).apply()
    }

    fun loadProfile(): CoachProfile {
        val str = prefs.getString("p_identity", null) ?: return CoachProfile()
        return try {
            json.decodeFromString<CoachProfile>(str)
        } catch (e: Exception) {
            CoachProfile()
        }
    }

    // --- SERVICE 2 : COLLECTIFS (Teams & Players) ---
    fun saveTeams(teams: List<Team>) {
        val str = json.encodeToString(teams)
        prefs.edit().putString("c_teams", str).apply()
    }

    fun loadTeams(): List<Team> {
        val str = prefs.getString("c_teams", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<Team>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePlayers(players: List<Player>) {
        val str = json.encodeToString(players)
        prefs.edit().putString("c_players", str).apply()
    }

    fun loadPlayers(): List<Player> {
        val str = prefs.getString("c_players", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<Player>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSchedules(schedules: List<TrainingSchedule>) {
        val str = json.encodeToString(schedules)
        prefs.edit().putString("s_schedules", str).apply()
    }

    fun loadSchedules(): List<TrainingSchedule> {
        val str = prefs.getString("s_schedules", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<TrainingSchedule>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCompetitions(comps: List<CompetitionEvent>) {
        val str = json.encodeToString(comps)
        prefs.edit().putString("s_competitions", str).apply()
    }

    fun loadCompetitions(): List<CompetitionEvent> {
        val str = prefs.getString("s_competitions", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<CompetitionEvent>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveClubEvents(events: List<ClubEvent>) {
        val str = json.encodeToString(events)
        prefs.edit().putString("s_club_events", str).apply()
    }

    fun loadClubEvents(): List<ClubEvent> {
        val str = prefs.getString("s_club_events", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<ClubEvent>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- SERVICE 3 : SAISON (Planning & Sessions) ---
    fun saveSessions(sessions: List<TrainingSession>) {
        val str = json.encodeToString(sessions)
        prefs.edit().putString("s_sessions", str).apply()
    }

    fun loadSessions(): List<TrainingSession> {
        val str = prefs.getString("s_sessions", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<TrainingSession>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- SERVICE 4 : HISTORIQUE & DIAGNOSTICS ---
    fun saveResults(results: Map<String, Double>, coachNote: String? = null) {
        val history = loadHistory().toMutableList()
        history.add(AssessmentRecord(System.currentTimeMillis(), results, coachNote))
        val str = json.encodeToString(history)
        prefs.edit().putString("h_diagnostics", str).apply()
    }

    fun loadHistory(): List<AssessmentRecord> {
        val str = prefs.getString("h_diagnostics", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<AssessmentRecord>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadResults(type: AssessmentType): Map<String, Double>? {
        val history = loadHistory()
        val data = if (type == AssessmentType.FLASH) flashDiagnosticData else globalDiagnosticData
        // Important: Results are stored by DOMAIN ID, not question ID.
        val validIds = data.map { it.id }.toSet()
        
        return history.lastOrNull { record: AssessmentRecord ->
            record.scores.keys.any { id: String -> id in validIds }
        }?.scores
    }

    // --- COMPATIBILITY WRAPPER ---
    fun saveSeasonConfig(config: SeasonConfig) {
        saveProfile(config.coachProfile)
        saveTeams(config.teams)
        savePlayers(config.players)
        saveSessions(config.plannedTrainings)
        saveSchedules(config.trainingSchedules)
        saveCompetitions(config.competitions)
        saveClubEvents(config.clubEvents)
        prefs.edit().putBoolean("sys_onboarding", config.isOnboardingCompleted).apply()
    }

    fun loadSeasonConfig(): SeasonConfig {
        return SeasonConfig(
            coachProfile = loadProfile(),
            teams = loadTeams(),
            players = loadPlayers(),
            trainingSchedules = loadSchedules(),
            plannedTrainings = loadSessions(),
            competitions = loadCompetitions(),
            clubEvents = loadClubEvents(),
            isOnboardingCompleted = prefs.getBoolean("sys_onboarding", false)
        )
    }

    // --- TACTICAL BOARDS ---
    fun saveTacticalBoard(name: String, lines: List<BoardLine>, elements: List<BoardElement>) {
        val boardsStr = prefs.getString("t_boards", "[]") ?: "[]"
        val boardsList = try {
            json.decodeFromString<MutableList<SavedBoard>>(boardsStr)
        } catch (e: Exception) {
            mutableListOf()
        }
        
        boardsList.add(SavedBoard(name, System.currentTimeMillis(), lines, elements))
        prefs.edit().putString("t_boards", json.encodeToString(boardsList)).apply()
    }

    fun loadTacticalBoards(): List<SavedBoard> {
        val str = prefs.getString("t_boards", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<SavedBoard>>(str)
        } catch (e: Exception) {
            emptyList()
        }
    }
    fun saveCredentials(user: String, pass: String) = prefs.edit().putString("auth_user", user).putString("auth_pass", pass).apply()
    fun getCredentials(): Pair<String?, String?> = prefs.getString("auth_user", null) to prefs.getString("auth_pass", null)

    // --- COULEURS ÉQUIPES (Préférences Coach) ---
    fun saveTeamColors(colors: Map<String, Int>) {
        prefs.edit().putString("ui_team_colors", json.encodeToString(colors)).apply()
    }

    fun loadTeamColors(): Map<String, Int> {
        val str = prefs.getString("ui_team_colors", "{}") ?: "{}"
        return try {
            json.decodeFromString<Map<String, Int>>(str)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun clearAllData() = prefs.edit().clear().apply()
}
