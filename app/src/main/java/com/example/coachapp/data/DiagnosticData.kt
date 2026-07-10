package com.example.coachapp.data

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: String,
    val text: String,
    val score: Int = 0
)

@Serializable
data class Domain(
    val id: String,
    val title: String,
    val questions: List<Question>
)

@Serializable
enum class AssessmentType { FLASH, GLOBAL }

/**
 * DIAGNOSTIC FLASH (Post-Session)
 */
val flashDiagnosticData = listOf(
    Domain(
        id = "pedagogie_flash",
        title = "Pédagogie & Com",
        questions = listOf(
            Question("f_ped_1", "Clarté : Mes consignes ont été comprises immédiatement."),
            Question("f_ped_2", "Corrections : Mes interventions étaient ciblées et opportunes.")
        )
    ),
    Domain(
        id = "rythme_flash",
        title = "Rythme & Timing",
        questions = listOf(
            Question("f_ryt_1", "Temps de balle : Volume de touches optimal (peu d'attente)."),
            Question("f_ryt_2", "Timing : J'ai respecté le temps alloué à chaque phase.")
        )
    ),
    Domain(
        id = "climat_flash",
        title = "Climat & Energie",
        questions = listOf(
            Question("f_cli_1", "Investissement : Les joueurs étaient concentrés et actifs."),
            Question("f_cli_2", "Posture : Je suis resté positif malgré les échecs techniques.")
        )
    ),
    Domain(
        id = "resultat_flash",
        title = "Analyse Technique",
        questions = listOf(
            Question("f_res_1", "Objectif : Le thème de séance a été assimilé par le groupe.")
        )
    )
)

/**
 * DIAGNOSTIC GLOBAL (Career / Skills)
 */
val globalDiagnosticData = listOf(
    Domain(
        id = "vocation",
        title = "Vocation & Plaisir",
        questions = listOf(
            Question("g_voc_1", "Je prends toujours autant de plaisir à entraîner."),
            Question("g_voc_2", "Je parviens à équilibrer vie pro, vie perso et coaching.")
        )
    ),
    Domain(
        id = "technique",
        title = "Maîtrise Technique",
        questions = listOf(
            Question("tech_1", "Je sais corriger un geste technique sans le dénaturer."),
            Question("tech_2", "Je construis des exercices adaptés au niveau des joueurs."),
            Question("tech_3", "Je varie les formes d’exercices pour un même objectif."),
            Question("tech_4", "Je repère rapidement la cause d’une erreur technique."),
            Question("tech_5", "Je veille à la sécurité dans les situations d’apprentissage.")
        )
    ),
    Domain(
        id = "tactique",
        title = "Compétences Tactiques",
        questions = listOf(
            Question("tact_1", "Je sais expliquer les choix tactiques pendant un match."),
            Question("tact_2", "Je maîtrise les systèmes de jeu adaptés à chaque catégorie."),
            Question("tact_3", "Je modifie mes consignes selon l’adversaire ou la situation."),
            Question("tact_4", "Je fais participer les joueurs à l’analyse du jeu."),
            Question("tact_5", "Je planifie des séances orientées sur la tactique.")
        )
    ),
    Domain(
        id = "social",
        title = "Relationnel & Social",
        questions = listOf(
            Question("g_soc_1", "Je gère sereinement les relations avec les parents/dirigeants."),
            Question("g_soc_2", "Je sais désamorcer les conflits au sein du collectif."),
            Question("ped_4", "J’adapte mes interventions à chaque joueur.")
        )
    ),
    Domain(
        id = "organisation",
        title = "Organisation",
        questions = listOf(
            Question("org_1", "Je planifie mes séances à l’avance."),
            Question("org_2", "J’organise le matériel pour optimiser le temps de jeu."),
            Question("org_4", "Je communique régulièrement avec les dirigeants du club.")
        )
    ),
    Domain(
        id = "progression",
        title = "Auto-Formation",
        questions = listOf(
            Question("dev_1", "Je participe régulièrement à des formations ou stages."),
            Question("dev_2", "Je prends du recul sur mes séances pour progresser."),
            Question("g_pro_2", "Je sais remettre en question mes certitudes techniques.")
        )
    )
)
