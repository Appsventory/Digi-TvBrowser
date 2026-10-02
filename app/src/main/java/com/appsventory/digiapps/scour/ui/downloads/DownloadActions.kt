package com.appsventory.digiapps.scour.ui.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object DownloadActions {

    fun openFile(context: Context, item: DownloadItem): Boolean {
        val dm = context.getSystemService(DownloadManager::class.java) ?: return false
        var uri: Uri? = null
        var mime: String? = null
        if (item.id > 0L) {
            runCatching {
                uri = dm.getUriForDownloadedFile(item.id)
                mime = dm.getMimeTypeForDownloadedFile(item.id)
            }
        }
        if (uri == null) {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                item.name
            )
            if (!file.exists()) return false
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            mime = guessMime(item.name)
        }
        val type = mime?.takeIf { it.isNotBlank() } ?: guessMime(item.name)
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, item.name))
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Opens system Downloads UI / folder. */
    fun openInFolder(context: Context) {
        val intents = listOf(
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS),
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ).toString()
                    ),
                    "resource/folder"
                )
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.externalstorage.documents/document/primary:Download")
            }
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }
    }

    fun deleteFromDisk(context: Context, item: DownloadItem): Boolean {
        val dm = context.getSystemService(DownloadManager::class.java)
        if (item.id > 0L) {
            runCatching { dm?.remove(item.id) }
        }
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            item.name
        )
        return runCatching { if (file.exists()) file.delete() else true }.getOrDefault(false)
    }

    fun isApk(name: String): Boolean =
        name.endsWith(".apk", ignoreCase = true)

    private fun guessMime(name: String): String = when {
        name.endsWith(".apk", true) -> "application/vnd.android.package-archive"
        name.endsWith(".pdf", true) -> "application/pdf"
        name.endsWith(".zip", true) -> "application/zip"
        name.endsWith(".mp4", true) -> "video/mp4"
        name.endsWith(".mp3", true) -> "audio/mpeg"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".txt", true) -> "text/plain"
        name.endsWith(".html", true) || name.endsWith(".htm", true) -> "text/html"
        else -> "*/*"
    }
}
