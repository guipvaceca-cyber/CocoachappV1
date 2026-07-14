package com.example.coachapp.data.model

data class Collectif(
    val id: String,
    val clubId: String,
    val nom: String,
    val categorie: String,
    val sexe: String,
    val format: String,
    val competition: String,
    val saison: String,
    val statut: CollectifStatut,
    val compoStatut: String? = null,
    val signalementArchive: Boolean,
    val createdBy: String?
)

enum class CollectifStatut {
    EN_ATTENTE_CT, ACTIF, ARCHIVE, REFUSE;

    companion object {
        fun from(value: String) = when (value) {
            "en_attente_ct" -> EN_ATTENTE_CT
            "actif" -> ACTIF
            "archive" -> ARCHIVE
            "refuse" -> REFUSE
            else -> EN_ATTENTE_CT
        }
    }
}
