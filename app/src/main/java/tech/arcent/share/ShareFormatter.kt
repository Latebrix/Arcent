package tech.arcent.share

/*
 Utility for formatting share text for an Achievement.
 */

import android.content.Context
import tech.arcent.home.Achievement
import java.io.File
import java.io.FileOutputStream

object ShareFormatter {
    private val markerRegex = Regex("(?m)^\\s{0,3}(#{1,6}\\s+|[-*>`]\\s*|\\d+\\.\\s+)")

    fun formatText(a: Achievement): String {
        val sb = StringBuilder()
        sb.append(a.title)
        a.details?.takeIf { it.isNotBlank() }?.let { raw ->
            val cleaned = markerRegex.replace(raw, " ").replace("**", "").replace("*", "").trim()
            if (cleaned.isNotBlank()) {
                sb.append('\n').append('\n').append(cleaned)
            }
        }
        return sb.toString()
    }

    fun prepareImageFile(context: Context, photoUrl: String): File? {
        return try {
            val path = when {
                photoUrl.startsWith("file://") -> photoUrl.removePrefix("file://")
                else -> photoUrl
            }
            val src = File(path)
            if (!src.exists() || !src.isFile) return null
            val cacheDir = File(context.cacheDir, "share_images").apply { if (!exists()) mkdirs() }
            val out = File(cacheDir, "share_${System.currentTimeMillis()}.jpg")
            src.inputStream().use { input -> FileOutputStream(out).use { output -> input.copyTo(output) } }
            out
        } catch (_: Exception) {
            null
        }
    }
}
