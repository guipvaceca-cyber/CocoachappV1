package com.example.coachapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    @SerialName("first") val firstName: String,
    @SerialName("last") val lastName: String,
    @SerialName("num") val number: Int,
    @SerialName("pos") val position: String,
    @SerialName("license") val licenseNumber: String = "",
    @SerialName("birth") val birthYear: Int = 0,
    @SerialName("practice") val yearsOfPractice: Int = 0,
    @SerialName("cat") val category: String = "", // M13, M15, M18, M21, Senior
    @SerialName("catYear") val categoryYear: Int = 1, // 1 or 2
    val notes: String = "",
    // History of skills evolution
    val assessmentHistory: List<PlayerAssessment> = emptyList(),
    // Current scores (most recent assessment or initial)
    @SerialName("tech") val techScore: Int = 0,
    @SerialName("tact") val tactScore: Int = 0,
    @SerialName("phys") val physicalScore: Int = 0,
    val vivierId: Long? = null // Link to Supabase vivier for sync
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
