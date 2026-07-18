package com.example.coachapp.data

import kotlinx.serialization.Serializable

@Serializable
enum class FormationScope(val label: String) {
    DEPARTEMENTAL("Départemental"),
    REGIONAL("Régional"),
    NATIONAL("National"),
    INTERNATIONAL("International")
}

@Serializable
data class DiplomaModule(
    val id: String,
    val label: String,
    val hours: Int,
    val scope: FormationScope
)

@Serializable
data class Diploma(
    val id: String,
    val name: String,
    val level: String,
    val modules: List<DiplomaModule>,
    val totalHours: Int,
    val globalScope: FormationScope
)

val ffvbDiplomas = listOf(
    Diploma(
        id = "afj",
        name = "AFJ",
        level = "Animateur Formation Jeunes",
        totalHours = 12,
        globalScope = FormationScope.DEPARTEMENTAL,
        modules = listOf(
            DiplomaModule("afj_base", "Bases de l'animation", 6, FormationScope.DEPARTEMENTAL),
            DiplomaModule("afj_jeunes", "Spécificités Jeunes", 6, FormationScope.DEPARTEMENTAL)
        )
    ),
    Diploma(
        id = "dre1",
        name = "DRE 1",
        level = "Diplôme Responsable d'Équipe 1",
        totalHours = 50,
        globalScope = FormationScope.REGIONAL,
        modules = listOf(
            DiplomaModule("dre1_animateur", "Module Animateur", 26, FormationScope.REGIONAL),
            DiplomaModule("dre1_initiateur", "Module Initiateur", 24, FormationScope.REGIONAL)
        )
    ),
    Diploma(
        id = "er",
        name = "ER",
        level = "Entraîneur Régional",
        totalHours = 60,
        globalScope = FormationScope.REGIONAL,
        modules = listOf(
            DiplomaModule("er_general", "Tronc commun régional", 30, FormationScope.REGIONAL),
            DiplomaModule("er_perf", "Perfectionnement technique", 30, FormationScope.REGIONAL)
        )
    ),
    Diploma(
        id = "dre2",
        name = "DRE 2",
        level = "Diplôme Responsable d'Équipe 2",
        totalHours = 80,
        globalScope = FormationScope.NATIONAL,
        modules = listOf(
            DiplomaModule("dre2_educateur", "Module Éducateur", 40, FormationScope.NATIONAL),
            DiplomaModule("dre2_entraineur", "Module Entraîneur", 40, FormationScope.NATIONAL)
        )
    ),
    Diploma(
        id = "ef",
        name = "EF",
        level = "Entraîneur Fédéral",
        totalHours = 120,
        globalScope = FormationScope.NATIONAL,
        modules = listOf(
            DiplomaModule("ef_perf", "Performance et Haut Niveau", 60, FormationScope.NATIONAL),
            DiplomaModule("ef_stat", "Analyse Statistique", 60, FormationScope.NATIONAL)
        )
    ),
    Diploma(
        id = "en",
        name = "EN",
        level = "Entraîneur National",
        totalHours = 200,
        globalScope = FormationScope.NATIONAL,
        modules = listOf(
            DiplomaModule("en_national", "Expertise Nationale", 200, FormationScope.NATIONAL)
        )
    ),
    Diploma(
        id = "master_coach",
        name = "Master Coach",
        level = "Haut Niveau International",
        totalHours = 300,
        globalScope = FormationScope.INTERNATIONAL,
        modules = listOf(
            DiplomaModule("mc_expert", "Expertise Internationale", 300, FormationScope.INTERNATIONAL)
        )
    )
)
