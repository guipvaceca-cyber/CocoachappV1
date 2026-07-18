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
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

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

    var isFetchingProfile by mutableStateOf(false)
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

    var vivierPrincipal by mutableStateOf<List<com.example.coachapp.ui.screens.JoueurVivier>>(emptyList())
        private set

    var vivierInferieur by mutableStateOf<List<com.example.coachapp.ui.screens.JoueurVivier>>(emptyList())
        private set

    var convocationEnCours by mutableStateOf<Long?>(null)
        private set

    var slotsPersistes by mutableStateOf<List<com.example.coachapp.ui.screens.JoueurSlot?>>(emptyList())
        private set

    var bancPersiste by mutableStateOf<List<com.example.coachapp.ui.screens.JoueurSlot?>>(emptyList())
        private set
    var userRole by mutableStateOf(UserRole.USER)
        private set

    var presidentClubId: String? = null

    var isCoachCde by mutableStateOf(false)
        private set

    var isStageOpen by mutableStateOf(false)
        private set

    // Liste des affectations CDE (ex: M13M selection_principal, M18F selection_principal)
    var cdeAssignments by mutableStateOf<List<CdeAssignment>>(emptyList())
        private set

    var pendingInvitations by mutableStateOf<List<Team>>(emptyList())
        private set

    var selectionAlerteMessage by mutableStateOf<String?>(null)
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
    fun chargerVivier() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseManager.db
                    .from("vue_vivier_coach")
                    .select()
                val body = response.data
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val joueurs = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)

                val principal = mutableListOf<com.example.coachapp.ui.screens.JoueurVivier>()
                val inferieur = mutableListOf<com.example.coachapp.ui.screens.JoueurVivier>()

                joueurs.forEach { j ->
                    val priorite = j["priorite_affichage"]?.toString()?.replace("\"","")?.toIntOrNull() ?: 1
                    val joueur = com.example.coachapp.ui.screens.JoueurVivier(
                        id           = j["id"]?.toString()?.replace("\"","") ?: "",
                        nom          = j["nom"]?.toString()?.replace("\"","") ?: "",
                        prenom       = j["prenom"]?.toString()?.replace("\"","") ?: "",
                        club         = j["club_nom"]?.toString()?.replace("\"","") ?: "",
                        categorie    = j["categorie"]?.toString()?.replace("\"","") ?: "",
                        estSurclasse = j["est_surclasse"]?.toString() == "true",
                        taille       = j["taille"]?.toString()?.replace("\"","")?.toIntOrNull() ?: 0
                    )
                    if (priorite == 1) principal.add(joueur)
                    else inferieur.add(joueur)
                }

                withContext(Dispatchers.Main) {
                    vivierPrincipal = principal
                    vivierInferieur = inferieur
                }
            } catch (e: Exception) {
                android.util.Log.e("VIVIER", "Erreur chargement vivier", e)
            }
        }
    }
    fun creerOuChargerConvocation(categorie: String, quota: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseManager.auth.currentUserOrNull()?.id ?: return@launch
                Log.d("CONVOCATION", "Recherche BROUILLON pour coach: $userId, catégorie: $categorie")

                // Cherche une convocation BROUILLON existante
                val response = SupabaseManager.db
                    .from("cde_convocation")
                    .select {
                        filter {
                            eq("coach_id", userId)
                            eq("categorie", categorie)
                            eq("statut", "BROUILLON")
                        }
                    }

                val body = response.data
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val convocations = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)
                Log.d("CONVOCATION", "Convocation existantes trouvées: ${convocations.size}")

                val convocationId = if (convocations.isNotEmpty()) {
                    convocations[0]["id"]?.toString()?.replace("\"","")?.toLongOrNull()
                } else {
                    Log.d("CONVOCATION", "Création nouvelle convocation BROUILLON")
                    // Crée une nouvelle convocation
                    val nouvelle = mapOf(
                        "coach_id"         to userId,
                        "categorie"        to categorie,
                        "niveau"           to "selection",
                        "statut"           to "BROUILLON",
                        "quota_principal"  to quota,
                        "quota_banc"       to quota
                    )
                    val createResponse = SupabaseManager.db
                        .from("cde_convocation")
                        .insert(nouvelle) {
                            select()
                        }
                    val createBody = createResponse.data
                    val created = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(createBody)
                    val newId = created.firstOrNull()?.get("id")?.toString()?.replace("\"","")?.toLongOrNull()
                    Log.d("CONVOCATION", "Nouvelle convocation créée avec ID: $newId")
                    newId
                }

                convocationEnCours = convocationId

                // Charge les slots existants
                convocationId?.let { chargerSlots(it, quota) }

            } catch (e: Exception) {
                android.util.Log.e("CONVOCATION", "Erreur création/chargement", e)
            }
        }
    }

    fun chargerSlots(convocationId: Long, quota: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseManager.db
                    .from("cde_slot")
                    .select {
                        filter { eq("convocation_id", convocationId) }
                    }

                val body = response.data
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val slots = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)
                Log.d("CONVOCATION", "Slots chargés depuis Supabase: ${slots.size}")

                val principal = MutableList<com.example.coachapp.ui.screens.JoueurSlot?>(quota) { null }
                val banc = MutableList<com.example.coachapp.ui.screens.JoueurSlot?>(quota) { null }

                slots.forEach { s ->
                    val ordre = s["ordre"]?.toString()?.replace("\"","")?.toIntOrNull()?.minus(1) ?: return@forEach
                    val type = s["type_tableau"]?.toString()?.replace("\"","") ?: return@forEach
                    val slot = com.example.coachapp.ui.screens.JoueurSlot(
                        id       = s["joueur_id"]?.toString()?.replace("\"","") ?: "",
                        nom      = s["joueur_nom"]?.toString()?.replace("\"","") ?: "",
                        prenom   = s["joueur_prenom"]?.toString()?.replace("\"","") ?: "",
                        club     = s["joueur_club"]?.toString()?.replace("\"","") ?: "",
                        categorie = s["joueur_categorie"]?.toString()?.replace("\"","") ?: "",
                        statut   = com.example.coachapp.ui.screens.StatutSlot.valueOf(
                            s["statut"]?.toString()?.replace("\"","") ?: "CONVOQUE"
                        )
                    )
                    if (type == "PRINCIPAL" && ordre in 0 until quota) principal[ordre] = slot
                    if (type == "BANC" && ordre in 0 until quota) banc[ordre] = slot
                }

                withContext(Dispatchers.Main) {
                    slotsPersistes = principal
                    bancPersiste = banc
                    traiterRemplacementsAutomatiques()
                }
            } catch (e: Exception) {
                android.util.Log.e("CONVOCATION", "Erreur chargement slots", e)
            }
        }
    }

    private fun traiterRemplacementsAutomatiques() {
        var aFaitUnChangement = false
        val principal = slotsPersistes.toMutableList()
        val banc = bancPersiste.toMutableList()
        var alerte = false

        principal.forEachIndexed { index, slot ->
            if (slot?.statut == com.example.coachapp.ui.screens.StatutSlot.INDISPONIBLE) {
                // Chercher un remplaçant
                val idxRemplacant = banc.indexOfFirst { it != null }
                if (idxRemplacant != -1) {
                    val remplacant = banc[idxRemplacant]!!.copy(statut = com.example.coachapp.ui.screens.StatutSlot.INSCRIT)
                    principal[index] = remplacant
                    banc[idxRemplacant] = null
                    aFaitUnChangement = true
                    Log.d("CONVOCATION", "Remplacement auto : ${remplacant.nom} prend la place de ${slot.nom}")
                } else {
                    alerte = true
                }
            }
        }

        if (aFaitUnChangement) {
            sauvegarderSelection(principal, banc)
        }
        
        selectionAlerteMessage = if (alerte) "Une place titulaire est vacante et votre banc est vide !" else null
    }

    fun sauvegarderSlot(
        index: Int,
        type: String,
        joueur: com.example.coachapp.ui.screens.JoueurSlot?
    ) {
        val convId = convocationEnCours ?: return

        // Mise à jour locale immédiate pour la fluidité UI
        val currentList = if (type == "PRINCIPAL") slotsPersistes else bancPersiste
        val updatedList = currentList.toMutableList()
        if (index in updatedList.indices) {
            updatedList[index] = joueur
            if (type == "PRINCIPAL") slotsPersistes = updatedList else bancPersiste = updatedList
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (joueur == null) {
                    // Supprime le slot
                    SupabaseManager.db.from("cde_slot").delete {
                        filter {
                            eq("convocation_id", convId)
                            eq("type_tableau", type)
                            eq("ordre", index + 1)
                        }
                    }
                } else {
                    // Upsert le slot
                    val row = mapOf(
                        "convocation_id"   to convId,
                        "joueur_id"        to joueur.id.toLongOrNull(),
                        "type_tableau"     to type,
                        "ordre"            to (index + 1),
                        "statut"           to joueur.statut.name,
                        "joueur_nom"       to joueur.nom,
                        "joueur_prenom"    to joueur.prenom,
                        "joueur_club"      to joueur.club,
                        "joueur_categorie" to joueur.categorie
                    )
                    SupabaseManager.db.from("cde_slot").upsert(row, onConflict = "convocation_id,type_tableau,ordre")
                }
            } catch (e: Exception) {
                android.util.Log.e("CONVOCATION", "Erreur sauvegarde slot", e)
            }
        }
    }

    fun sauvegarderSelection(
        principal: List<com.example.coachapp.ui.screens.JoueurSlot?>,
        banc: List<com.example.coachapp.ui.screens.JoueurSlot?>
    ) {
        // Mise à jour locale immédiate
        slotsPersistes = principal
        bancPersiste = banc

        val convId = convocationEnCours ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // On prépare tous les slots non nulls
                val rows = mutableListOf<Map<String, Any?>>()
                
                principal.forEachIndexed { index, joueur ->
                    if (joueur != null) {
                        rows.add(mapOf(
                            "convocation_id"   to convId,
                            "joueur_id"        to joueur.id,
                            "type_tableau"     to "PRINCIPAL",
                            "ordre"            to (index + 1),
                            "statut"           to joueur.statut.name,
                            "joueur_nom"       to joueur.nom,
                            "joueur_prenom"    to joueur.prenom,
                            "joueur_club"      to joueur.club,
                            "joueur_categorie" to joueur.categorie
                        ))
                    }
                }
                
                banc.forEachIndexed { index, joueur ->
                    if (joueur != null) {
                        rows.add(mapOf(
                            "convocation_id"   to convId,
                            "joueur_id"        to joueur.id,
                            "type_tableau"     to "BANC",
                            "ordre"            to (index + 1),
                            "statut"           to joueur.statut.name,
                            "joueur_nom"       to joueur.nom,
                            "joueur_prenom"    to joueur.prenom,
                            "joueur_club"      to joueur.club,
                            "joueur_categorie" to joueur.categorie
                        ))
                    }
                }

                if (rows.isNotEmpty()) {
                    SupabaseManager.db.from("cde_slot").upsert(rows, onConflict = "convocation_id,type_tableau,ordre")
                }
                
                Log.d("CONVOCATION", "Sauvegarde globale réussie : ${rows.size} slots")
            } catch (e: Exception) {
                Log.e("CONVOCATION", "Erreur sauvegarde globale", e)
            }
        }
    }

    fun finaliserConvocation() {
        val convId = convocationEnCours ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Marque la convocation comme PRÊTE (ou autre statut final)
                SupabaseManager.db.from("cde_convocation").update(
                    mapOf("statut" to "READY")
                ) {
                    filter { eq("id", convId) }
                }
                Log.d("CONVOCATION", "Convocation $convId marquée comme READY")
            } catch (e: Exception) {
                Log.e("CONVOCATION", "Erreur finalisation", e)
            }
        }
    }

    fun envoyerSelection(categorie: String) {
        val userId = SupabaseManager.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Marquer la convocation comme ENVOYE
                SupabaseManager.db.from("cde_convocation").update(
                    mapOf("statut" to "ENVOYE")
                ) {
                    filter {
                        eq("coach_id", userId)
                        eq("categorie", categorie)
                        or {
                            eq("statut", "BROUILLON")
                            eq("statut", "READY")
                        }
                    }
                }

                // 2. Verrouiller les slots du tableau principal (passer en INSCRIT)
                val convId = convocationEnCours ?: return@launch
                SupabaseManager.db.from("cde_slot").update(
                    mapOf("statut" to "INSCRIT")
                ) {
                    filter {
                        eq("convocation_id", convId)
                        eq("type_tableau", "PRINCIPAL")
                        eq("statut", "CONVOQUE")
                    }
                }

                Log.d("CONVOCATION", "Sélection $categorie envoyée et slots verrouillés")
                
                // Rafraîchir pour voir le nouveau statut
                creerOuChargerConvocation(categorie, 14) 
            } catch (e: Exception) {
                Log.e("CONVOCATION", "Erreur envoi sélection", e)
            }
        }
    }
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

    fun checkStageOpening(categorie: String, sexe: String) {
        viewModelScope.launch {
            try {
                val response = SupabaseManager.db.from("stage")
                    .select {
                        filter {
                            eq("categorie", categorie)
                            eq("sexe", sexe)
                        }
                    }
                val stages = response.decodeList<Stage>()
                if (stages.isNotEmpty()) {
                    val stage = stages[0]
                    val openingDate = LocalDate.parse(stage.dateOuvertureInscription)
                    val today = LocalDate.now()
                    isStageOpen = today.isAfter(openingDate) || today.isEqual(openingDate)
                    Log.d("STAGE", "Stage trouvé pour $categorie $sexe : ouverture le $openingDate. isStageOpen = $isStageOpen")
                } else {
                    isStageOpen = false
                    Log.d("STAGE", "Aucun stage trouvé pour $categorie $sexe")
                }
            } catch (e: Exception) {
                Log.e("STAGE", "Erreur checkStageOpening", e)
                isStageOpen = false
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
                isFetchingProfile = true
                Log.d("DEBUG_ROLE", "Début fetchUserRole pour ${user.id}")
                
                // 1. Lire le profil existant
                val response = SupabaseManager.db.from("profiles")
                    .select { filter { eq("id", user.id) } }
                val body = response.data
                Log.d("DEBUG_ROLE", "Profil body = $body")
                
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val profileList = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)

                if (profileList.isNotEmpty()) {
                    val profile = profileList[0]
                    fun getStr(key: String) = profile[key]?.toString()?.replace("\"", "")?.takeIf { it != "null" }
                    
                    val roleStr = getStr("role") ?: "user"
                    Log.d("DEBUG_ROLE", "roleStr du profil = $roleStr")
                    
                    val detectedRole = when(roleStr.lowercase()) {
                        "admin" -> UserRole.ADMIN
                        "megadmin" -> UserRole.MEGADMIN
                        "president_club" -> UserRole.PRESIDENT_CLUB
                        "referent_tech" -> UserRole.REFERENT_TECH
                        else -> UserRole.USER
                    }
                    userRole = detectedRole
                    Log.d("DEBUG_ROLE", "userRole détecté = $userRole")
                    
                    // On pré-remplit le profil local avec les infos Supabase
                    val currentProfile = seasonConfig.coachProfile
                    val updatedProfile = currentProfile.copy(
                        firstName = getStr("first_name") ?: currentProfile.firstName,
                        lastName = getStr("last_name") ?: currentProfile.lastName,
                        clubName = getStr("club_nom") ?: currentProfile.clubName,
                        role = detectedRole
                    )
                    updateSeasonConfig(seasonConfig.copy(coachProfile = updatedProfile))
                    
                    isCoachCde = profile["is_coach_cde"]?.toString() == "true"
                    if (isCoachCde) {
                        val cat = getStr("cde_categorie") ?: "M15"
                        val sexe = getStr("cde_sexe") ?: "M"
                        val role = getStr("cde_role") ?: "selection_adjoint"
                        cdeAssignments = listOf(CdeAssignment(cat, sexe, role))
                        checkStageOpening(cat, sexe)
                    }
                    
                    if (detectedRole == UserRole.PRESIDENT_CLUB || detectedRole == UserRole.REFERENT_TECH) {
                        presidentClubId = getStr("club_id")
                        Log.d("DEBUG_ROLE", "Club ID récupéré du profil = $presidentClubId")
                    }
                }

                // 2. Vérifier les rôles additionnels (table user_roles)
                try {
                    val userRolesResponse = SupabaseManager.db.from("user_roles")
                        .select { filter { eq("user_id", user.id) } }
                    val userRolesList = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(userRolesResponse.data)
                    
                    val appRoles = userRolesList.map { it["role"]?.toString()?.replace("\"", "") ?: "" }
                    Log.d("DEBUG_ROLE", "Rôles additionnels (user_roles) = $appRoles")

                    if ("admin_ct" in appRoles) {
                        userRole = UserRole.ADMIN
                    } else if ("president_club" in appRoles) {
                        userRole = UserRole.PRESIDENT_CLUB
                        val clubEntry = userRolesList.firstOrNull { it["role"]?.toString()?.replace("\"", "") == "president_club" }
                        presidentClubId = clubEntry?.get("club_id")?.toString()?.replace("\"", "")
                    } else if ("referent_tech" in appRoles) {
                        userRole = UserRole.REFERENT_TECH
                        val clubEntry = userRolesList.firstOrNull { it["role"]?.toString()?.replace("\"", "") == "referent_tech" }
                        presidentClubId = clubEntry?.get("club_id")?.toString()?.replace("\"", "")
                    }
                } catch (e: Exception) {
                    Log.w("DEBUG_ROLE", "Erreur (non critique) lecture table user_roles: ${e.message}")
                }

                Log.d("DEBUG_ROLE", "userRole FINAL = $userRole | ClubID = $presidentClubId")

                if (userRole == UserRole.MEGADMIN) fetchAdminAlerts()
                
                // 3. Charger les invitations pour l'onboarding
                fetchClubInvitations()

            } catch (e: Exception) {
                Log.e("DEBUG_ROLE", "CRITICAL ERROR in fetchUserRole", e)
            } finally {
                isFetchingProfile = false
            }
        }
    }

    fun fetchClubInvitations() {
        val user = SupabaseManager.auth.currentUserOrNull() ?: return
        val email = user.email
        
        viewModelScope.launch {
            try {
                // On cherche les invitations par email
                val response = SupabaseManager.db.from("invitation")
                    .select(Columns.raw("*, collectif(*)")) {
                        filter {
                            eq("email", email ?: "")
                            eq("statut", "en_attente")
                        }
                    }
                
                val body = response.data
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val invites = json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(body)
                
                val detectedTeams = invites.mapNotNull { inv ->
                    val coll = inv["collectif"]?.let { if (it is kotlinx.serialization.json.JsonObject) it else null }
                    if (coll != null) {
                        val cat = coll["categorie"]?.toString()?.replace("\"", "") ?: ""
                        val sexe = coll["sexe"]?.toString()?.replace("\"", "") ?: ""
                        val nom = coll["nom"]?.toString()?.replace("\"", "") ?: ""
                        
                        Team(
                            id = coll["id"]?.toString()?.replace("\"", "") ?: UUID.randomUUID().toString(),
                            name = "$cat $sexe ($nom)",
                            color = Color(0xFF4CAF50), // Vert par défaut pour les équipes officielles
                            objective = "Projet Club Officiel",
                            format = when(coll["format"]?.toString()?.replace("\"", "")) {
                                "2x2" -> TeamFormat.TWO_TWO
                                "3x3" -> TeamFormat.THREE_THREE
                                "4x4" -> TeamFormat.FOUR_FOUR
                                else -> TeamFormat.SIX_SIX
                            }
                        )
                    } else null
                }
                
                pendingInvitations = detectedTeams
                Log.d("DEBUG_INVITE", "Invitations trouvées : ${pendingInvitations.size}")
                
            } catch (e: Exception) {
                Log.e("DEBUG_INVITE", "Erreur fetchClubInvitations", e)
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
                chargerCalendrierPrevisionnel()
                chargerVivier()
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
                // ← supprime fetchUserRole() ici, le init s'en charge
            } catch (e: Exception) {
                authError = "Erreur de connexion : ${e.message} / ${e.cause} / ${e::class.simpleName}"
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
            cdeAssignments = emptyList()
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
                ).map { event ->
                    // Pre-fill attendance with all players of this team
                    val players = seasonConfig.players.filter { it.teamId == team.id }
                    val initialAttendance = players.associate { it.id to "pending" }
                    event.copy(attendance = initialAttendance)
                }
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

    fun updateClubEventAttendance(eventId: String, status: String) {
        val updatedRegistrations = seasonConfig.clubEventRegistrations.toMutableMap()
        updatedRegistrations[eventId] = status
        updateSeasonConfig(seasonConfig.copy(clubEventRegistrations = updatedRegistrations))
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

    fun preRegisterFormation(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseManager.auth.currentUserOrNull()?.id ?: return@launch
                val entry = mapOf(
                    "user_id" to userId,
                    "formation_type" to type,
                    "timestamp" to System.currentTimeMillis(),
                    "statut" to "INTENTION"
                )
                SupabaseManager.db.from("formation_pre_registrations").insert(entry)
                Log.d("FORMATION", "Pré-inscription réussie pour $type")
            } catch (e: Exception) {
                Log.e("FORMATION", "Erreur pré-inscription $type", e)
            }
        }
    }

    fun clearSelectedResource() { selectedResource = null }
    fun clearSelectedTool() { selectedTool = null }
    fun clearAuthError() { authError = null }
}