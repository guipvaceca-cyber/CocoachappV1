package com.example.coachapp.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.coachapp.data.Team
import kotlin.math.abs

object ColorUtils {

    /**
     * Palette de 12 couleurs contrastées pour la substitution
     */
    private val distinctPalette = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFFE91E63), // Pink
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF009688), // Teal
        Color(0xFF3F51B5), // Indigo
        Color(0xFFFFC107), // Amber
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF5722), // Deep Orange
        Color(0xFFCDDC39), // Lime
        Color(0xFF03A9F4)  // Light Blue
    )

    /**
     * Fusionne mes équipes avec les autres équipes du club, en s'assurant
     * que les autres n'utilisent pas mes couleurs locales.
     */
    fun resolveLocalColorConflicts(
        myTeams: List<Team>,
        otherClubTeams: List<Team>
    ): List<Team> {
        val myColors = myTeams.map { it.color.toArgb() }.toSet()
        
        // Palette disponible (on retire les couleurs trop proches de "mes" couleurs)
        val availablePalette = distinctPalette.filter { paletteColor ->
            myColors.none { isClose(it, paletteColor.toArgb()) }
        }.ifEmpty { distinctPalette }

        val processedOthers = otherClubTeams.map { other ->
            // Pour les autres équipes, on impose TOUJOURS une couleur de la palette disponible
            // de façon déterministe, pour garantir qu'elles soient différentes des miennes.
            val paletteIndex = abs(other.id.hashCode()) % availablePalette.size
            other.copy(color = availablePalette[paletteIndex])
        }

        return (myTeams + processedOthers).distinctBy { it.id }
    }

    /**
     * Vérifie si deux couleurs sont "trop proches" visuellement
     * (Logique simplifiée basée sur la somme des différences RGB)
     */
    private fun isClose(color1: Int, color2: Int): Boolean {
        if (color1 == color2) return true
        
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val diff = abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
        return diff < 60 // Seuil arbitraire pour détecter la proximité
    }
}
