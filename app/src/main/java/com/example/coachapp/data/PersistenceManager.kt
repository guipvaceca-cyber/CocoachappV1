package com.example.coachapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AssessmentRecord(
    val date: Long,
    val scores: Map<String, Double>,
    val coachNote: String? = null
)

/**
 * NEW SEGMENTED PERSISTENCE MANAGER
 * Splitting data into distinct services for modularity and network readiness.
 */
class PersistenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("coach_app_prefs_v2", Context.MODE_PRIVATE)

    // --- SERVICE 1 : PROFILE & IDENTITY ---
    fun saveProfile(profile: CoachProfile) {
        val obj = JSONObject()
        obj.put("first", profile.firstName)
        obj.put("last", profile.lastName)
        obj.put("nickname", profile.nickname)
        obj.put("club", profile.clubName)
        obj.put("level", profile.formationLevel)
        obj.put("goalPersonal", profile.goalPersonal)
        obj.put("goalCollective", profile.goalCollective)
        obj.put("goal3Years", profile.goal3Years)
        obj.put("persona", profile.coachPersona)
        prefs.edit().putString("p_identity", obj.toString()).apply()
    }

    fun loadProfile(): CoachProfile {
        val str = prefs.getString("p_identity", null) ?: return CoachProfile()
        val obj = JSONObject(str)
        return CoachProfile(
            firstName = obj.optString("first"),
            lastName = obj.optString("last"),
            nickname = obj.optString("nickname"),
            clubName = obj.optString("club"),
            formationLevel = obj.optString("level"),
            goalPersonal = obj.optString("goalPersonal"),
            goalCollective = obj.optString("goalCollective"),
            goal3Years = obj.optString("goal3Years"),
            coachPersona = obj.optString("persona")
        )
    }

    // --- SERVICE 2 : COLLECTIFS (Teams & Players) ---
    fun saveTeams(teams: List<Team>) {
        val arr = JSONArray()
        teams.forEach { team ->
            val tObj = JSONObject()
            tObj.put("id", team.id)
            tObj.put("name", team.name)
            tObj.put("color", team.color.toArgb())
            tObj.put("projectType", team.projectType)
            tObj.put("objective", team.objective)
            tObj.put("format", team.format.name)
            arr.put(tObj)
        }
        prefs.edit().putString("c_teams", arr.toString()).apply()
    }

    fun loadTeams(): List<Team> {
        val str = prefs.getString("c_teams", "[]") ?: "[]"
        val list = mutableListOf<Team>()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Team(
                id = obj.getString("id"),
                name = obj.getString("name"),
                color = Color(obj.getInt("color")),
                projectType = obj.optString("projectType", "Développement"),
                objective = obj.optString("objective"),
                format = TeamFormat.valueOf(obj.optString("format", "SIX_SIX"))
            ))
        }
        return list
    }

    fun savePlayers(players: List<Player>) {
        val arr = JSONArray()
        players.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("teamId", p.teamId)
            pObj.put("first", p.firstName)
            pObj.put("last", p.lastName)
            pObj.put("pos", p.position)
            pObj.put("num", p.number)
            pObj.put("license", p.licenseNumber)
            pObj.put("birth", p.birthYear)
            pObj.put("practice", p.yearsOfPractice)
            pObj.put("cat", p.category)
            pObj.put("catYear", p.categoryYear)
            pObj.put("notes", p.notes)
            pObj.put("tech", p.techScore)
            pObj.put("tact", p.tactScore)
            pObj.put("phys", p.physicalScore)
            arr.put(pObj)
        }
        prefs.edit().putString("c_players", arr.toString()).apply()
    }

    fun loadPlayers(): List<Player> {
        val str = prefs.getString("c_players", "[]") ?: "[]"
        val list = mutableListOf<Player>()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val pObj = arr.getJSONObject(i)
            list.add(Player(
                id = pObj.getString("id"),
                teamId = pObj.getString("teamId"),
                firstName = pObj.getString("first"),
                lastName = pObj.getString("last"),
                position = pObj.getString("pos"),
                number = pObj.getInt("num"),
                licenseNumber = pObj.optString("license"),
                birthYear = pObj.optInt("birth"),
                yearsOfPractice = pObj.optInt("practice"),
                category = pObj.optString("cat"),
                categoryYear = pObj.optInt("catYear", 1),
                notes = pObj.optString("notes"),
                techScore = pObj.optInt("tech"),
                tactScore = pObj.optInt("tact"),
                physicalScore = pObj.optInt("phys")
            ))
        }
        return list
    }

    fun saveSchedules(schedules: List<TrainingSchedule>) {
        val arr = JSONArray()
        schedules.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id ?: "")
            obj.put("teamId", s.teamId)
            obj.put("clubId", s.clubId ?: "")
            obj.put("day", s.dayOfWeek.name)
            obj.put("time", s.startTime.toString())
            obj.put("duration", s.durationMinutes)
            obj.put("terrain", s.terrain ?: "Terrain 1")
            arr.put(obj)
        }
        prefs.edit().putString("s_schedules", arr.toString()).apply()
    }

    fun loadSchedules(): List<TrainingSchedule> {
        val str = prefs.getString("s_schedules", "[]") ?: "[]"
        val list = mutableListOf<TrainingSchedule>()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(TrainingSchedule(
                id = obj.optString("id").takeIf { it.isNotEmpty() },
                teamId = obj.getString("teamId"),
                clubId = obj.optString("clubId").takeIf { it.isNotEmpty() },
                dayOfWeek = java.time.DayOfWeek.valueOf(obj.getString("day")),
                startTime = LocalTime.parse(obj.getString("time")),
                durationMinutes = obj.getInt("duration"),
                terrain = obj.optString("terrain", "Terrain 1")
            ))
        }
        return list
    }

    fun saveCompetitions(comps: List<CompetitionEvent>) {
        val arr = JSONArray()
        comps.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("teamId", c.teamId)
            obj.put("clubId", c.clubId ?: "")
            obj.put("date", c.date.toString())
            obj.put("time", c.startTime.toString())
            obj.put("type", c.type.name)
            obj.put("opponent", c.opponent)
            obj.put("location", c.location)
            
            val attObj = JSONObject()
            c.attendance.forEach { (k, v) -> attObj.put(k, v) }
            obj.put("attendance", attObj)
            
            obj.put("saison", c.saison)
            arr.put(obj)
        }
        prefs.edit().putString("s_competitions", arr.toString()).apply()
    }

    fun loadCompetitions(): List<CompetitionEvent> {
        val str = prefs.getString("s_competitions", "[]") ?: "[]"
        val list = mutableListOf<CompetitionEvent>()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val att = mutableMapOf<String, String>()
            val attJson = obj.optJSONObject("attendance")
            attJson?.keys()?.forEach { key -> att[key] = attJson.getString(key) }

            list.add(CompetitionEvent(
                id = obj.getString("id"),
                teamId = obj.getString("teamId"),
                clubId = obj.optString("clubId").takeIf { it.isNotEmpty() },
                date = LocalDate.parse(obj.getString("date")),
                startTime = LocalTime.parse(obj.getString("time")),
                type = CompetitionType.valueOf(obj.getString("type")),
                opponent = obj.getString("opponent"),
                location = obj.getString("location"),
                attendance = att,
                saison = obj.optString("saison", "2026-2027")
            ))
        }
        return list
    }

    // --- SERVICE 3 : SAISON (Planning & Sessions) ---
    fun saveSessions(sessions: List<TrainingSession>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("teamId", s.teamId)
            sObj.put("date", s.date.toString())
            sObj.put("time", s.startTime.toString())
            sObj.put("duration", s.durationMinutes)
            sObj.put("focus", s.focusArea ?: "")
            
            val attObj = JSONObject()
            s.attendance.forEach { (k, v) -> attObj.put(k, v) }
            sObj.put("attendance", attObj)

            sObj.put("assessmentId", s.assessmentId ?: "")
            sObj.put("warmup", s.warmup)
            sObj.put("warmupDur", s.warmupDuration)
            sObj.put("drills", s.drills)
            sObj.put("drillsDur", s.drillsDuration)
            sObj.put("smallGroup", s.smallGroupSituations)
            sObj.put("smallGroupDur", s.smallGroupDuration)
            sObj.put("collective", s.collectiveGame)
            sObj.put("collectiveDur", s.collectiveDuration)
            sObj.put("isValidated", s.isValidated)
            sObj.put("liveFeedback", s.liveFeedback)
            sObj.put("coachNotes", s.coachNotes ?: "")
            sObj.put("futureNote", s.noteForFutureMe)
            arr.put(sObj)
        }
        prefs.edit().putString("s_sessions", arr.toString()).apply()
    }

    fun loadSessions(): List<TrainingSession> {
        val str = prefs.getString("s_sessions", "[]") ?: "[]"
        val list = mutableListOf<TrainingSession>()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val sObj = arr.getJSONObject(i)
            
            val att = mutableMapOf<String, String>()
            val attJson = sObj.opt("attendance")
            if (attJson is JSONObject) {
                attJson.keys().forEach { key ->
                    att[key] = attJson.getString(key)
                }
            } else if (attJson is JSONArray) {
                for (j in 0 until attJson.length()) {
                    att[attJson.getString(j)] = "present"
                }
            }
            
            list.add(TrainingSession(
                id = sObj.getString("id"),
                teamId = sObj.getString("teamId"),
                date = LocalDate.parse(sObj.getString("date")),
                startTime = LocalTime.parse(sObj.getString("time")),
                durationMinutes = sObj.getInt("duration"),
                focusArea = sObj.optString("focus").takeIf { it.isNotEmpty() },
                attendance = att,
                assessmentId = sObj.optString("assessmentId").takeIf { it.isNotEmpty() },
                warmup = sObj.optString("warmup"),
                warmupDuration = sObj.optInt("warmupDur", 15),
                drills = sObj.optString("drills"),
                drillsDuration = sObj.optInt("drillsDur", 20),
                smallGroupSituations = sObj.optString("smallGroup"),
                smallGroupDuration = sObj.optInt("smallGroupDur", 20),
                collectiveGame = sObj.optString("collective"),
                collectiveDuration = sObj.optInt("collectiveDur", 25),
                isValidated = sObj.optBoolean("isValidated", false),
                liveFeedback = sObj.optString("liveFeedback", ""),
                coachNotes = sObj.optString("coachNotes").takeIf { it.isNotEmpty() },
                noteForFutureMe = sObj.optString("futureNote", "")
            ))
        }
        return list
    }

    // --- SERVICE 4 : HISTORIQUE & DIAGNOSTICS ---
    fun saveResults(results: Map<String, Double>, coachNote: String? = null) {
        val history = loadHistory().toMutableList()
        history.add(AssessmentRecord(System.currentTimeMillis(), results, coachNote))
        val arr = JSONArray()
        history.forEach { record ->
            val rObj = JSONObject()
            rObj.put("date", record.date)
            record.coachNote?.let { rObj.put("note", it) }
            val sObj = JSONObject()
            record.scores.forEach { (k, v) -> sObj.put(k, v) }
            rObj.put("scores", sObj)
            arr.put(rObj)
        }
        prefs.edit().putString("h_diagnostics", arr.toString()).apply()
    }

    fun loadHistory(): List<AssessmentRecord> {
        val str = prefs.getString("h_diagnostics", null) ?: return emptyList()
        val list = mutableListOf<AssessmentRecord>()
        try {
            val arr = JSONArray(str)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val scoresObj = obj.getJSONObject("scores")
                val scores = mutableMapOf<String, Double>()
                val keys = scoresObj.keys()
                while (keys.hasNext()) { val k = keys.next(); scores[k] = scoresObj.getDouble(k) }
                list.add(AssessmentRecord(obj.getLong("date"), scores, obj.optString("note", "")))
            }
        } catch (e: Exception) {}
        return list
    }

    fun loadResults(type: AssessmentType): Map<String, Double>? {
        val history = loadHistory()
        val data = if (type == AssessmentType.FLASH) flashDiagnosticData else globalDiagnosticData
        val validIds = data.flatMap { d -> d.questions.map { it.id } }.toSet()
        
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
            isOnboardingCompleted = prefs.getBoolean("sys_onboarding", false)
        )
    }

    // --- TACTICAL BOARDS ---
    fun saveTacticalBoard(name: String, lines: List<BoardLine>, elements: List<BoardElement>) {
        val editor = prefs.edit()
        val boardsStr = prefs.getString("t_boards", "[]") ?: "[]"
        val boardsArray = JSONArray(boardsStr)
        
        val boardObj = JSONObject()
        boardObj.put("name", name)
        boardObj.put("date", System.currentTimeMillis())
        
        val linesArray = JSONArray()
        lines.forEach { line ->
            val lObj = JSONObject()
            lObj.put("color", line.color.toArgb())
            val ptsArray = JSONArray()
            line.points.forEach { pt ->
                val pObj = JSONObject()
                pObj.put("x", pt.x); pObj.put("y", pt.y)
                ptsArray.put(pObj)
            }
            lObj.put("points", ptsArray)
            linesArray.put(lObj)
        }
        boardObj.put("lines", linesArray)
        
        val elemsArray = JSONArray()
        elements.forEach { el ->
            val eObj = JSONObject()
            eObj.put("type", el.type.name)
            eObj.put("x", el.position.x); eObj.put("y", el.position.y)
            eObj.put("label", el.label)
            eObj.put("rot", el.rotation)
            elemsArray.put(eObj)
        }
        boardObj.put("elements", elemsArray)
        
        boardsArray.put(boardObj)
        editor.putString("t_boards", boardsArray.toString())
        editor.apply()
    }

    fun loadTacticalBoards(): List<SavedBoard> {
        val str = prefs.getString("t_boards", "[]") ?: "[]"
        val list = mutableListOf<SavedBoard>()
        try {
            val arr = JSONArray(str)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lines = mutableListOf<BoardLine>()
                val lArr = obj.getJSONArray("lines")
                for (j in 0 until lArr.length()) {
                    val lObj = lArr.getJSONObject(j)
                    val pts = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    val pArr = lObj.getJSONArray("points")
                    for (k in 0 until pArr.length()) {
                        val pObj = pArr.getJSONObject(k)
                        pts.add(androidx.compose.ui.geometry.Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                    }
                    lines.add(BoardLine(pts, Color(lObj.getInt("color"))))
                }
                
                val elements = mutableListOf<BoardElement>()
                val eArr = obj.getJSONArray("elements")
                for (j in 0 until eArr.length()) {
                    val eObj = eArr.getJSONObject(j)
                    elements.add(BoardElement(UUID.randomUUID().toString(), ElementType.valueOf(eObj.getString("type")), androidx.compose.ui.geometry.Offset(eObj.getDouble("x").toFloat(), eObj.getDouble("y").toFloat()), eObj.optString("label", ""), eObj.optDouble("rot", 0.0).toFloat()))
                }
                list.add(SavedBoard(obj.getString("name"), obj.getLong("date"), lines, elements))
            }
        } catch (e: Exception) {}
        return list
    }
    fun saveCredentials(user: String, pass: String) = prefs.edit().putString("auth_user", user).putString("auth_pass", pass).apply()
    fun getCredentials(): Pair<String?, String?> = prefs.getString("auth_user", null) to prefs.getString("auth_pass", null)

    // --- COULEURS ÉQUIPES (Préférences Coach) ---
    fun saveTeamColors(colors: Map<String, Int>) {
        val obj = JSONObject()
        colors.forEach { (id, color) -> obj.put(id, color) }
        prefs.edit().putString("ui_team_colors", obj.toString()).apply()
    }

    fun loadTeamColors(): Map<String, Int> {
        val str = prefs.getString("ui_team_colors", "{}") ?: "{}"
        val map = mutableMapOf<String, Int>()
        val obj = JSONObject(str)
        obj.keys().forEach { id -> map[id] = obj.getInt(id) }
        return map
    }

    fun clearAllData() = prefs.edit().clear().apply()
}
