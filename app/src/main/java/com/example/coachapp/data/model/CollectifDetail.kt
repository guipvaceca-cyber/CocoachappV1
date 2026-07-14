package com.example.coachapp.data.model

data class JoueurCollectif(
    val id: String,
    val nom: String,
    val prenom: String,
    val categorie: String,
    val dateNaissance: String?,
    val niveauSurclassement: String?,
    val source: String,
    val vivierJoueurId: Long?,
    val poste: String? = null
) {
    val nomComplet get() = "$prenom $nom"
    val initiales get() = "${prenom.firstOrNull() ?: ""}${nom.firstOrNull() ?: ""}"
    val estSurclasse get() = niveauSurclassement != null
    val estManuel get() = source == "manuel"
}

data class JoueurVivier(
    val id: Long,
    val nom: String,
    val prenom: String,
    val categorie: String,
    val dateNaissance: String?,
    val niveauSurclassement: String?,
    val groupeAffichage: Int  // 0=catégorie propre, 1=N-1, 2=N-2, 3=N-3
) {
    val nomComplet get() = "$prenom $nom"
    val initiales get() = "${prenom.firstOrNull() ?: ""}${nom.firstOrNull() ?: ""}"
    val labelSurclassement get() = when(groupeAffichage) {
        0 -> null
        else -> niveauSurclassement
    }
}

data class FormatLimite(
    val format: String,
    val minJoueurs: Int,
    val maxJoueurs: Int
)
