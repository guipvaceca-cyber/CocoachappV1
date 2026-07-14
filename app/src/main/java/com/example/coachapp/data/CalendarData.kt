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
    TOURNAMENT("Tournoi", Color(0xFFFF9800))
}

@Serializable
data class CdeAssignment(
    val categorie: String,
    val sexe: String,          // "M" | "F"
    val role: String           // "selection_principal" | "selection_adjoint"
)

@Serializable
enum class UserRole {
    @SerialName("user") USER,
    @SerialName("admin") ADMIN,
    @SerialName("megadmin") MEGADMIN,
    @SerialName("president_club") PRESIDENT_CLUB
}

@Serializable
data class CoachProfile(
    val firstName: String = "",
    val lastName: String = "",
    val nickname: String = "",
    val clubName: String = "",
    val formationLevel: String = "Novice",
    val goalPersonal: String = "",
    val goalCollective: String = "",
    val goal3Years: String = "",
    val coachPersona: String = "", // e.g., "Le Tacticien", "Le Pédagogue", "Le Leader"
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
    @Serializable(with = DayOfWeekSerializer::class) val dayOfWeek: DayOfWeek,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    val durationMinutes: Int = 90,
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
    val focusArea: String? = null,
    val attendance: Map<String, String> = emptyMap(), // PlayerID -> Status (present, absent, blesse, pending)
    val assessmentId: String? = null,
    // New fields for session construction
    val warmup: String = "",
    val warmupDuration: Int = 15,
    val drills: String = "",
    val drillsDuration: Int = 20,
    val smallGroupSituations: String = "",
    val smallGroupDuration: Int = 20,
    val collectiveGame: String = "",
    val collectiveDuration: Int = 25,
    val trainerIntentions: String = "", 
    val coachIntentions: String = "",
    @SerialName("coach_notes") val coachNotes: String? = null,
    val isValidated: Boolean = false,
    val liveFeedback: String = "",
    val noteForFutureMe: String = "",
    val saison: String = "2026-2027"
)

@Serializable
data class CompetitionEvent(
    val id: String,
    val teamId: String,
    val clubId: String? = null,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class) val startTime: LocalTime,
    val type: CompetitionType,
    val opponent: String,
    val location: String,
    val attendance: Map<String, String> = emptyMap(), // Convocations CoPlayer
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
    @Serializable(with = LocalDateSerializer::class) val seasonStart: LocalDate = LocalDate.of(2024, 9, 1),
    @Serializable(with = LocalDateSerializer::class) val seasonEnd: LocalDate = LocalDate.of(2025, 6, 30),
    val isOnboardingCompleted: Boolean = false,
    val helpUsages: List<Long> = emptyList() // Timestamps of "Help!" button usage
)
