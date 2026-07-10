package com.example.coachapp.data

import kotlinx.serialization.Serializable

@Serializable
data class PlayerAssessment(
    val date: Long,
    val type: String, // "Semaine", "Mois", "Trimestre", "Mi-Saison", "Saison"
    val techScore: Int,
    val tactScore: Int,
    val physicalScore: Int,
    val coachComment: String = ""
)

@Serializable
data class Player(
    val id: String,
    val teamId: String,
    val firstName: String,
    val lastName: String,
    val number: Int,
    val position: String,
    val licenseNumber: String = "",
    val birthYear: Int = 0,
    val yearsOfPractice: Int = 0,
    val category: String = "", // M13, M15, M18, M21, Senior
    val categoryYear: Int = 1, // 1 or 2
    val notes: String = "",
    // History of skills evolution
    val assessmentHistory: List<PlayerAssessment> = emptyList(),
    // Current scores (most recent assessment or initial)
    val techScore: Int = 0,
    val tactScore: Int = 0,
    val physicalScore: Int = 0
) {
    val fullName: String get() = "$firstName $lastName"
}

val defaultPlayers = listOf(
    Player("p1", "default", "Jean", "Passeur", 1, "Passeur"),
    Player("p2", "default", "Marc", "Pointu", 9, "Pointu"),
    Player("p3", "default", "Luc", "Central", 4, "Central"),
    Player("p4", "default", "Paul", "RA", 7, "Réceptionneur-Attaquant"),
    Player("p5", "default", "Rémi", "Libero", 10, "Libero"),
    Player("p6", "default", "Pierre", "Central", 2, "Central")
)
