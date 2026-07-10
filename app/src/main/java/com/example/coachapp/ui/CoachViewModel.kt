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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CoachViewModel(application: Application) : AndroidViewModel(application) {
    private val persistenceManager = PersistenceManager(application)

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

    // --- COMMUNITY / LOCKER ROOM ---
    var publicPosts by mutableStateOf<List<AnonymousPost>>(emptyList())
        private set

    var isLockerRoomLoading by mutableStateOf(false)
        private set

    var lockerRoomError by mutableStateOf<String?>(null)
        private set

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
        Log.d("DEBUG_ROLE", "fetchUserRole: Starting for user ${user.id}")
        
        viewModelScope.launch {
            try {
                val response = SupabaseManager.db.from("profiles")
                    .select {
                        filter { eq("id", user.id) }
                    }
                
                val body = response.data
                Log.d("DEBUG_ROLE", "Response body = $body")

                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val profileList = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)
                
                if (profileList.isNotEmpty()) {
                    val roleStr = profileList[0]["role"]?.toString()?.replace("\"", "") ?: "user"
                    Log.d("DEBUG_ROLE", "Role detected = $roleStr")
                    
                    userRole = when(roleStr.lowercase()) {
                        "admin" -> UserRole.ADMIN
                        "megadmin" -> UserRole.MEGADMIN
                        else -> UserRole.USER
                    }
                } else {
                    Log.d("DEBUG_ROLE", "Profile list is empty")
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
                // On crée un objet JSON explicite pour éviter l'erreur "Any"
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
        // Listen to Auth changes
        Log.d("DEBUG_ROLE", "ViewModel Init: starting sessionStatus collection")
        SupabaseManager.auth.sessionStatus.onEach { status ->
            Log.d("DEBUG_ROLE", "Auth status changed: $status")
            isLoggedIn = status is io.github.jan.supabase.gotrue.SessionStatus.Authenticated
            if (isLoggedIn) {
                fetchLockerRoomPosts()
                fetchUserRole()
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
                // Refresh role immediately after login
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
            try {
                SupabaseManager.auth.signOut()
            } catch (e: Exception) {}
            persistenceManager.clearAllData()
            isLoggedIn = false
            // Refresh local states
            seasonConfig = SeasonConfig()
            flashResults = null
            globalResults = null
            history = emptyList()
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
        Log.d("DEBUG_DELETE", "Attempting to delete post: $postId")
        viewModelScope.launch {
            try {
                SupabaseManager.db.from("locker_room_posts")
                    .delete { 
                        filter { eq("id", postId) } 
                    }
                Log.d("DEBUG_DELETE", "Delete request sent successfully")
                fetchLockerRoomPosts()
            } catch (e: Exception) {
                Log.e("DEBUG_DELETE", "Error deleting post", e)
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

    fun isMonthlyReviewDue(): Boolean {
        val today = LocalDate.now()
        return today.dayOfMonth == 1
    }

    fun clearSelectedResource() {
        selectedResource = null
    }

    fun clearSelectedTool() {
        selectedTool = null
    }
    
    fun clearAuthError() {
        authError = null
    }
}
