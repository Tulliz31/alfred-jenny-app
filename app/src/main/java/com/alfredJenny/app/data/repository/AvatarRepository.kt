package com.alfredJenny.app.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun jennyDir(): File = File(context.filesDir, "avatars/jenny").also { it.mkdirs() }
    private fun alfredDir(): File = File(context.filesDir, "avatars/alfred").also { it.mkdirs() }

    // ── Jenny ─────────────────────────────────────────────────────────────────

    fun jennyFileExists(filename: String): Boolean =
        File(jennyDir(), filename).exists()

    fun assetExists(filename: String): Boolean = runCatching {
        context.assets.open("jenny/$filename").close()
        true
    }.getOrDefault(false)

    suspend fun saveJennyImage(filename: String, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(jennyDir(), filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }.getOrDefault(false)
        }

    fun removeJennyImage(filename: String): Boolean =
        File(jennyDir(), filename).delete()

    /** Returns a down-sampled thumbnail: checks filesDir first, then assets. */
    suspend fun loadJennyThumbnail(filename: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            val f = File(jennyDir(), filename)
            if (f.exists()) {
                return@withContext runCatching {
                    f.inputStream().use { BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap() }
                }.getOrNull()
            }
            runCatching {
                context.assets.open("jenny/$filename").use {
                    BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }

    // ── Alfred ────────────────────────────────────────────────────────────────

    fun alfredFileExists(filename: String): Boolean =
        File(alfredDir(), filename).exists()

    suspend fun saveAlfredImage(filename: String, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(alfredDir(), filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }.getOrDefault(false)
        }

    fun removeAlfredImage(filename: String): Boolean =
        File(alfredDir(), filename).delete()

    suspend fun loadAlfredThumbnail(filename: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val f = File(alfredDir(), filename)
            if (!f.exists()) return@withContext null
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                f.inputStream().use { BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap() }
            }.getOrNull()
        }

    fun listJennyAssets(): List<String> = runCatching {
        context.assets.list("jenny")?.toList() ?: emptyList()
    }.getOrDefault(emptyList())
}
