package com.example.coachapp.data

data class Resource(
    val title: String,
    val type: String // "Vidéo", "PDF", "Lien", "Formation"
)

val resourcesData = mapOf(
    "Technique" to listOf(
        Resource("Fiches d’exercices FFVB", "PDF"),
        Resource("Vidéos de gestes fondamentaux", "Vidéo")
    ),
    "Tactique" to listOf(
        Resource("Modules DRE2", "Formation"),
        Resource("Vidéos de systèmes de jeu", "Vidéo")
    ),
    "Pédagogie" to listOf(
        Resource("Formation gestion de groupe", "Formation"),
        Resource("Articles sur la motivation", "Lien")
    ),
    "Organisation" to listOf(
        Resource("Formation Responsable de structure sportive", "Formation"),
        Resource("Outils de planification", "Lien")
    ),
    "Développement personnel" to listOf(
        Resource("Stages FFVB", "Formation"),
        Resource("Échanges interclubs", "Lien"),
        Resource("Podcasts d’entraîneurs", "Audio")
    )
)
