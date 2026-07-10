package com.example.coachapp.data.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.coachapp.data.room.Seance;

import java.util.List;

@Dao
public interface SeanceDao {

    @Insert
    long inserer(Seance seance);

    @Update
    void modifier(Seance seance);

    @Delete
    void supprimer(Seance seance);

    // Hub : uniquement les séances validées d'une catégorie, triées par date
    @Query("SELECT * FROM seance WHERE categorieId = :categorieId AND statut = 'VALIDEE' ORDER BY dateHeure ASC")
    List<Seance> getValideesPourCategorie(int categorieId);

    // Programmateur de séance : toutes les séances d'une catégorie (brouillons inclus)
    @Query("SELECT * FROM seance WHERE categorieId = :categorieId ORDER BY dateHeure ASC")
    List<Seance> getToutesPourCategorie(int categorieId);

    // Récupérer une séance par id (pour l'édition)
    @Query("SELECT * FROM seance WHERE id = :id")
    Seance getById(int id);

    // Valider une séance (passer de BROUILLON à VALIDEE)
    @Query("UPDATE seance SET statut = 'VALIDEE', updatedAt = :timestamp WHERE id = :id")
    void valider(int id, long timestamp);

    // Repasser en brouillon si le coach veut modifier après validation
    @Query("UPDATE seance SET statut = 'BROUILLON', updatedAt = :timestamp WHERE id = :id")
    void mettreEnBrouillon(int id, long timestamp);

    // Séances d'un cycle donné (vue programmateur de saison)
    @Query("SELECT * FROM seance WHERE cycleId = :cycleId ORDER BY dateHeure ASC")
    List<Seance> getPourCycle(int cycleId);

    // Pour la sync future : séances modifiées depuis un timestamp donné
    @Query("SELECT * FROM seance WHERE updatedAt > :depuis")
    List<Seance> getModifieeDepuis(long depuis);
}
