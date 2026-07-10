package com.example.coachapp.data.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categorie")
public class Categorie {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // ex: "M13", "M18", "Seniors"
    public String label;

    // ex: "lundi,mercredi"
    public String joursEntrainement;

    // ex: "18:00"
    public String heureDebut;

    // ex: 90
    public int dureeMinutes;
}
