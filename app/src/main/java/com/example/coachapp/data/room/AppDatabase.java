package com.example.coachapp.data.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.coachapp.data.room.BlocSeanceDao;
import com.example.coachapp.data.room.CycleDao;
import com.example.coachapp.data.room.SeanceDao;
import com.example.coachapp.data.room.BlocSeance;
import com.example.coachapp.data.room.Categorie;
import com.example.coachapp.data.room.Cycle;
import com.example.coachapp.data.room.Seance;

@Database(
    entities = {
        Categorie.class,
        Cycle.class,
        Seance.class,
        BlocSeance.class
    },
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SeanceDao seanceDao();
    public abstract BlocSeanceDao blocSeanceDao();
    public abstract CycleDao cycleDao();

    // Singleton — une seule instance dans toute l'app
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "volley-coach-db"
                    )
                    // À retirer avant la prod — permet les requêtes sur le thread principal pendant le dev
                    .allowMainThreadQueries()
                    // Si tu changes une Entity, incrémente version et ajoute une Migration
                    // plutôt que de tout effacer en prod
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
