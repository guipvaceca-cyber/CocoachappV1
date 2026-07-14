package com.example.coachapp.data

import android.content.Context
import com.example.coachapp.data.CompetitionEvent
import com.example.coachapp.data.CompetitionType
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

object CalendrierParser {

    /**
     * Charge les événements d'une ou plusieurs catégories depuis le XML embarqué.
     * @param context Context Android pour accéder aux assets
     * @param categories Liste des catégories à charger (ex: ["M18", "Seniors"])
     * @param teamId L'id de l'équipe à associer aux événements créés
     */
    fun chargerEvenements(
        context: Context,
        categories: List<String>,
        teamId: String
    ): List<CompetitionEvent> {

        val events = mutableListOf<CompetitionEvent>()

        try {
            val inputStream = context.assets.open("calendrier_drome_ardeche_2026_2027.xml")
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var categorieEnCours: String? = null
            var enEvenement = false

            // Champs de l'événement en cours
            var date: String? = null
            var heure: String? = null
            var type: String? = null
            var description: String? = null
            var lieu: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = runCatching { parser.name }.getOrNull()

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "categorie" -> {
                                categorieEnCours = parser.getAttributeValue(null, "nom")
                            }
                            "evenement" -> {
                                // On initialise uniquement si la catégorie nous intéresse
                                if (categorieEnCours != null && categories.contains(categorieEnCours)) {
                                    enEvenement = true
                                    date = null; heure = null; type = null
                                    description = null; lieu = null
                                }
                            }
                            "date"        -> if (enEvenement) date        = parser.nextText()
                            "heure"       -> if (enEvenement) heure       = parser.nextText()
                            "type"        -> if (enEvenement) type        = parser.nextText()
                            "description" -> if (enEvenement) description = parser.nextText()
                            "lieu"        -> if (enEvenement) lieu        = parser.nextText()
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when (tagName) {
                            "categorie" -> categorieEnCours = null
                            "evenement" -> {
                                if (enEvenement && date != null) {
                                    val competitionType = when (type) {
                                        "CUP"        -> CompetitionType.CUP
                                        "TOURNAMENT" -> CompetitionType.TOURNAMENT
                                        "FRIENDLY"   -> CompetitionType.FRIENDLY
                                        else         -> CompetitionType.CHAMPIONSHIP
                                    }
                                    val startTime = runCatching {
                                        LocalTime.parse(heure ?: "09:00")
                                    }.getOrDefault(LocalTime.of(9, 0))

                                    events.add(
                                        CompetitionEvent(
                                            id = UUID.randomUUID().toString(),
                                            teamId = teamId,
                                            date = LocalDate.parse(date),
                                            startTime = startTime,
                                            type = competitionType,
                                            opponent = "À définir",
                                            location = lieu ?: "À définir",
                                            attendance = emptyMap()
                                        )
                                    )
                                }
                                enEvenement = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            inputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }

    /**
     * Correspondance entre le nom d'équipe tel que saisi dans l'onboarding
     * et le nom de catégorie dans le XML.
     * Ex: "M18 Elite" → "M18"
     */
    fun normaliserCategorie(nomEquipe: String): String {
        return when {
            nomEquipe.contains("M9",  ignoreCase = true) -> "M9"
            nomEquipe.contains("M11", ignoreCase = true) -> "M11"
            nomEquipe.contains("M13", ignoreCase = true) -> "M13"
            nomEquipe.contains("M15", ignoreCase = true) -> "M15"
            nomEquipe.contains("M18", ignoreCase = true) -> "M18"
            nomEquipe.contains("M21", ignoreCase = true) -> "M21"
            nomEquipe.contains("Senior", ignoreCase = true) -> "Seniors"
            nomEquipe.contains("Sénior", ignoreCase = true) -> "Seniors"
            else -> nomEquipe
        }
    }
}
