package com.example.coachapp.data.model

data class CollectifAvecDetail(
    val collectif: Collectif,
    val rattachements: List<Rattachement>,
    val invitationsEnAttente: List<Invitation>,
    val joueurs: List<JoueurCollectif> = emptyList()
) {
    val placesRestantes: Int
        get() = 3 - rattachements.count {
            it.statut == RattachementStatut.ACTIF ||
                    it.statut == RattachementStatut.EN_ATTENTE_CT
        } - invitationsEnAttente.size

    val estComplet: Boolean get() = placesRestantes <= 0

    val coachPrincipal: String?
        get() = rattachements.find { it.poste == Poste.PRINCIPAL }?.let { "${it.coachPrenom ?: ""} ${it.coachNom ?: ""}".trim() }
}
