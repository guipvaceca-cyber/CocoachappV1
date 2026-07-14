package com.example.coachapp.data.model

// data/model/Invitation.kt
data class Invitation(
    val id: String,
    val collectifId: String,
    val invitePar: String,
    val email: String?,
    val telephone: String?,
    val poste: Poste,
    val statut: String,
    val expiresAt: String
)

