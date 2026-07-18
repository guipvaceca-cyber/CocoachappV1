package com.example.coachapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val audioPath: String,
    val transcription: String,
    val timestamp: Long
)
