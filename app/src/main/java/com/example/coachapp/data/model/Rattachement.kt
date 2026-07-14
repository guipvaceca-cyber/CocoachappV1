package com.example.coachapp.data.model

// data/model/Rattachement.kt
data class Rattachement(
    val id: String,
    val coachId: String,
    val collectifId: String,
    val poste: Poste,
    val statut: RattachementStatut,
    val saison: String,
    val source: String,
    // Jointure profiles
    val coachPrenom: String?,
    val coachNom: String?
)

enum class Poste {
    PRINCIPAL, ADJOINT_1, ADJOINT_2;

    fun label() = when (this) {
        PRINCIPAL -> "Principal"
        ADJOINT_1 -> "Adjoint 1"
        ADJOINT_2 -> "Adjoint 2"
    }

    companion object {
        fun from(value: String) = when (value) {
            "principal" -> PRINCIPAL
            "adjoint_1" -> ADJOINT_1
            "adjoint_2" -> ADJOINT_2
            else -> PRINCIPAL
        }
    }
}

enum class RattachementStatut {
    EN_ATTENTE_CT, ACTIF, REFUSE;

    companion object {
        fun from(value: String) = when (value) {
            "en_attente_ct" -> EN_ATTENTE_CT
            "actif" -> ACTIF
            "refuse" -> REFUSE
            else -> EN_ATTENTE_CT
        }
    }
}

