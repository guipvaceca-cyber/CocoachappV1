package com.example.coachapp.data.repository

import android.content.Context
import com.example.coachapp.data.LocalVoiceManager
import com.example.coachapp.data.room.VoiceNoteDao
import com.example.coachapp.data.room.VoiceNoteEntity
import java.io.File

class VoiceNoteRepository(
    private val dao: VoiceNoteDao,
    private val context: Context
) {

    private val voiceManager = LocalVoiceManager(context)

    suspend fun saveVoiceNote(audioFile: File, transcription: String) {
        val entity = VoiceNoteEntity(
            audioPath = audioFile.absolutePath,
            transcription = transcription,
            timestamp = System.currentTimeMillis()
        )

        dao.insert(entity)
    }
}
