package com.example.coachapp.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Serializable
enum class CompetitionType(val label: String, @Serializable(with = ColorSerializer::class) val color: Color) {
    CHAMPIONSHIP("Championnat", Color(0xFF2196F3)),
    CUP("Coupe", Color(0xFFE91E63)),
    FRIENDLY("Amical", Color(0xFF4CAF50)),
    TOURNAMENT("Tournoi", Color(0xFFFF9800)),
    STAGE("Stage", Color(0xFF9C27B0)),
    SELECTION("Sélection", Color(0xFFFFC107))
}

@Serializable
data class CdeAssignment(
    val categorie: String,
    val sexe: String,          // "M" | "F"
    val role: String           // "selection_principal" | "selection_adjoint"
)

@Serializable
data class Stage(
    val id: Long,
    val categorie: String,
    val sexe: String,
    @SerialName("date_ouverture_inscription") val dateOuvertureInscription: String,
    @SerialName("date_fermeture_inscription") val dateFermetureInscription: String? = null,
    @SerialName("date_stage") val dateStage: String? = null
)

@Serializable
enum class UserRole {
    @SerialName("user") USER,
    @SerialName("admin") ADMIN,
    @SerialName("megadmin") MEGADMIN,
    @SerialName("president_club") PRESIDENT_CLUB,
    @SerialName("referent_tech") REFERENT_TECH
}

@Serializable
data class CoachProfile(
    @SerialName("first") val firstName: String = "",
    @SerialName("last") val lastName: String = "",
    val nickname: String = "",
    @SerialName("club") val clubName: String = "",
    @SerialName("level") val formationLevel: String = "Novice",
    val acquiredModules: List<String> = emptyList(),
    val goalPersonal: String = "",
    val goalCollective: String = "",
    val goal3Years: String = "",
    @SerialName("persona") val coachPersona: String = "", // e.g., "Le Tacticien", "Le Pédagogue", "Le Leader"
    val profilePictureUri: String? = null,
    val role: UserRole = UserRole.USER
)

@Serializable
data class Team(
    val id: String,
    val name: String,
    @Serializable(with = ColorSerializer::class) val color: Color,
    val projectType: String = "Développement",
    val objective: String = "",
    val format: TeamFormat = TeamFormat.SIX_SIX
)

@Serializable
enum class TeamFormat(val label: String, val playerCount: Int) {
    TWO_TWO("2x2 (M7/M9)", 2),
    THREE_THREE("3x3 (M11)", 3),
    FOUR_FOUR("4x4 (M13)", 4),
    SIX_SIX("6x6 (M15+)", 6)
}

@Serializable
data class TrainingSchedule(
    val id: String? = null,
    val teamId: String,
    val clubId: String? = null,
    @SerialName("day") @Serializable(with = DayOfWeekSerializer::class) val dayOfWeek: DayOfWeek,
    @SerialName("time") @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    @SerialName("duration") val durationMinutes: Int = 90,
    val terrain: String? = "Terrain 1"
)

@Serializable
data class TrainingSession(
    val id: String,
    val teamId: String,
    val clubId: String? = null,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    val durationMinutes: Int = 90,
    val terrain: String? = "Terrain 1",
    @SerialName("focus") val focusArea: String? = null,
    val attendance: Map<String, String> = emptyMap(), // PlayerID -> Status (present, absent, blesse, pending)
    val assessmentId: String? = null,
    // New fields for session construction
    val warmup: String = "",
    @SerialName("warmupDur") val warmupDuration: Int = 15,
    val drills: String = "",
    @SerialName("drillsDur") val drillsDuration: Int = 20,
    val smallGroupSituations: String = "",
    @SerialName("smallGroupDur") val smallGroupDuration: Int = 20,
    val collectiveGame: String = "",
    @SerialName("collectiveDur") val collectiveDuration: Int = 25,
    val trainerIntentions: String = "", 
    val coachIntentions: String = "",
    @SerialName("coachNotes") val coachNotes: String? = null,
    val isValidated: Boolean = false,
    val liveFeedback: String = "",
    @SerialName("futureNote") val noteForFutureMe: String = "",
    val saison: String = "2026-2027"
) {
    companion object {
        fun generateDeterministicId(teamId: String, date: LocalDate, startTime: LocalTime): String {
            val dateStr = date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) // yyyyMMdd
            val timeStr = startTime.format(java.time.format.DateTimeFormatter.ofPattern("HHmm"))
            val rawString = "session_${teamId}_${dateStr}_${timeStr}"
            // Generate a valid UUID version 3 from the deterministic string
            return java.util.UUID.nameUUIDFromBytes(rawString.toByteArray()).toString()
        }
    }
}

@Serializable
data class CompetitionEvent(
    val id: String,
    val teamId: String,
    val clubId: String? = null,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @SerialName("time") @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    val type: CompetitionType,
    val opponent: String,
    val location: String,
    val attendance: Map<String, String> = emptyMap(), // PlayerID -> Status
    val carpooling: Map<String, Int> = emptyMap(),    // PlayerID or "coach" -> capacity
    val coachNotes: String = "",
    val saison: String = "2026-2027"
)

@Serializable
data class SeasonConfig(
    val coachProfile: CoachProfile = CoachProfile(),
    val teams: List<Team> = emptyList(),
    val players: List<Player> = emptyList(),
    val trainingSchedules: List<TrainingSchedule> = emptyList(),
    val plannedTrainings: List<TrainingSession> = emptyList(),
    val competitions: List<CompetitionEvent> = emptyList(),
    @Serializable(with = LocalDateSerializer::class) val seasonStart: LocalDate = LocalDate.of(2026, 9, 1),
    @Serializable(with = LocalDateSerializer::class) val seasonEnd: LocalDate = LocalDate.of(2027, 6, 30),
    val isOnboardingCompleted: Boolean = false,
    val helpUsages: List<Long> = emptyList(), // Timestamps of "Help!" button usage
    val clubEvents: List<ClubEvent> = emptyList(),
    val clubEventRegistrations: Map<String, String> = emptyMap() // eventId -> status ("present", "absent", "pending")
)

@Serializable
enum class ClubEventType {
    TOURNOI, SOIRÉE, RÉUNION
}

@Serializable
enum class ClubEventScope {
    CLUB_ENTIER, ÉQUIPES_CIBLÉES, COACHS_CIBLÉS, EXTERNE_DA
}

@Serializable
data class ClubEvent(
    val id: String,
    val clubId: String,
    val title: String,
    val type: ClubEventType,
    val scope: ClubEventScope,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    val location: String,
    val description: String = "",
    val targetTeamIds: List<String> = emptyList(),
    val targetCoachIds: List<String> = emptyList(),
    val isExternalDA: Boolean = false,
    val registrationLink: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ClubEventRegistration(
    val eventId: String,
    val memberId: String, // Player ID or Coach ID
    val status: String,   // "confirmed", "declined"
    val timestamp: Long = System.currentTimeMillis()
)
