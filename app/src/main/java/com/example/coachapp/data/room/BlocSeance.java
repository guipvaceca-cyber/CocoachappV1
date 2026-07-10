package com.example.coachapp.data.room;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "bloc_seance",
    indices = @Index("seanceId")
)
public class BlocSeance {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int seanceId;

    // position du bloc dans la séance (0, 1, 2…)
    public int ordre;

    // ex: "Échauffement", "Manips balle", "Jeu réduit", "Collectifs"
    public String label;

    public int dureeMinutes;

    // notes ou description du contenu du bloc
    public String description;
}
