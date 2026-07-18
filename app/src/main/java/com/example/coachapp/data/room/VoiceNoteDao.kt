package com.example.coachapp.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VoiceNoteDao {
    @Insert suspend fun insert(note: VoiceNoteEntity)
    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    suspend fun getAll(): List<VoiceNoteEntity>
}
