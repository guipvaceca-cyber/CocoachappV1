package com.example.coachapp.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfLauncher {

    /**
     * Copies a PDF from assets to cache and opens it with an external viewer.
     */
    fun openLocalPdf(context: Context, assetPath: String) {
        try {
            val cacheDir = File(context.cacheDir, "rules")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val fileName = assetPath.substringAfterLast("/")
            val tempFile = File(cacheDir, fileName)

            // Copy asset to file if it doesn't exist or we want to ensure fresh copy
            context.assets.open(assetPath).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PdfLauncher", "Error opening PDF: $assetPath", e)
        }
    }

    /**
     * Opens a remote PDF URL in the default browser or PDF viewer.
     */
    fun openRemotePdf(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PdfLauncher", "Error opening remote URL: $url", e)
        }
    }
}
