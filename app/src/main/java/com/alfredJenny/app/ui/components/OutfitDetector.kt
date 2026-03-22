package com.alfredJenny.app.ui.components

/**
 * Detects outfit trigger words in user messages and/or AI responses
 * and returns the appropriate [JennyOutfit] change with a spoken phrase.
 *
 * Priority: SERATA > BIKINI > CASUAL (so "mare" wins over "casa").
 * Returns null when no outfit change is triggered.
 */
object OutfitDetector {

    data class OutfitChange(val outfit: JennyOutfit, val phrase: String)

    // ── Keyword sets ──────────────────────────────────────────────────────────

    private val SERATA_WORDS = setOf(
        "serata", "sera", "uscire", "usciremo", "usciamo", "elegante", "eleganza",
        "cena", "ristorante", "aperitivo", "party", "festa", "discoteca", "club",
        "evento", "gala", "cocktail", "brindisi", "ballo", "pista", "vestito da sera",
        "look da sera", "vestiti bene", "vestita bene"
    )

    private val BIKINI_WORDS = setOf(
        "mare", "spiaggia", "piscina", "caldo", "vacanza", "estate", "sole",
        "bagno", "nuotare", "lago", "tuffo", "ombrellone", "sdraio", "abbronzatura",
        "abbronzarsi", "costume", "bikini", "nuoto", "stabilimento", "riviera",
        "tropicale", "afoso", "torrido", "afa"
    )

    private val CASUAL_WORDS = setOf(
        "casual", "casa", "rilassato", "comodo", "mattina", "lavoro", "normale",
        "quotidiano", "ordinario", "semplice", "sportivo", "jeans", "maglietta",
        "abbigliamento casual", "look casual", "vestita casual", "casual look"
    )

    // ── Phrases ───────────────────────────────────────────────────────────────

    private val PHRASES = mapOf(
        JennyOutfit.CASUAL to listOf(
            "Eccomi in versione casual! Più comoda così. 😊",
            "Il look casual è sempre una buona scelta!",
            "Niente di meglio del comfort quotidiano!"
        ),
        JennyOutfit.SERATA to listOf(
            "Eccomi con il mio look da sera! ✨",
            "Pronta per la serata! Come mi sta?",
            "Un po' di eleganza non guasta mai! 💜",
            "Et voilà, il mio outfit da sera! 🌙"
        ),
        JennyOutfit.BIKINI to listOf(
            "Perfetto per la spiaggia! Come mi sta? 😊",
            "Pronta per il mare! Che caldo! 🌊",
            "Estate, sole e mare! Andiamo? ☀️",
            "Look estivo attivato! 🏖️"
        )
    )

    // ── Detection ─────────────────────────────────────────────────────────────

    /**
     * Analyses [userText] and optionally [aiText] for outfit triggers.
     *
     * @param userText  The user's message (checked first for explicit requests).
     * @param aiText    The AI reply (optional, checked for contextual triggers).
     * @return          [OutfitChange] or null if no trigger detected.
     */
    fun detect(userText: String, aiText: String = ""): OutfitChange? {
        val combined = (userText + " " + aiText).lowercase()

        val target = when {
            SERATA_WORDS.any { combined.contains(it) } -> JennyOutfit.SERATA
            BIKINI_WORDS.any { combined.contains(it) } -> JennyOutfit.BIKINI
            CASUAL_WORDS.any { combined.contains(it) } -> JennyOutfit.CASUAL
            else -> return null
        }

        val phrase = PHRASES[target]?.random() ?: return null
        return OutfitChange(target, phrase)
    }
}
