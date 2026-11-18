package com.mushroom.notificationlogger.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class CapturedBitmap(
    val imageType: String,
    val bitmap: Bitmap
)

data class SavedImageMetadata(
    val fileName: String,
    val imageType: String,
    val width: Int,
    val height: Int
)

class ImageStorageManager(context: Context) {

    private val imagesDir: File = File(context.applicationContext.filesDir, IMAGES_DIR_NAME).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun saveBitmap(
        notificationId: Long,
        imageType: String,
        index: Int,
        sourceBitmap: Bitmap
    ): SavedImageMetadata? {
        if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) return null

        return runCatching {
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val safeBitmap = ensureSoftwareAndReasonableSize(sourceBitmap)
            val suffix = (System.nanoTime() and 0xFFFFF).toString(16)
            val safeType = imageType.lowercase().replace(Regex("[^a-z0-9_]"), "_")
            val fileName = "notif_${notificationId}_${safeType}_${index}_$suffix.png"
            val outFile = File(imagesDir, fileName)

            FileOutputStream(outFile).use { stream ->
                val ok = safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                if (!ok) {
                    outFile.delete()
                    return null
                }
            }

            val width = safeBitmap.width
            val height = safeBitmap.height
            if (safeBitmap !== sourceBitmap) {
                safeBitmap.recycle()
            }

            SavedImageMetadata(
                fileName = fileName,
                imageType = imageType,
                width = width,
                height = height
            )
        }.getOrNull()
    }

    fun resolveImageFile(fileName: String): File {
        val sanitized = File(fileName).name
        return File(imagesDir, sanitized)
    }

    fun deleteFiles(fileNames: List<String>) {
        for (name in fileNames) {
            runCatching {
                val target = resolveImageFile(name)
                if (target.exists()) {
                    target.delete()
                }
            }
        }
    }

    fun deleteAllFiles() {
        runCatching {
            imagesDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    private fun ensureSoftwareAndReasonableSize(bitmap: Bitmap): Bitmap {
        var current = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }

        val maxDim = max(current.width, current.height)
        if (maxDim > MAX_IMAGE_DIMENSION) {
            val scale = MAX_IMAGE_DIMENSION.toFloat() / maxDim.toFloat()
            val targetW = (current.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (current.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(current, targetW, targetH, true)
            if (current !== bitmap && current !== scaled) {
                current.recycle()
            }
            current = scaled
        }
        return current
    }

    companion object {
        const val IMAGES_DIR_NAME = "notification_images"
        private const val MAX_IMAGE_DIMENSION = 2048
    }
}
