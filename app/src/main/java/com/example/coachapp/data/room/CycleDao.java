package com.example.coachapp.data.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.coachapp.data.room.Cycle;

import java.util.List;

@Dao
public interface CycleDao {

    @Insert
    long inserer(Cycle cycle);

    @Update
    void modifier(Cycle cycle);

    @Delete
    void supprimer(Cycle cycle);

    // Tous les cycles d'une catégorie triés par date (vue programmateur de saison)
    @Query("SELECT * FROM cycle WHERE categorieId = :categorieId ORDER BY dateDebut ASC")
    List<Cycle> getCyclesPourCategorie(int categorieId);

    // Cycle actif à une date donnée (pour suggérer le thème dans le programmateur de séance)
    // dateRef format : "2025-07-14"
    @Query("SELECT * FROM cycle WHERE categorieId = :categorieId AND dateDebut <= :dateRef AND dateFin >= :dateRef LIMIT 1")
    Cycle getCycleActif(int categorieId, String dateRef);

    @Query("SELECT * FROM cycle WHERE id = :id")
    Cycle getById(int id);
}
