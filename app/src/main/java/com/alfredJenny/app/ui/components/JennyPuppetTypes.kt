package com.alfredJenny.app.ui.components

import java.util.Calendar

// ── Eye states ────────────────────────────────────────────────────────────────

enum class EyeState(val assetFile: String) {
    OPEN("eyes_open.png"),
    HALF("eyes_half.png"),
    CLOSED("eyes_closed.png"),
    HAPPY("eyes_happy.png"),
    SURPRISED("eyes_surprised.png"),
}

// ── Mouth states ──────────────────────────────────────────────────────────────

enum class MouthState(val assetFile: String) {
    CLOSED("mouth_closed.png"),
    SMILE("mouth_smile.png"),
    OPEN_S("mouth_open_s.png"),
    OPEN_M("mouth_open_m.png"),
    OPEN_L("mouth_open_l.png"),
}

// ── Outfit ────────────────────────────────────────────────────────────────────

enum class JennyOutfit(val assetFile: String, val label: String) {
    CASUAL("body_casual.png", "Casual"),
    SERATA("body_serata.png", "Serata"),
    BIKINI("body_bikini.png", "Bikini"),
}

// ── Emotion detector ──────────────────────────────────────────────────────────

/**
 * Analyses AI reply text and returns the appropriate EyeState for Jenny.
 * Runs on the reply at StreamEvent.Done; result expires after ~5 seconds.
 */
object EmotionDetector {

    private val HAPPY_WORDS = setOf(
        "bene", "felice", "ottimo", "bravo", "perfetto", "amore", "meraviglioso",
        "fantastico", "grazie", "piacere", "bella", "divertente", "adoro",
        "😊", "❤️", "😄", "🎉", "♥", "benissimo", "contenta", "sorrido", "che bello"
    )
    private val SURPRISED_WORDS = setOf(
        "wow", "incredibile", "davvero", "ma no", "sorprendente", "inaspettato",
        "oh", "mamma mia", "che cosa", "assurdo", "impossibile", "😮", "‼️", "😱", "!!"
    )
    private val SAD_WORDS = setOf(
        "purtroppo", "mi dispiace", "triste", "difficile", "problema", "errore",
        "peccato", "non posso", "😔", "😢", "😕", "ahimè", "sigh"
    )

    fun detect(text: String): EyeState {
        val lower = text.lowercase()
        return when {
            SURPRISED_WORDS.any { lower.contains(it) } -> EyeState.SURPRISED
            HAPPY_WORDS.any { lower.contains(it) }     -> EyeState.HAPPY
            SAD_WORDS.any { lower.contains(it) }       -> EyeState.HALF
            else                                        -> EyeState.OPEN
        }
    }
}

// ── Outfit auto-selector ──────────────────────────────────────────────────────

/**
 * Returns the default outfit for the current hour of day.
 *
 *  06:00–17:59 → CASUAL
 *  18:00–21:59 → SERATA
 *  22:00–05:59 → BIKINI
 */
object OutfitManager {
    fun autoOutfit(): JennyOutfit {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return fromHour(hour)
    }

    fun fromHour(hour: Int): JennyOutfit = when (hour) {
        in 6..17  -> JennyOutfit.CASUAL
        in 18..21 -> JennyOutfit.SERATA
        else      -> JennyOutfit.BIKINI
    }
}
