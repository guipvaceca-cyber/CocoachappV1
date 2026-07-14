package com.example.coachapp.ui.util

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.EnumMap

@Serializable
data class PlayerQrData(
    val n: String, // Nom
    val p: String, // Prénom
    val l: String, // Licence
    val c: String, // Catégorie
    val a: Int?    // Âge (calculé)
)

object AgeUtils {
    fun calculateAge(birthDateStr: String?): Int? {
        if (birthDateStr == null) return null
        return try {
            val birthDate = LocalDate.parse(birthDateStr.replace("/", "-"), DateTimeFormatter.ISO_LOCAL_DATE)
            Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }

    fun getCategoryFromBirthDate(birthDateStr: String?): String? {
        if (birthDateStr == null) return null
        return try {
            val birthDate = LocalDate.parse(birthDateStr.replace("/", "-"), DateTimeFormatter.ISO_LOCAL_DATE)
            val year = birthDate.year
            val currentYear = LocalDate.now().year
            val ageAtEndOfYear = currentYear - year

            when {
                ageAtEndOfYear >= 20 -> "Seniors"
                ageAtEndOfYear >= 18 -> "M21"
                ageAtEndOfYear >= 15 -> "M18"
                ageAtEndOfYear >= 13 -> "M15"
                ageAtEndOfYear >= 11 -> "M13"
                ageAtEndOfYear >= 9 -> "M11"
                else -> "M9"
            }
        } catch (e: Exception) {
            null
        }
    }

    // Retourne l'ordre pour comparer si un joueur est trop vieux
    fun getCategoryLevel(cat: String): Int {
        return when (cat) {
            "M9" -> 1
            "M11" -> 2
            "M13" -> 3
            "M15" -> 4
            "M18" -> 5
            "M21" -> 6
            "Seniors" -> 7
            else -> 0
        }
    }
}

@Serializable
data class TeamQrPayload(
    val team: String,
    val players: List<PlayerQrData>
)

object QrCodeGenerator {
    fun generateQrBitmap(content: String, size: Int): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1
            
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun QrCodeDisplay(
    payload: TeamQrPayload,
    sizeDp: Int = 250
) {
    val json = remember { Json { encodeDefaults = true } }
    val content = remember(payload) { json.encodeToString(payload) }
    
    val bitmap = remember(content) {
        QrCodeGenerator.generateQrBitmap(content, 512)
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "QR Code de match",
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}
