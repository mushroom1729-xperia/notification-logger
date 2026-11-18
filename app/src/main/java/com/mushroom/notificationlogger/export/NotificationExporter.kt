package com.mushroom.notificationlogger.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mushroom.notificationlogger.R
import com.mushroom.notificationlogger.data.local.NotificationWithImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class ExportEnvelope(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val exportedAtIso: String,
    val count: Int,
    val notifications: List<ExportNotificationItem>
)

@Serializable
data class ExportNotificationItem(
    val id: Long,
    val sbnKey: String,
    val packageName: String,
    val appName: String,
    val postTime: Long,
    val updatedAt: Long,
    val title: String,
    val text: String,
    val category: String?,
    val images: List<ExportImageRef>
)

@Serializable
data class ExportImageRef(
    val fileName: String,
    val type: String,
    val width: Int,
    val height: Int
)

object NotificationExporter {

    private val jsonFormatter = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val fileTimestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun serializeNotificationsToJson(
        records: List<NotificationWithImages>,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val items = records.map { entry ->
            val n = entry.notification
            ExportNotificationItem(
                id = n.id,
                sbnKey = n.sbnKey,
                packageName = n.packageName,
                appName = n.appName,
                postTime = n.postTime,
                updatedAt = n.updatedAt,
                title = n.title,
                text = n.text,
                category = n.category,
                images = entry.images.map { img ->
                    // 将来の他デバイス転送に備え、内部パスを含めず純粋なファイル名のみで参照する
                    ExportImageRef(
                        fileName = File(img.fileName).name,
                        type = img.imageType,
                        width = img.width,
                        height = img.height
                    )
                }
            )
        }

        val envelope = ExportEnvelope(
            schemaVersion = 1,
            exportedAt = exportedAtMillis,
            exportedAtIso = Instant.ofEpochMilli(exportedAtMillis).toString(),
            count = items.size,
            notifications = items
        )

        return jsonFormatter.encodeToString(envelope)
    }

    suspend fun writeExportFileAndBuildShareIntent(
        context: Context,
        records: List<NotificationWithImages>
    ): Intent = withContext(Dispatchers.IO) {
        val nowMillis = System.currentTimeMillis()
        val jsonContent = serializeNotificationsToJson(records, nowMillis)

        val exportsDir = File(context.cacheDir, "exports").apply {
            if (!exists()) {
                mkdirs()
            }
            // 古い一時エクスポートファイルを掃除
            listFiles()?.forEach { file ->
                if (file.isFile) {
                    runCatching { file.delete() }
                }
            }
        }

        val timestamp = Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .format(fileTimestampFormatter)
        val outFile = File(exportsDir, "notifications_$timestamp.json")
        outFile.writeText(jsonContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("notifications_$timestamp.json", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
