package com.example.coachapp.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.example.coachapp.BuildConfig
import java.io.File

class VoiceInputManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY

    // Démarre l'enregistrement
    fun demarrerEnregistrement(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
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
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("VOICE", "Erreur démarrage: ${e.message}")
            false
        }
    }

    // Arrête l'enregistrement et retourne le fichier
    fun arreterEnregistrement(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFile
        } catch (e: Exception) {
            android.util.Log.e("VOICE", "Erreur arrêt: ${e.message}")
            null
        }
    }

    // Transcrit via Gemini
    suspend fun transcrire(fichierAudio: File): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Upload du fichier audio vers Gemini Files API
                val client = OkHttpClient()

                val uploadResponse = try {
                    val uploadRequest = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/upload/v1beta/files?key=$geminiApiKey")
                        .post(
                            MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart(
                                    "file",
                                    fichierAudio.name,
                                    fichierAudio.asRequestBody("audio/mp4".toMediaType())
                                )
                                .build()
                        )
                        .build()
                    android.util.Log.d("VOICE", "Envoi fichier: ${fichierAudio.absolutePath} taille: ${fichierAudio.length()} bytes")
                    client.newCall(uploadRequest).execute()
                } catch (e: Exception) {
                    android.util.Log.e("VOICE", "Erreur réseau upload: ${e.message}", e)
                    return@withContext "[Erreur réseau: ${e.message}]"
                }

                val uploadBody = uploadResponse.body?.string() ?: ""
                android.util.Log.d("VOICE", "Réponse upload (${uploadResponse.code}): $uploadBody")
                val uploadJson = JSONObject(uploadBody)
                val fileUri = uploadJson.getJSONObject("file").getString("uri")

                android.util.Log.d("VOICE", "Fichier uploadé: $fileUri")

                // 2. Demander la transcription à Gemini
                val prompt = """
                    Transcris fidèlement cette note vocale d'un coach de volleyball.
                    Corrige l'orthographe et la ponctuation.
                    Garde le vocabulaire technique volleyball (réception, attaque, bloc, libero, etc.).
                    Retourne uniquement le texte transcrit, sans commentaire.
                """.trimIndent()

                val requestBody = """
                    {
                        "contents": [{
                            "parts": [
                                {"text": "$prompt"},
                                {"file_data": {"mime_type": "audio/mp4", "file_uri": "$fileUri"}}
                            ]
                        }],
                        "generationConfig": {
                            "temperature": 0.1,
                            "maxOutputTokens": 1000
                        }
                    }
                """.trimIndent()

                val geminiRequest = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val geminiResponse = client.newCall(geminiRequest).execute()
                val geminiJson = JSONObject(geminiResponse.body?.string() ?: "")

                val texte = geminiJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                android.util.Log.d("VOICE", "Transcription: $texte")
                texte

            } catch (e: Exception) {
                android.util.Log.e("VOICE", "Erreur transcription: ${e.message}", e)
                "[Erreur de transcription]"
            }
        }
    }

    // Nettoyer les fichiers temporaires
    fun nettoyerCache() {
        context.cacheDir.listFiles()?.filter {
            it.name.startsWith("voice_note_")
        }?.forEach { it.delete() }
    }
}
