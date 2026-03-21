package com.alfredJenny.app.services

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import com.alfredJenny.app.data.model.VoiceSpeakRequestDto
import com.alfredJenny.app.data.remote.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the backend /voice/speak endpoint, decodes the MP3 response,
 * and plays it with MediaPlayer.
 *
 * Exposes [isPlaying] so the UI can animate while audio is playing.
 */
@Singleton
class VoicePlaybackService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var mediaPlayer: MediaPlayer? = null
    private var lastTempFile: File? = null

    suspend fun speak(
        text: String,
        voiceId: String,
        apiKey: String
    ): Result<Unit> = runCatching {
        stopPlayback()

        val response = apiService.speak(
            VoiceSpeakRequestDto(
                text = text,
                voiceId = voiceId,
                apiKey = apiKey.ifBlank { null }
            )
        )

        if (!response.isSuccessful) {
            error("Voice API error ${response.code()}: ${response.errorBody()?.string()}")
        }

        val audioBase64 = response.body()!!.audioBase64
        val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)

        // Write to cache on IO thread
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("tts_", ".mp3", context.cacheDir).also {
                it.writeBytes(audioBytes)
            }
        }
        lastTempFile = tempFile

        // MediaPlayer must be prepared on a thread with a Looper (Main is safe)
        withContext(Dispatchers.Main) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener { cleanUp(tempFile) }
                setOnErrorListener { _, _, _ -> cleanUp(tempFile); false }
                start()
                _isPlaying.value = true
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        lastTempFile?.delete()
        lastTempFile = null
        _isPlaying.value = false
    }

    private fun cleanUp(file: File) {
        mediaPlayer?.release()
        mediaPlayer = null
        file.delete()
        _isPlaying.value = false
    }
}
