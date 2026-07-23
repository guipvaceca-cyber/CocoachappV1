package com.example.coachapp.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

data class JoueurVivier(
    val numLicence: String,
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val categorie: String,
    val sexe: String,
    val taille: Int,
    val nationalite: String,
    val clubCode: String,
    val clubNom: String,
    val estSurclasse: Boolean
)

object CdeVivierParser {

    fun parserCSV(
        context: Context,
        uri: Uri,
        categorieAttendue: String? = null
    ): Result<List<JoueurVivier>> {
        return runCatching {
            val joueurs = mutableListOf<JoueurVivier>()

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Impossible d'ouvrir le fichier"))

            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            val headerLine = reader.readLine() ?: return Result.failure(Exception("Fichier vide"))

            val separateur = if (headerLine.contains(";")) ";" else ","

            val headers = headerLine.split(separateur).map { it.trim() }

            val idxLicence     = headers.indexOf("lnumlic")
            val idxNom         = headers.indexOf("Nom")
            val idxPrenom      = headers.indexOf("Prenom")
            val idxNaissance   = headers.indexOf("Naissance")
            val idxCategorie   = headers.indexOf("Categorie")
            val idxNationalite = headers.indexOf("Nationalite")
            val idxSexe        = headers.indexOf("Sexe")
            val idxTaille      = headers.indexOf("Taille")
            val idxSurclasse   = headers.indexOf("Surcl.")
            val idxClub        = headers.indexOf("vb_club")

            if (idxLicence == -1 || idxNom == -1 || idxPrenom == -1) {
                return Result.failure(Exception("Format CSV non reconnu — colonnes manquantes"))
            }

            var ligne = reader.readLine()
            while (ligne != null) {
                if (ligne.isBlank()) { ligne = reader.readLine(); continue }

                val cols = ligne.split(separateur)

                if (cols.size < headers.size - 5) { ligne = reader.readLine(); continue }

                fun col(idx: Int) = if (idx >= 0 && idx < cols.size) cols[idx].trim() else ""

                val categorie = col(idxCategorie)
                    .replace("é", "e")
                    .uppercase()

                if (categorieAttendue != null && categorie != categorieAttendue) {
                    ligne = reader.readLine()
                    continue
                }

                val clubRaw = col(idxClub)
                val clubCode = clubRaw.take(7).trim()
                val clubNom = if (clubRaw.length > 8) clubRaw.drop(8).trim() else clubRaw

                fun nettoyerTexte(s: String): String = s
                    .replace("Ã©", "é").replace("Ã¨", "è").replace("Ã ", "à")
                    .replace("Ã´", "ô").replace("Ã®", "î").replace("Ã¢", "â")
                    .replace("Ã»", "û").replace("Ã§", "ç").replace("Ã‰", "É")
                    .replace("Ã€", "À").replace("Ã‡", "Ç").replace("â€™", "'")
                    .replace("Ã¯", "ï").replace("Ã«", "ë")
                    .replace("Ã¼", "ü").replace("Ãö", "ö")

                // Normalisation du sexe : "Masc" → "M", "Fém." → "F"
                val sexe = when {
                    col(idxSexe).trim().startsWith("M", ignoreCase = true) -> "M"
                    col(idxSexe).trim().startsWith("F", ignoreCase = true) -> "F"
                    else -> col(idxSexe).trim()
                }

                val joueur = JoueurVivier(
                    numLicence    = col(idxLicence),
                    nom           = nettoyerTexte(col(idxNom)),
                    prenom        = nettoyerTexte(col(idxPrenom)),
                    dateNaissance = col(idxNaissance),
                    categorie     = categorie,
                    sexe          = sexe,
                    taille        = col(idxTaille).toIntOrNull() ?: 0,
                    nationalite   = col(idxNationalite),
                    clubCode      = clubCode,
                    clubNom       = nettoyerTexte(clubNom),
                    estSurclasse  = col(idxSurclasse).trim().uppercase() == "S"
                )

                if (joueur.numLicence.isNotBlank() && joueur.nom.isNotBlank()) {
                    joueurs.add(joueur)
                }

                ligne = reader.readLine()
            }

            reader.close()
            joueurs.sortedWith(compareBy({ it.nom }, { it.prenom }))
        }
    }

    suspend fun syncVersSupabase(
        joueurs: List<JoueurVivier>,
        saison: String = "2026-2027"
    ): Result<Int> {
        return runCatching {
            var compteur = 0

            joueurs.chunked(50).forEach { batch ->
                val rows = batch.map { j ->
                    mapOf(
                        "num_licence"    to j.numLicence,
                        "nom"            to j.nom,
                        "prenom"         to j.prenom,
                        "date_naissance" to j.dateNaissance.ifBlank { null },
                        "categorie"      to j.categorie,
                        "sexe"           to j.sexe,
                        "taille"         to j.taille.takeIf { it > 0 },
                        "nationalite"    to j.nationalite,
                        "club_code"      to j.clubCode,
                        "club_nom"       to j.clubNom,
                        "est_surclasse"  to j.estSurclasse,
                        "saison"         to saison
                    )
                }

                SupabaseManager.db
                    .from("cde_vivier")
                    .upsert(rows) {
                        onConflict = "num_licence,saison"
                    }

                compteur += batch.size
            }

            compteur
        }
    }

    fun stats(joueurs: List<JoueurVivier>): Map<String, Int> {
        return joueurs
            .groupBy { it.categorie }
            .mapValues { it.value.size }
            .toSortedMap()
    }
}