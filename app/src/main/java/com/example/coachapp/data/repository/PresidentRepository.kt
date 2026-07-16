package com.example.coachapp.data.repository

import com.example.coachapp.data.model.*
import com.example.coachapp.data.TrainingSchedule
import com.example.coachapp.data.TrainingSession
import com.example.coachapp.data.CompetitionEvent
import com.example.coachapp.data.CompetitionType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PresidentRepository(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getClubId(): String? {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return null
            val response = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
            val profileList = json.decodeFromString<List<JsonObject>>(response.data)
            if (profileList.isNotEmpty()) profileList[0].getStr("club_id") else null
        } catch (e: Exception) {
            android.util.Log.e("PRESIDENT_REPO", "Erreur getClubId", e)
            null
        }
    }

    suspend fun getClubCode(clubId: String): String? {
        return try {
            val response = supabase.from("club")
                .select { filter { eq("id", clubId) } }
            val clubList = json.decodeFromString<List<JsonObject>>(response.data)
            clubList.firstOrNull()?.getStr("code_club")
        } catch (e: Exception) { null }
    }

    suspend fun getCollectifsAvecDetail(clubId: String, saisonSelectionnee: String): List<CollectifAvecDetail> {
        return try {
            val collectifsResponse = supabase.from("collectif")
                .select {
                    filter {
                        eq("club_id", clubId)
                        eq("saison", saisonSelectionnee)
                        neq("statut", "refuse")
                    }
                    order("categorie", Order.ASCENDING)
                    order("nom", Order.ASCENDING)
                }
            val collectifs = json.decodeFromString<List<JsonObject>>(collectifsResponse.data)
                .map { it.toCollectif() }

            val collectifIds = collectifs.map { it.id }
            if (collectifIds.isEmpty()) return emptyList()

            val rattachementsResponse = supabase.from("rattachement")
                .select(
                    Columns.raw("""
                        id, coach_id, collectif_id, poste, statut, saison, source,
                        profiles!coach_id(first_name, last_name)
                    """)
                ) {
                    filter {
                        isIn("collectif_id", collectifIds)
                        eq("saison", saisonSelectionnee)
                        neq("statut", "refuse")
                    }
                }
            val rattachements = json.decodeFromString<List<JsonObject>>(rattachementsResponse.data)
                .map { it.toRattachement() }

            val invitationsResponse = supabase.from("invitation")
                .select {
                    filter {
                        isIn("collectif_id", collectifIds)
                        eq("statut", "en_attente")
                    }
                }
            val invitations = json.decodeFromString<List<JsonObject>>(invitationsResponse.data)
                .map { it.toInvitation() }

            val joueursResponse = supabase.from("collectif_joueur")
                .select(
                    Columns.raw("""
                        id, collectif_id, source, joueur_id, joueur_manuel_id, poste,
                        cde_vivier!joueur_id(nom, prenom, categorie, date_naissance, niveau_surclassement),
                        joueur_manuel!joueur_manuel_id(nom, prenom, categorie, date_naissance)
                    """)
                ) {
                    filter { isIn("collectif_id", collectifIds) }
                }
            val allJoueursRaw = json.decodeFromString<List<JsonObject>>(joueursResponse.data)

            collectifs.map { collectif ->
                val joueursDuCollectif = allJoueursRaw
                    .filter { it.getStr("collectif_id") == collectif.id }
                    .mapNotNull { it.toJoueurCollectif() }

                CollectifAvecDetail(
                    collectif = collectif,
                    rattachements = rattachements.filter { it.collectifId == collectif.id },
                    invitationsEnAttente = invitations.filter { it.collectifId == collectif.id },
                    joueurs = joueursDuCollectif
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PRESIDENT_REPO", "Erreur getCollectifsAvecDetail", e)
            emptyList()
        }
    }

    suspend fun getFormatLimite(format: String): FormatLimite {
        return try {
            val response = supabase.from("format_limite")
                .select { filter { eq("format", format) } }
            val dataList = json.decodeFromString<List<JsonObject>>(response.data)
            val data = dataList.firstOrNull()
            FormatLimite(
                format = format,
                minJoueurs = data?.get("min_joueurs")?.jsonPrimitive?.int ?: 6,
                maxJoueurs = data?.get("max_joueurs")?.jsonPrimitive?.int ?: 14
            )
        } catch (e: Exception) {
            FormatLimite(format, 6, 14)
        }
    }

    suspend fun getJoueursCollectif(collectifId: String): List<JoueurCollectif> {
        return try {
            val response = supabase.from("collectif_joueur")
                .select(
                    Columns.raw("""
                        id, source, joueur_id, joueur_manuel_id, poste,
                        cde_vivier!joueur_id(nom, prenom, categorie, date_naissance, niveau_surclassement),
                        joueur_manuel!joueur_manuel_id(nom, prenom, categorie, date_naissance)
                    """)
                ) {
                    filter { eq("collectif_id", collectifId) }
                }
            val rows = json.decodeFromString<List<JsonObject>>(response.data)
            rows.mapNotNull { it.toJoueurCollectif() }
        } catch (e: Exception) {
            android.util.Log.e("PRESIDENT_REPO", "Erreur getJoueursCollectif", e)
            emptyList()
        }
    }

    suspend fun chargerVivierClub(clubCode: String, saison: String): List<JsonObject> {
        return try {
            val response = supabase.from("cde_vivier")
                .select(
                    Columns.raw("""
                        id, nom, prenom, categorie, sexe, date_naissance,
                        niveau_surclassement, club_code
                    """)
                ) {
                    filter {
                        eq("club_code", clubCode)
                        eq("saison", saison)
                    }
                }
            json.decodeFromString<List<JsonObject>>(response.data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun rechercherVivier(
        query: String,
        clubCode: String,
        saison: String
    ): List<JsonObject> {
        return try {
            val response = supabase.from("cde_vivier")
                .select(
                    Columns.raw("""
            id, nom, prenom, categorie, date_naissance,
            niveau_surclassement, club_code,
            categorie_ordre!fk_cde_vivier_categorie(ordre)
        """)
                ) {
                    filter {
                        eq("club_code", clubCode)
                        eq("saison", saison)
                        or {
                            ilike("nom", "$query%")
                            ilike("prenom", "$query%")
                        }
                    }
                }
            json.decodeFromString<List<JsonObject>>(response.data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCategorieOrdre(categorie: String): Int {
        return try {
            val response = supabase.from("categorie_ordre")
                .select { filter { eq("categorie", categorie) } }
            val dataList = json.decodeFromString<List<JsonObject>>(response.data)
            dataList.firstOrNull()?.get("ordre")?.jsonPrimitive?.int ?: 99
        } catch (e: Exception) { 99 }
    }

    suspend fun getTousLesOrdresCategorie(): Map<String, Int> {
        return try {
            val response = supabase.from("categorie_ordre").select()
            val dataList = json.decodeFromString<List<JsonObject>>(response.data)
            dataList.associate {
                (it.getStr("categorie") ?: "") to (it["ordre"]?.jsonPrimitive?.int ?: 99)
            }
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun ajouterJoueurCollectif(
        collectifId: String,
        joueurId: Long,
        poste: String?,
        saison: String
    ): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val data = buildJsonObject {
                put("collectif_id", collectifId)
                put("joueur_id", joueurId)
                put("source", "vivier")
                put("saison", saison)
                put("added_by", userId)
                if (poste != null) put("poste", poste)
            }
            supabase.from("collectif_joueur").insert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun retirerJoueurCollectif(collectifJoueurId: String): Result<Unit> {
        return try {
            supabase.from("collectif_joueur").delete {
                filter { eq("id", collectifJoueurId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ajouterJoueurManuel(
        collectifId: String,
        nom: String,
        prenom: String,
        numLicence: String?,
        dateNaissance: String?,
        categorie: String,
        saison: String
    ): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val manuelData = buildJsonObject {
                put("nom", nom)
                put("prenom", prenom)
                put("categorie", categorie)
                put("collectif_id", collectifId)
                put("coach_id", userId)
                if (numLicence != null) put("num_licence", numLicence)
                if (dateNaissance != null) put("date_naissance", dateNaissance)
                if (userId.isNotEmpty()) put("created_by", userId)
            }

            val response = supabase.from("joueur_manuel").insert(manuelData) { select() }
            val responseList = json.decodeFromString<List<JsonObject>>(response.data)
            val responseJson = responseList.firstOrNull() ?: throw Exception("Réponse vide")
            val joueurId = responseJson.getStr("id") ?: throw Exception("ID non généré")

            val linkData = buildJsonObject {
                put("collectif_id", collectifId)
                put("joueur_manuel_id", joueurId)
                put("source", "manuel")
                put("saison", saison)
                put("added_by", userId)
            }
            supabase.from("collectif_joueur").insert(linkData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun soumettreComposition(collectifId: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val data = buildJsonObject {
                put("compo_statut", "soumise_president")
                put("compo_soumise_at", Clock.System.now().toString())
            }
            supabase.from("collectif").update(data) {
                filter { eq("id", collectifId) }
            }
            val hist = buildJsonObject {
                put("collectif_id", collectifId)
                put("action", "soumise_coach")
                put("auteur_id", userId)
            }
            supabase.from("compo_historique").insert(hist)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun creerCollectif(
        clubId: String,
        nom: String,
        categorie: String,
        sexe: String,
        format: String,
        competition: String,
        saisonSelectionnee: String
    ): Result<Collectif> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Non connecté"))
            val data = buildJsonObject {
                put("club_id", clubId)
                put("nom", nom)
                put("categorie", categorie)
                put("sexe", sexe)
                put("format", format)
                put("competition", competition)
                put("saison", saisonSelectionnee)
                put("created_by", userId)
                put("statut", "actif")
            }
            val response = supabase.from("collectif").insert(data) { select() }
            val resultList = json.decodeFromString<List<JsonObject>>(response.data)
            val result = resultList.firstOrNull() ?: throw Exception("Réponse vide")
            Result.success(result.toCollectif())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun envoyerInvitation(
        collectifId: String,
        email: String?,
        telephone: String?,
        poste: Poste
    ): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Non connecté"))
            val data = buildJsonObject {
                put("collectif_id", collectifId)
                put("invite_par", userId)
                if (email != null) put("email", email)
                if (telephone != null) put("telephone", telephone)
                put("poste", poste.name.lowercase())
            }
            supabase.from("invitation").insert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validerCollectif(collectifId: String): Result<Unit> {
        return try {
            val data = buildJsonObject {
                put("statut", "en_attente_ct")
                put("validated_by_president_at", Clock.System.now().toString())
            }
            supabase.from("collectif").update(data) {
                filter { eq("id", collectifId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refuserEffectif(collectifId: String): Result<Unit> {
        return try {
            val data = buildJsonObject {
                put("compo_statut", "refusee")
            }
            supabase.from("collectif").update(data) {
                filter { eq("id", collectifId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayerCollectifs(): List<Collectif> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            
            // 1. Trouver le vivier_id lié à ce compte CoPlayer
            val profileResponse = supabase.from("coplayer_profil")
                .select { filter { eq("id", userId) } }
            val profile = json.decodeFromString<List<JsonObject>>(profileResponse.data).firstOrNull()
            val vivierId = profile?.get("vivier_id")?.jsonPrimitive?.longOrNull ?: return emptyList()

            // 2. Trouver les collectifs liés à ce vivier_id
            val linksResponse = supabase.from("collectif_joueur")
                .select { filter { eq("joueur_id", vivierId) } }
            val collectifIds = json.decodeFromString<List<JsonObject>>(linksResponse.data)
                .mapNotNull { it.getStr("collectif_id") }

            if (collectifIds.isEmpty()) return emptyList()

            // 3. Charger les détails des collectifs
            val collectifsResponse = supabase.from("collectif")
                .select { filter { isIn("id", collectifIds) } }
            json.decodeFromString<List<JsonObject>>(collectifsResponse.data)
                .map { it.toCollectif() }
        } catch (e: Exception) {
            android.util.Log.e("PRESIDENT_REPO", "Erreur getPlayerCollectifs", e)
            emptyList()
        }
    }

    suspend fun getCollectifPlanning(collectifId: String): List<TrainingSchedule> {
        return try {
            val response = supabase.from("collectif_planning")
                .select {
                    filter {
                        eq("collectif_id", collectifId)
                    }
                }
            val rows = json.decodeFromString<List<JsonObject>>(response.data)
            rows.map { it.toTrainingSchedule() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun supprimerCollectif(collectifId: String): Result<Unit> {
        return try {
            supabase.from("collectif").delete {
                filter { eq("id", collectifId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rattacherSoiMeme(collectifId: String, poste: Poste, saisonAssignation: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Non connecté"))
            val data = buildJsonObject {
                put("collectif_id", collectifId)
                put("coach_id", userId)
                put("poste", poste.name.lowercase())
                put("statut", "actif")
                put("saison", saisonAssignation)
                put("source", "manuel")
            }
            supabase.from("rattachement").insert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClubPlanning(clubId: String, saison: String): List<TrainingSchedule> {
        return try {
            val response = supabase.from("collectif_planning")
                .select {
                    filter {
                        eq("club_id", clubId)
                        eq("saison", saison)
                    }
                }
            val rows = json.decodeFromString<List<JsonObject>>(response.data)
            rows.map { it.toTrainingSchedule() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun upsertPlanning(schedule: TrainingSchedule): Result<Unit> {
        return try {
            val data = buildJsonObject {
                if (schedule.id != null) put("id", schedule.id)
                put("collectif_id", schedule.teamId)
                put("club_id", schedule.clubId)
                put("jour_semaine", schedule.dayOfWeek.value)
                put("heure_debut", schedule.startTime.toString())
                put("duree_minutes", schedule.durationMinutes)
                put("terrain", schedule.terrain)
                put("saison", "2026-2027") 
            }
            supabase.from("collectif_planning").upsert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun supprimerPlanning(scheduleId: String): Result<Unit> {
        return try {
            supabase.from("collectif_planning").delete {
                filter { eq("id", scheduleId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushSession(session: TrainingSession, clubId: String, saison: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            
            // 1. On pousse la fiche technique complète (Coach uniquement)
            val dataTechnique = buildJsonObject {
                put("id", session.id)
                put("collectif_id", session.teamId)
                put("date_seance", session.date.toString())
                put("saison", saison)
                put("warmup", session.warmup)
                put("drills", session.drills)
                put("situations", session.smallGroupSituations)
                put("collective_game", session.collectiveGame)
                put("coach_intentions", session.coachIntentions)
            }
            supabase.from("collectif_seances").upsert(dataTechnique)

            // 2. On pousse la séance publique (Visible par les joueurs)
            // Alignement avec la table 'seance' : id, collectif_id, coach_id, titre, date_heure, duree_minutes, lieu, type, statut, note_coach
            val dataPublique = buildJsonObject {
                put("id", session.id)
                put("collectif_id", session.teamId)
                put("coach_id", userId)
                put("titre", session.focusArea ?: "Séance sans thème")
                put("date_heure", "${session.date}T${session.startTime}")
                put("duree_minutes", session.durationMinutes)
                put("lieu", session.terrain ?: "Terrain 1")
                put("type", "entrainement")
                put("statut", "planifie")
                put("note_coach", session.coachNotes ?: "")
            }
            supabase.from("seance").upsert(dataPublique)

            if (session.isValidated) {
                convoquerJoueurs(session.id, session.teamId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PUSH_SESSION", "Erreur: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchSessionPresences(sessionId: String): Map<Long, String> {
        return try {
            val response = supabase.from("coplayer_presence")
                .select(Columns.raw("status, player_id, coplayer_profil(vivier_id)")) {
                    filter { eq("session_id", sessionId) }
                }
            val rows = json.decodeFromString<List<JsonObject>>(response.data)
            rows.associate { row ->
                val status = row.getStr("status")?.lowercase() ?: "pending"
                val profile = row["coplayer_profil"] as? JsonObject
                val vivierId = profile?.get("vivier_id")?.jsonPrimitive?.longOrNull ?: -1L
                vivierId to status
            }.filterKeys { it != -1L }
        } catch (e: Exception) {
            android.util.Log.e("PRESIDENT_REPO", "Erreur fetchSessionPresences", e)
            emptyMap()
        }
    }

    private suspend fun convoquerJoueurs(sessionId: String, collectifId: String) {
        try {
            val joueurs = getJoueursCollectif(collectifId)
            val vivierIds = joueurs.filter { it.source == "vivier" }.mapNotNull { it.vivierJoueurId }
            if (vivierIds.isEmpty()) return

            val profilesResponse = supabase.from("coplayer_profil")
                .select { filter { isIn("vivier_id", vivierIds) } }
            
            val profiles = json.decodeFromString<List<JsonObject>>(profilesResponse.data)
            val playerIds = profiles.mapNotNull { it.getStr("id") }

            val presences = playerIds.map { playerId ->
                buildJsonObject {
                    put("session_id", sessionId)
                    put("player_id", playerId)
                    put("status", "PENDING")
                }
            }
            
            if (presences.isNotEmpty()) {
                // ignoreDuplicates = true permet de NE PAS écraser les réponses (Présent/Absent) déjà faites par les joueurs
                supabase.from("coplayer_presence").upsert(presences, ignoreDuplicates = true)
            }
        } catch (e: Exception) {
            android.util.Log.e("PRESIDENT_REPO", "Erreur convocation automatique", e)
        }
    }

    suspend fun pushMatch(event: CompetitionEvent, clubId: String, saison: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val data = buildJsonObject {
                put("id", event.id)
                put("collectif_id", event.teamId)
                put("club_id", clubId)
                put("date_match", event.date.toString())
                put("heure_debut", event.startTime.toString())
                put("adversaire", event.opponent)
                put("lieu", event.location)
                put("type_match", event.type.label)
                val attendanceJson = buildJsonObject {
                    event.attendance.forEach { (k, v) -> put(k, v) }
                }
                put("attendance", attendanceJson)
                put("saison", saison)
                put("created_by", userId)
            }
            supabase.from("collectif_matchs").upsert(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JsonObject.toTrainingSchedule() = TrainingSchedule(
        id = getStr("id"),
        teamId = getStr("collectif_id") ?: "",
        clubId = getStr("club_id"),
        dayOfWeek = DayOfWeek.of(this["jour_semaine"]?.jsonPrimitive?.int ?: 1),
        startTime = LocalTime.parse(getStr("heure_debut") ?: "18:00"),
        durationMinutes = this["duree_minutes"]?.jsonPrimitive?.int ?: 90,
        terrain = getStr("terrain") ?: "Terrain 1"
    )

    private fun JsonObject.getStr(key: String) =
        this[key]?.jsonPrimitive?.content?.takeIf { it != "null" }

    private fun JsonObject.toCollectif() = Collectif(
        id = getStr("id") ?: "",
        clubId = getStr("club_id") ?: "",
        nom = getStr("nom") ?: "",
        categorie = getStr("categorie") ?: "",
        sexe = getStr("sexe") ?: "G",
        format = getStr("format") ?: "6x6",
        competition = getStr("competition") ?: "championnat",
        saison = getStr("saison") ?: "",
        statut = CollectifStatut.from(getStr("statut") ?: "actif"),
        compoStatut = getStr("compo_statut"),
        signalementArchive = this["signalement_archive"]?.jsonPrimitive?.content == "true",
        createdBy = getStr("created_by")
    )

    private fun JsonObject.toRattachement(): Rattachement {
        val profile = this["profiles"] as? JsonObject
        return Rattachement(
            id = getStr("id") ?: "",
            coachId = getStr("coach_id") ?: "",
            collectifId = getStr("collectif_id") ?: "",
            poste = Poste.from(getStr("poste") ?: "principal"),
            statut = RattachementStatut.from(getStr("statut") ?: "en_attente_ct"),
            saison = getStr("saison") ?: "",
            source = getStr("source") ?: "manuel",
            coachPrenom = profile?.getStr("first_name"),
            coachNom = profile?.getStr("last_name")
        )
    }

    private fun JsonObject.toInvitation() = Invitation(
        id = getStr("id") ?: "",
        collectifId = getStr("collectif_id") ?: "",
        invitePar = getStr("invite_par") ?: "",
        email = getStr("email"),
        telephone = getStr("telephone"),
        poste = Poste.from(getStr("poste") ?: "principal"),
        statut = getStr("statut") ?: "en_attente",
        expiresAt = getStr("expires_at") ?: ""
    )

    private fun JsonObject.toJoueurCollectif(): JoueurCollectif? {
        val source = getStr("source") ?: "vivier"
        return if (source == "vivier") {
            val vivier = this["cde_vivier"] as? JsonObject ?: return null
            JoueurCollectif(
                id = getStr("id") ?: "",
                nom = vivier.getStr("nom") ?: "",
                prenom = vivier.getStr("prenom") ?: "",
                categorie = vivier.getStr("categorie") ?: "",
                dateNaissance = vivier.getStr("date_naissance"),
                niveauSurclassement = vivier.getStr("niveau_surclassement"),
                source = "vivier",
                vivierJoueurId = this["joueur_id"]?.jsonPrimitive?.content?.toLongOrNull(),
                poste = getStr("poste")
            )
        } else {
            val manuel = this["joueur_manuel"] as? JsonObject ?: return null
            JoueurCollectif(
                id = getStr("id") ?: "",
                nom = manuel.getStr("nom") ?: "",
                prenom = manuel.getStr("prenom") ?: "",
                categorie = manuel.getStr("categorie") ?: "",
                dateNaissance = manuel.getStr("date_naissance"),
                niveauSurclassement = null,
                source = "manuel",
                vivierJoueurId = null,
                poste = getStr("poste")
            )
        }
    }
}
