package com.example.coachapp.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class LaboCategory(val label: String) {
    TECHNIQUE("Technique Individuelle"),
    TACTIQUE("Systèmes & Lecture"),
    PHYSIQUE("Physique & Tonicité"),
    MENTAL("Mental & Cohésion")
}

@Serializable
enum class LaboTab { CORPUS, EXTERNAL }

@Serializable
data class LaboResource(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val authorNickname: String,
    val category: LaboCategory,
    val description: String,
    val focalPoints: List<FocalPoint>, // Different angles for the same exercise
    val crashTests: List<CrashTestFeedback> = emptyList(),
    val difficulty: Int = 1, // 1 to 5
    val setupImage: Int? = null,
    val versionsCount: Int = 1
)

@Serializable
data class FocalPoint(
    val title: String, // e.g. "Timing", "Posture", "Communication"
    val instruction: String // "Focus sur le transfert d'appui..."
)

@Serializable
data class CrashTestFeedback(
    val coachNickname: String,
    val result: String, // "Succès", "Échec", "Adapté"
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

val laboCorpus = listOf(
    LaboResource(
        title = "Le Triangle Multi-Ballons",
        authorNickname = "Coach Ben",
        category = LaboCategory.TECHNIQUE,
        description = "Situation de haute répétition avec 3 ballons injectés successivement. Travail de la transition Manchette -> Déplacement -> Attaque.",
        focalPoints = listOf(
            FocalPoint("Timing", "Gestion de l'intervalle entre le ballon 1 et 2 pour éviter la précipitation."),
            FocalPoint("Posture", "Gainage du bassin lors du contact du 3ème ballon sous fatigue."),
            FocalPoint("Mental", "Garder sa lucidité malgré l'accumulation visuelle de balles.")
        ),
        crashTests = listOf(
            CrashTestFeedback("Pédagogue26", "Échec", "En M13, 3 ballons c'est trop. Je recommande de passer à 2 ballons pour garder la qualité technique."),
            CrashTestFeedback("Tacticien07", "Succès", "Excellent pour mes M18 Elite. On a ajouté un 4ème ballon 'surprise' pour tester la réaction.")
        ),
        difficulty = 3,
        versionsCount = 2
    ),
    LaboResource(
        title = "Lecture de Bloc en Relance",
        authorNickname = "Expert CTD",
        category = LaboCategory.TACTIQUE,
        description = "Situation de jeu réduit 3x3 avec lecture obligatoire du placement du bloc adverse avant l'attaque.",
        focalPoints = listOf(
            FocalPoint("Vision", "Prise d'information avant le saut."),
            FocalPoint("Communication", "Annonce vocale du passeur sur le trou dans le bloc.")
        ),
        crashTests = listOf(
            CrashTestFeedback("Coach Léo", "Adapté", "Fonctionne bien si on matérialise les zones de bloc avec des plots au début.")
        ),
        difficulty = 4,
        versionsCount = 1
    )
)
