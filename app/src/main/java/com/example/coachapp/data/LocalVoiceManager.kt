package com.example.coachapp.data

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class LocalVoiceManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _partialText = MutableStateFlow("")
    val partialText = _partialText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    fun startListeningAndRecording() {
        if (_isRecording.value) return

        try {
            // 1. Préparation de l'enregistrement audio
            val folder = File(context.filesDir, "voice_notes")
            if (!folder.exists()) folder.mkdirs()
            
            val file = File(folder, "note_${System.currentTimeMillis()}.m4a")
            audioFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            // 2. Préparation de la reconnaissance vocale
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("LocalVoiceManager", "Prêt pour la parole")
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.e("LocalVoiceManager", "Erreur SpeechRecognizer: $error")
                        _isRecording.value = false
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        _partialText.value = matches?.firstOrNull() ?: ""
                        _isRecording.value = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        _partialText.value = matches?.firstOrNull() ?: ""
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            _isRecording.value = true
            _partialText.value = ""

        } catch (e: Exception) {
            Log.e("LocalVoiceManager", "Erreur démarrage: ${e.message}")
            _isRecording.value = false
        }
    }

    fun stopListeningAndRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            
            _isRecording.value = false
            return audioFile
        } catch (e: Exception) {
            Log.e("LocalVoiceManager", "Erreur arrêt: ${e.message}")
            _isRecording.value = false
            return null
        }
    }

    fun deleteFile(file: File?) {
        file?.let {
            if (it.exists()) {
                val deleted = it.delete()
                Log.d("LocalVoiceManager", "Fichier supprimé: ${it.name} ($deleted)")
            }
        }
    }
}
