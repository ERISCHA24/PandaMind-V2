package com.example.animepopular.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtil {

    /**
     * Copies a URI (from gallery/camera) to app's internal files directory.
     * Returns the absolute path string for persistence in Room.
     */
    fun copyUriToInternalStorage(
        context: Context,
        uri: Uri,
        isGif: Boolean = false
    ): String {
        val ext = if (isGif) "gif" else getExtension(context, uri)
        val fileName = "review_${UUID.randomUUID()}.$ext"
        val dir = File(context.filesDir, "reviews").also { it.mkdirs() }
        val dest = File(dir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }

        return dest.absolutePath
    }

    private fun getExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        return extensionFromMime(mimeType)
    }

    fun getExtensionFromUri(context: Context, uri: Uri): String =
        getExtension(context, uri)

    private fun extensionFromMime(mimeType: String): String = when {
        mimeType.contains("gif")  -> "gif"
        mimeType.contains("png")  -> "png"
        mimeType.contains("webp") -> "webp"
        else -> "jpg"
    }

    /**
     * Checks if a stored path is a GIF file.
     */
    fun isGif(path: String): Boolean =
        path.endsWith(".gif", ignoreCase = true)

    /**
     * Returns a Uri that can be used with Coil for loading from internal storage.
     */
    fun pathToUri(path: String): Uri = Uri.fromFile(File(path))

    /**
     * Deletes a stored review image to free up space.
     */
    fun deleteFile(path: String) {
        try { File(path).delete() } catch (e: Exception) { /* ignore */ }
    }
}