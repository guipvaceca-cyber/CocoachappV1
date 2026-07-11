package com.example.coachapp.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coachapp.data.*
import com.example.coachapp.data.room.AppDatabase
import com.example.coachapp.data.room.Cycle
import com.example.coachapp.ui.screens.SeasonCycle
import com.example.coachapp.data.CalendrierParser
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CoachViewModel(application: Application) : AndroidViewModel(application) {
    private val persistenceManager = PersistenceManager(application)

    // --- ROOM ---
    private val db = AppDatabase.getInstance(application)
    private val cycleRepository = CycleRepository(db.cycleDao())

    // Cycles exposés à l'UI — liste de SeasonCycle (modèle Compose)
    var cycles by mutableStateOf<List<SeasonCycle>>(emptyList())
        private set

    var flashResults by mutableStateOf(persistenceManager.loadResults(AssessmentType.FLASH))
        private set

    var globalResults by mutableStateOf(persistenceManager.loadResults(AssessmentType.GLOBAL))
        private set

    var history by mutableStateOf(persistenceManager.loadHistory())
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    var isAuthLoading by mutableStateOf(false)
        private set

    var selectedResource by mutableStateOf<LaboResource?>(null)
    var coachSpaceTab by mutableIntStateOf(0)
    var laboTab by mutableStateOf(LaboTab.CORPUS)
    var selectedTool by mutableStateOf<String?>(null)
    var selectedSessionForRecap by mutableStateOf<TrainingSession?>(null)
    var selectedSessionIdForBuilder by mutableStateOf<String?>(null)
    var currentAssessmentType by mutableStateOf(AssessmentType.FLASH)

    var adminAlerts by mutableStateOf<List<AdminAlert>>(emptyList())
        private set

    var userRole by mutableStateOf(UserRole.USER)
        private set

    var isCoachCde by mutableStateOf(false)
        private set

    var cdeCategorie by mutableStateOf<String?>(null)
        private set

    var cdeRole by mutableStateOf<String?>(null)
        private set

    // --- COMMUNITY / LOCKER ROOM ---
    var publicPosts by mutableStateOf<List<AnonymousPost>>(emptyList())
        private set

    var isLockerRoomLoading by mutableStateOf(false)
        private set

    var lockerRoomError by mutableStateOf<String?>(null)
        private set

    // ----------------------------------------------------------------
    // CYCLES — lecture / écriture Room
    // ----------------------------------------------------------------

    // Charge les cycles d'une catégorie depuis Room
    // categorieId = index de l'équipe dans la liste (solution temporaire
    // en attendant que Categorie soit fully migrée dans Room)
    fun chargerCycles(teamId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val teamIndex = seasonConfig.teams.indexOfFirst { it.id == teamId }.coerceAtLeast(0)
            val roomCycles = cycleRepository.getCyclesPourCategorie(teamIndex)
            val seasonCycles = roomCycles.map { it.toSeasonCycle() }
            withContext(Dispatchers.Main) {
                cycles = seasonCycles
            }
        }
    }

    fun chargerTousLesCycles() {
        viewModelScope.launch(Dispatchers.IO) {
            val allCycles = seasonConfig.teams.flatMap { team ->
                val teamIndex = seasonConfig.teams.indexOfFirst { it.id == team.id }.coerceAtLeast(0)
                cycleRepository.getCyclesPourCategorie(teamIndex).map { it.toSeasonCycle() }
            }
            withContext(Dispatchers.Main) {
                cycles = allCycles
            }
        }
    }

    fun ajouterCycle(seasonCycle: SeasonCycle) {
        viewModelScope.launch(Dispatchers.IO) {
            val teamIndex = seasonConfig.teams.indexOfFirst { it.id == seasonCycle.teamId }.coerceAtLeast(0)
            val roomCycle = seasonCycle.toRoomCycle(teamIndex)
            cycleRepository.ajouterCycle(roomCycle)
            chargerTousLesCycles()
        }
    }

    fun modifierCycle(seasonCycle: SeasonCycle) {
        viewModelScope.launch(Dispatchers.IO) {
            val teamIndex = seasonConfig.teams.indexOfFirst { it.id == seasonCycle.teamId }.coerceAtLeast(0)
            val roomCycle = seasonCycle.toRoomCycle(teamIndex)
            cycleRepository.modifierCycle(roomCycle)
            chargerTousLesCycles()
        }
    }

    fun supprimerCycle(cycleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val teamIndex = 0 // On cherche par id
            val roomCycles = cycleRepository.getCyclesPourCategorie(teamIndex)
            // Cherche dans tous les cycles
            val allRoomCycles = seasonConfig.teams.flatMapIndexed { index, _ ->
                cycleRepository.getCyclesPourCategorie(index)
            }
            val toDelete = allRoomCycles.firstOrNull { it.id.toString() == cycleId }
            toDelete?.let { cycleRepository.supprimerCycle(it) }
            chargerTousLesCycles()
        }
    }

    // ----------------------------------------------------------------
    // Conversions SeasonCycle ↔ Room Cycle
    // ----------------------------------------------------------------

    private fun Cycle.toSeasonCycle(): SeasonCycle {
        val team = seasonConfig.teams.getOrNull(categorieId)
        return SeasonCycle(
            id = this.id.toString(),
            teamId = team?.id ?: "",
            label = this.label ?: "",
            theme = this.theme ?: "fondamentaux",
            dateDebut = LocalDate.parse(this.dateDebut),
            dateFin = LocalDate.parse(this.dateFin),
            notes = this.notes ?: ""
        )
    }

    private fun SeasonCycle.toRoomCycle(categorieId: Int): Cycle {
        val cycle = Cycle()
        cycle.categorieId = categorieId
        cycle.label = this.label
        cycle.theme = this.theme
        cycle.dateDebut = this.dateDebut.toString()
        cycle.dateFin = this.dateFin.toString()
        cycle.notes = this.notes
        // Si l'id est un UUID, on le passe en notes pour traçabilité
        // Room auto-génère l'int id
        return cycle
    }

    // ----------------------------------------------------------------
    // Reste du ViewModel — inchangé
    // ----------------------------------------------------------------

    fun fetchLockerRoomPosts() {
        viewModelScope.launch {
            isLockerRoomLoading = true
            lockerRoomError = null
            try {
                val response = SupabaseManager.db.from("locker_room_posts")
                    .select()
                val posts = response.decodeList<AnonymousPost>()
                publicPosts = mockPosts + posts.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                lockerRoomError = "Erreur de chargement : ${e.localizedMessage}"
                publicPosts = mockPosts
            } finally {
                isLockerRoomLoading = false
            }
        }
    }

    fun fetchAdminAlerts() {
        viewModelScope.launch {
            try {
                val alerts = SupabaseManager.db.from("admin_alerts")
                    .select()
                    .decodeList<AdminAlert>()
                adminAlerts = alerts.sortedByDescending { it.createdAt }
            } catch (e: Exception) {}
        }
    }

    fun fetchUserRole() {
        val user = SupabaseManager.auth.currentUserOrNull()
        if (user == null) {
            Log.d("DEBUG_ROLE", "fetchUserRole: No current user found")
            return
        }
        viewModelScope.launch {
            try {
                val response = SupabaseManager.db.from("profiles")
                    .select { filter { eq("id", user.id) } }
                val body = response.data
                Log.d("DEBUG_ROLE", "profiles response body = $body")
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val profileList = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)
                Log.d("DEBUG_ROLE", "profileList size = ${profileList.size}")
                if (profileList.isNotEmpty()) {
                    val roleStr = profileList[0]["role"]?.toString()?.replace("\"", "") ?: "user"
                    userRole = when(roleStr.lowercase()) {
                        "admin" -> UserRole.ADMIN
                        "megadmin" -> UserRole.MEGADMIN
                        else -> UserRole.USER
                    }

                    // On récupère les privilèges CDE
                    val rawIsCoachCde = profileList[0]["is_coach_cde"]?.toString()
                    isCoachCde = rawIsCoachCde == "true"
                    cdeCategorie = profileList[0]["cde_categorie"]?.toString()?.replace("\"", "")
                    
                    val rawCdeRole = profileList[0]["cde_role"]?.toString()
                    Log.d("DEBUG_CDE", "cde_role raw = $rawCdeRole")
                    cdeRole = rawCdeRole?.replace("\"", "")
                    
                    Log.d("DEBUG_CDE", "cdeRole final = $cdeRole")
                    Log.d("DEBUG_CDE", "isCoachCde = $isCoachCde")
                    Log.d("DEBUG_CDE", "cdeCategorie = $cdeCategorie")
                }
                if (userRole == UserRole.MEGADMIN) fetchAdminAlerts()
            } catch (e: Exception) {
                Log.e("DEBUG_ROLE", "Error fetching role", e)
            }
        }
    }

    fun postToLockerRoom(title: String, content: String, category: PostCategory, alias: String? = null, isOfficial: Boolean = false) {
        viewModelScope.launch {
            isLockerRoomLoading = true
            lockerRoomError = null
            try {
                val requestBody = kotlinx.serialization.json.buildJsonObject {
                    put("title", kotlinx.serialization.json.JsonPrimitive(title))
                    put("content", kotlinx.serialization.json.JsonPrimitive(content))
                    put("category", kotlinx.serialization.json.JsonPrimitive(category.name))
                    put("persona", kotlinx.serialization.json.JsonPrimitive(alias ?: "Coach Anonyme"))
                    put("club_initial", kotlinx.serialization.json.JsonPrimitive(seasonConfig.coachProfile.clubName.take(4).uppercase()))
                    put("is_official", kotlinx.serialization.json.JsonPrimitive(isOfficial))
                    put("author_id", kotlinx.serialization.json.JsonPrimitive(SupabaseManager.auth.currentUserOrNull()?.id ?: ""))
                }
                SupabaseManager.functions.invoke(
                    function = "anonymize-locker-posts",
                    body = requestBody
                )
                fetchLockerRoomPosts()
            } catch (e: Exception) {
                Log.e("POST_ERROR", "Erreur lors de l'envoi", e)
                lockerRoomError = "Erreur d'envoi : ${e.localizedMessage}"
            } finally {
                isLockerRoomLoading = false
            }
        }
    }

    init {
        SupabaseManager.auth.sessionStatus.onEach { status ->
            isLoggedIn = status is io.github.jan.supabase.gotrue.SessionStatus.Authenticated
            if (isLoggedIn) {
                fetchLockerRoomPosts()
                fetchUserRole()
                chargerTousLesCycles()
            }
        }.launchIn(viewModelScope)
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isAuthLoading = true
            authError = null
            try {
                SupabaseManager.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                fetchUserRole()
            } catch (e: Exception) {
                authError = "Erreur de connexion : ${e.localizedMessage}"
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            isAuthLoading = true
            authError = null
            try {
                SupabaseManager.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                authError = "Compte créé ! Vérifiez vos emails pour confirmer."
            } catch (e: Exception) {
                authError = "Erreur d'inscription : ${e.localizedMessage}"
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { SupabaseManager.auth.signOut() } catch (e: Exception) {}
            persistenceManager.clearAllData()
            isLoggedIn = false
            userRole = UserRole.USER
            isCoachCde = false
            cdeCategorie = null
            cdeRole = null
            seasonConfig = SeasonConfig()
            flashResults = null
            globalResults = null
            history = emptyList()
            cycles = emptyList()
        }
    }

    fun updateResults(type: AssessmentType, newResults: Map<String, Double>, coachNote: String? = null) {
        if (type == AssessmentType.FLASH) flashResults = newResults else globalResults = newResults
        persistenceManager.saveResults(newResults, coachNote)
        history = persistenceManager.loadHistory()
    }

    var seasonConfig by mutableStateOf(persistenceManager.loadSeasonConfig())
        private set

    fun updateSeasonConfig(config: SeasonConfig) {
        seasonConfig = config
        persistenceManager.saveSeasonConfig(config)
    }

    fun completeOnboarding(config: SeasonConfig) {
        seasonConfig = config.copy(isOnboardingCompleted = true)
        persistenceManager.saveSeasonConfig(seasonConfig)
        chargerCalendrierPrevisionnel()
    }

    fun chargerCalendrierPrevisionnel() {
        viewModelScope.launch(Dispatchers.IO) {
            val nouveauxEvenements = mutableListOf<CompetitionEvent>()
            seasonConfig.teams.forEach { team ->
                val categorie = CalendrierParser.normaliserCategorie(team.name)
                val events = CalendrierParser.chargerEvenements(
                    context = getApplication(),
                    categories = listOf(categorie),
                    teamId = team.id
                )
                nouveauxEvenements.addAll(events)
            }
            withContext(Dispatchers.Main) {
                // On ne duplique pas les événements déjà présents
                val existants = seasonConfig.competitions.map { it.date.toString() + it.teamId }.toSet()
                val aAjouter = nouveauxEvenements.filter {
                    (it.date.toString() + it.teamId) !in existants
                }
                if (aAjouter.isNotEmpty()) {
                    updateSeasonConfig(
                        seasonConfig.copy(
                            competitions = seasonConfig.competitions + aAjouter
                        )
                    )
                }
            }
        }
    }

    fun updatePlayer(player: Player) {
        val updatedPlayers = if (seasonConfig.players.any { it.id == player.id }) {
            seasonConfig.players.map { if (it.id == player.id) player else it }
        } else {
            seasonConfig.players + player
        }
        updateSeasonConfig(seasonConfig.copy(players = updatedPlayers))
    }

    fun deletePlayer(playerId: String) {
        updateSeasonConfig(seasonConfig.copy(players = seasonConfig.players.filter { it.id != playerId }))
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                SupabaseManager.db.from("locker_room_posts")
                    .delete { filter { eq("id", postId) } }
                fetchLockerRoomPosts()
            } catch (e: Exception) {
                lockerRoomError = "Erreur de suppression : ${e.localizedMessage}"
            }
        }
    }

    fun addPlayerAssessment(playerId: String, assessment: PlayerAssessment) {
        val updatedPlayers = seasonConfig.players.map { player ->
            if (player.id == playerId) {
                player.copy(
                    assessmentHistory = player.assessmentHistory + assessment,
                    techScore = assessment.techScore,
                    tactScore = assessment.tactScore,
                    physicalScore = assessment.physicalScore
                )
            } else player
        }
        updateSeasonConfig(seasonConfig.copy(players = updatedPlayers))
    }

    fun useHelp() {
        val now = System.currentTimeMillis()
        updateSeasonConfig(seasonConfig.copy(helpUsages = seasonConfig.helpUsages + now))
    }

    fun getHelpUsageCountThisMonth(): Int {
        val now = LocalDate.now()
        return seasonConfig.helpUsages.count {
            val date = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), java.time.ZoneId.systemDefault()).toLocalDate()
            date.month == now.month && date.year == now.year
        }
    }

    fun isDiagnosticAvailable(): Boolean {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val currentTime = now.toLocalTime()
        seasonConfig.plannedTrainings.find { it.date == today }?.let { session ->
            val endTime = session.startTime.plusMinutes(session.durationMinutes.toLong())
            if (currentTime.isAfter(endTime) && currentTime.isBefore(endTime.plusHours(4))) {
                return true
            }
        }
        return false
    }

    fun isMonthlyReviewDue(): Boolean = LocalDate.now().dayOfMonth == 1

    fun clearSelectedResource() { selectedResource = null }
    fun clearSelectedTool() { selectedTool = null }
    fun clearAuthError() { authError = null }
}