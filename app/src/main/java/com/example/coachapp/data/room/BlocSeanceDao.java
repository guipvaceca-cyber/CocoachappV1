package com.example.coachapp.data.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.coachapp.data.room.BlocSeance;

import java.util.List;

@Dao
public interface BlocSeanceDao {

    @Insert
    long inserer(BlocSeance bloc);

    @Update
    void modifier(BlocSeance bloc);

    @Delete
    void supprimer(BlocSeance bloc);

    // Tous les blocs d'une séance, dans l'ordre (pour l'App Terrain / timer)
    @Query("SELECT * FROM bloc_seance WHERE seanceId = :seanceId ORDER BY ordre ASC")
    List<BlocSeance> getBlocsDeSeance(int seanceId);

    // Supprimer tous les blocs d'une séance (utile pour reconstruire après édition)
    @Query("DELETE FROM bloc_seance WHERE seanceId = :seanceId")
    void supprimerBlocsDeSeance(int seanceId);

    // Durée totale calculée d'une séance (somme des blocs)
    @Query("SELECT SUM(dureeMinutes) FROM bloc_seance WHERE seanceId = :seanceId")
    int getDureeTotale(int seanceId);
}
