package com.example.coachapp.data

import com.example.coachapp.data.room.Cycle
import com.example.coachapp.data.room.CycleDao
import java.time.LocalDate

class CycleRepository(private val cycleDao: CycleDao) {

    // Récupérer tous les cycles d'une catégorie
    fun getCyclesPourCategorie(categorieId: Int): List<Cycle> {
        return cycleDao.getCyclesPourCategorie(categorieId)
    }

    // Récupérer le cycle actif à une date donnée
    fun getCycleActif(categorieId: Int, date: LocalDate): Cycle? {
        return cycleDao.getCycleActif(categorieId, date.toString())
    }

    // Ajouter un nouveau cycle
    fun ajouterCycle(cycle: Cycle): Long {
        return cycleDao.inserer(cycle)
    }

    // Modifier un cycle existant
    fun modifierCycle(cycle: Cycle) {
        cycleDao.modifier(cycle)
    }

    // Supprimer un cycle
    fun supprimerCycle(cycle: Cycle) {
        cycleDao.supprimer(cycle)
    }
}