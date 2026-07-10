package com.example.coachapp.data.room;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "seance",
    indices = {
        @Index("categorieId"),
        @Index("cycleId")
    }
)
public class Seance {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int categorieId;

    // nullable — séance hors cycle possible
    public Integer cycleId;

    // ex: "2025-07-14T18:00"
    public String dateHeure;

    public int dureeMinutes;

    // ex: "service_recep", "bloc_attaque"
    // pré-rempli depuis le cycle suggéré, modifiable par le coach
    public String theme;

    // ex: "Réception croisée + relance"
    public String titre;

    // "BROUILLON" | "VALIDEE"
    // seules les VALIDEE apparaissent dans le Hub
    public String statut;

    // notes libres du coach
    public String notes;

    // timestamp de dernière modification, pour la sync future
    public long updatedAt;
}
