package com.example.coachapp.data.repository

import com.example.coachapp.data.*
import com.example.coachapp.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TrainingRepository(
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
            android.util.Log.e("TRAINING_REPO", "Erreur getClubId", e)
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
            android.util.Log.e("TRAINING_REPO", "Erreur fetchSessionPresences", e)
            emptyMap()
        }
    }

    private suspend fun convoquerJoueurs(sessionId: String, collectifId: String) {
        try {
            // Note: Simplification here since getJoueursCollectif is in PresidentRepo
            val joueursResponse = supabase.from("collectif_joueur")
                .select(Columns.raw("source, joueur_id")) {
                    filter { eq("collectif_id", collectifId) }
                }
            val rows = json.decodeFromString<List<JsonObject>>(joueursResponse.data)
            val vivierIds = rows.filter { it.getStr("source") == "vivier" }
                .mapNotNull { it.get("joueur_id")?.jsonPrimitive?.longOrNull }

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
                supabase.from("coplayer_presence").upsert(presences) {
                    ignoreDuplicates = true
                }
                android.util.Log.d("TRAINING_REPO", "Convocations réussies: ${presences.size} joueurs")
            } else {
                android.util.Log.w("TRAINING_REPO", "Aucun compte CoPlayer trouvé pour les joueurs de ce collectif")
            }
        } catch (e: Exception) {
            android.util.Log.e("TRAINING_REPO", "Erreur convocation automatique", e)
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
                val carpoolingJson = buildJsonObject {
                    event.carpooling.forEach { (k, v) -> put(k, v) }
                }
                put("carpooling", carpoolingJson)
                put("coach_notes", event.coachNotes)
                put("saison", saison)
                put("created_by", userId)
            }
            supabase.from("collectif_matchs").upsert(data)
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

    private fun JsonObject.toTrainingSchedule() = TrainingSchedule(
        id = getStr("id"),
        teamId = getStr("collectif_id") ?: "",
        clubId = getStr("club_id"),
        dayOfWeek = DayOfWeek.of(get("jour_semaine")?.jsonPrimitive?.int ?: 1),
        startTime = LocalTime.parse(getStr("heure_debut") ?: "18:30"),
        durationMinutes = get("duree_minutes")?.jsonPrimitive?.int ?: 90,
        terrain = getStr("terrain")
    )

    private fun JsonObject.getStr(key: String) = get(key)?.jsonPrimitive?.contentOrNull
}
