package com.example.coachapp.data.room;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "cycle",
    indices = @Index("categorieId")
)
public class Cycle {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int categorieId;

    // ex: "Fondations", "Perfectionnement"
    public String label;

    // ex: "service_recep", "bloc_attaque", "fondamentaux"
    public String theme;

    // format ISO : "2025-09-01"
    public String dateDebut;

    // format ISO : "2025-09-30"
    public String dateFin;

    // notes libres du coach sur les objectifs du cycle
    public String notes;
}
