package com.alfredJenny.app.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.alfredJenny.app.data.model.VoiceMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class SpeechInputState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val casaPhase: CasaPhase = CasaPhase.IDLE
)

enum class CasaPhase { IDLE, LISTENING_FOR_WAKE, LISTENING_FOR_COMMAND }

/**
 * Wraps Android's [SpeechRecognizer] for two modes:
 *
 * **OUTDOOR** — push-to-talk: call [startOutdoorListening], stop with [stopListening].
 *
 * **CASA** — continuous wake-word: call [startCasaListening].
 * Listens continuously; when "alfred" is heard in the transcript it transitions to
 * command-capture mode, fires [onCommandDetected], then resumes wake-word listening.
 *
 * All [SpeechRecognizer] operations are posted to the main thread internally.
 */
@Singleton
class SpeechInputService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Minimum time (ms) the mic stays open in LISTENING_FOR_COMMAND before giving up. */
        private const val MIN_LISTEN_DURATION_MS = 5000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    /** Timestamp when the current LISTENING_FOR_COMMAND session started. */
    private var commandPhaseStartMs: Long = 0L

    private val _state = MutableStateFlow(SpeechInputState())
    val state: StateFlow<SpeechInputState> = _state

    /** Called with the final recognised text in OUTDOOR mode. */
    var onFinalResult: ((String) -> Unit)? = null

    /** Called in CASA mode after the wake-word "alfred" is heard; payload is the command. */
    var onCommandDetected: ((String) -> Unit)? = null

    /** Called with live partial transcript while the user is speaking. */
    var onPartialResult: ((String) -> Unit)? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun startCasaListening() {
        _state.update { it.copy(casaPhase = CasaPhase.LISTENING_FOR_WAKE) }
        mainHandler.post { launchRecognizer(CasaPhase.LISTENING_FOR_WAKE) }
    }

    fun startOutdoorListening() {
        _state.update { it.copy(casaPhase = CasaPhase.IDLE) }
        mainHandler.post { launchRecognizer(null) }
    }

    fun stopListening() {
        mainHandler.post {
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
            commandPhaseStartMs = 0L
            _state.update { it.copy(isListening = false, casaPhase = CasaPhase.IDLE, partialText = "") }
        }
    }

    fun release() {
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
            _state.update { SpeechInputState() }
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun launchRecognizer(casaPhase: CasaPhase?) {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(makeListener(casaPhase))
            startListening(buildIntent())
        }
        _state.update { it.copy(isListening = true) }
    }

    private fun makeListener(casaPhase: CasaPhase?) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            _state.update { it.copy(partialText = partial) }
            onPartialResult?.invoke(partial)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()

            _state.update { it.copy(partialText = "", isListening = false) }

            if (text.isNullOrBlank()) {
                if (casaPhase != null) scheduleRestart(casaPhase)
                return
            }

            when (casaPhase) {
                CasaPhase.LISTENING_FOR_WAKE -> handleWakePhaseResult(text)
                CasaPhase.LISTENING_FOR_COMMAND -> {
                    onCommandDetected?.invoke(text)
                    scheduleRestart(CasaPhase.LISTENING_FOR_WAKE)
                }
                null -> onFinalResult?.invoke(text) // OUTDOOR
                else -> {}
            }
        }

        override fun onError(error: Int) {
            _state.update { it.copy(isListening = false, partialText = "") }
            if (casaPhase == CasaPhase.LISTENING_FOR_COMMAND) {
                // Keep the mic open until MIN_LISTEN_DURATION_MS has elapsed
                val elapsed = System.currentTimeMillis() - commandPhaseStartMs
                if (elapsed < MIN_LISTEN_DURATION_MS) {
                    scheduleRestart(CasaPhase.LISTENING_FOR_COMMAND)
                } else {
                    // Timeout expired — give up and go back to wake-word listening
                    scheduleRestart(CasaPhase.LISTENING_FOR_WAKE)
                }
            } else if (casaPhase != null) {
                scheduleRestart(casaPhase)
            }
        }
    }

    private fun handleWakePhaseResult(text: String) {
        val lower = text.lowercase()
        val idx = lower.indexOf("alfred")
        if (idx < 0) {
            // Wake word not found, keep listening
            scheduleRestart(CasaPhase.LISTENING_FOR_WAKE)
            return
        }
        val command = text.substring(idx + 6).trim()
        if (command.isNotBlank()) {
            // e.g. "Hey Alfred what's the weather" — command in same utterance
            onCommandDetected?.invoke(command)
            scheduleRestart(CasaPhase.LISTENING_FOR_WAKE)
        } else {
            // "Alfred" alone — next utterance is the command; start the timer
            commandPhaseStartMs = System.currentTimeMillis()
            _state.update { it.copy(casaPhase = CasaPhase.LISTENING_FOR_COMMAND) }
            mainHandler.post { launchRecognizer(CasaPhase.LISTENING_FOR_COMMAND) }
        }
    }

    private fun scheduleRestart(phase: CasaPhase) {
        if (_state.value.casaPhase == CasaPhase.IDLE) return
        _state.update { it.copy(casaPhase = phase) }
        mainHandler.postDelayed({
            if (_state.value.casaPhase != CasaPhase.IDLE) launchRecognizer(phase)
        }, 400L)
    }
}
