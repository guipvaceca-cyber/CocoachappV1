package com.example.coachapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

@Serializable
enum class PostCategory(val label: String, val icon: String) {
    @SerialName("TACTIC") TACTIC("Tactique", "📋"),
    @SerialName("MENTAL") MENTAL("Mental / Relationnel", "🧠"),
    @SerialName("MANAGEMENT") MANAGEMENT("Gestion de groupe", "🤝"),
    @SerialName("EQUIPMENT") EQUIPMENT("Matériel / Logistique", "🏐"),
    @SerialName("SUCCESS") SUCCESS("Réussite (Vibes)", "✨"),
    @SerialName("SOS") SOS("SOS / Urgent", "🚨")
}

@Serializable
data class AnonymousPost(
    val id: String = UUID.randomUUID().toString(),
    val persona: String? = "Coach Anonyme", 
    @SerialName("club_initial") val clubInitial: String? = "CD", 
    val timestamp: Long = System.currentTimeMillis(),
    val category: PostCategory = PostCategory.TACTIC,
    val title: String? = "Sans titre",
    val content: String? = "",
    @SerialName("is_anonymized") val isAnonymizedByIA: Boolean = true,
    @SerialName("support_count") val supportCount: Int = 0,
    @SerialName("is_from_expert") val isFromExpert: Boolean = false,
    @SerialName("author_role") val authorRole: String? = "user",
    @kotlinx.serialization.Transient val comments: List<AnonymousComment> = emptyList()
)

@Serializable
data class AnonymousComment(
    val id: String = UUID.randomUUID().toString(),
    val persona: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isExpert: Boolean = false
)

@Serializable
data class AdminAlert(
    val id: Long? = null,
    @SerialName("post_id") val postId: Long? = null,
    @SerialName("author_id") val authorId: String,
    @SerialName("original_title") val originalTitle: String,
    @SerialName("original_content") val originalContent: String,
    @SerialName("niveau_alerte") val alertLevel: String,
    val raison: String,
    @SerialName("club_initial") val clubInitial: String? = null,
    val category: String? = null,
    val statut: String = "non_traite",
    @SerialName("admin_notes") val adminNotes: String? = null,
    @SerialName("traite_par") val processedBy: String? = null,
    @SerialName("traite_le") val processedAt: Long? = null,
    @SerialName("created_at") val createdAt: Long
)

val mockPosts = listOf<AnonymousPost>(
    AnonymousPost(
        persona = "Le Pédagogue",
        clubInitial = "CD26",
        category = PostCategory.SOS,
        title = "Parent difficile en M13",
        content = "Un parent critique mes choix de rotations pendant les matchs. Comment recadrer sans créer de conflit ?",
        supportCount = 12
    ),
    AnonymousPost(
        persona = "Le Tacticien",
        clubInitial = "CD07",
        category = PostCategory.TACTIC,
        title = "Passe arrière en M15",
        content = "Mes passeurs ont du mal avec la stabilité du bassin sur les passes arrières vers la zone 2. Des exercices miracles ?",
        supportCount = 5
    ),
    AnonymousPost(
        persona = "Expert Comité",
        clubInitial = "CTD",
        category = PostCategory.MANAGEMENT,
        title = "Rappel : Sécurité des poteaux",
        content = "N'oubliez pas de vérifier les protections de poteaux avant chaque début de plateau M11. C'est obligatoire.",
        supportCount = 20,
        isFromExpert = true
    ),
    AnonymousPost(
        persona = "Le Leader",
        clubInitial = "CD26",
        category = PostCategory.SUCCESS,
        title = "Première victoire !",
        content = "Mes M13 ont gagné leur premier set après 3 mois de travail acharné sur le service. Quel bonheur de voir leurs sourires !",
        supportCount = 45
    )
)

val COACH_ALIASES = listOf(
    "Coach_Zen", "Coach_en_Feu", "Coach_Perplexe", "Coach_Solitaire", 
    "Coach_Determine", "Coach_Epuise", "Coach_Ambitieux", "Coach_Incompris",
    "Coach_Optimiste", "Coach_Bosseur", "Coach_Creatif", "Coach_Rigoureux",
    "Coach_Passionne", "Coach_Observateur", "Coach_Protecteur", "Coach_Visionnaire",
    "Coach_Resilient", "Coach_Innovant", "Coach_Patient", "Coach_Exigeant"
)
