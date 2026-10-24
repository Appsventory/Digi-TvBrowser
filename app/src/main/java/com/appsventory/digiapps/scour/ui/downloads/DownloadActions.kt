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

    /**
     * Deletes only the file tied to this list entry.
     * Prefer DownloadManager ID so system-renamed files (e.g. file-1.apk) are targeted;
     * never scans the whole Downloads folder.
     */
    fun deleteFromDisk(context: Context, item: DownloadItem): Boolean {
        val dm = context.getSystemService(DownloadManager::class.java)
        if (item.id > 0L && dm != null) {
            // remove(id) deletes that download record and its local file only.
            val removed = runCatching { dm.remove(item.id) }.getOrDefault(0)
            if (removed > 0) return true
        }
        // Fallback when we never got a DM id: exact name in public Downloads only.
        val file = downloadsDirFile(item.name)
        return runCatching { if (file.exists()) file.delete() else true }.getOrDefault(false)
    }

    fun downloadsDirFile(name: String): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)

    /** True if a file with this exact name already exists in public Downloads. */
    fun localFileExists(name: String): Boolean = downloadsDirFile(name).exists()

    /**
     * Picks a free name: `file.ext` → `file-1.ext`, `file-2.ext`, …
     * Skips names that exist on disk or are in [reservedNames] (current list).
     */
    fun uniqueFileName(desired: String, reservedNames: Set<String> = emptySet()): String {
        if (!localFileExists(desired) && desired !in reservedNames) return desired
        val dot = desired.lastIndexOf('.')
        val base = if (dot > 0) desired.substring(0, dot) else desired
        val ext = if (dot > 0) desired.substring(dot) else ""
        var n = 1
        while (n < 10_000) {
            val candidate = "$base-$n$ext"
            if (!localFileExists(candidate) && candidate !in reservedNames) return candidate
            n++
        }
        return "$base-${System.currentTimeMillis()}$ext"
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
