package no.disckos.backend.application.admin.udiscimport

import kotlin.math.min

/** Name normalization and fuzzy matching for UDisc player and course names. */
object ImportNames {

    private val WHITESPACE = Regex("""\s+""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")

    /** Words that say "this is a disc golf course" and carry no information about which one. */
    private val COURSE_NOISE = setOf(
        "disc", "golf", "discgolf", "discgolfpark", "discgolfbane", "discgolfbanen",
        "frisbee", "frisbeegolf", "frisbeegolfbane", "frisbeegolfpark",
        "park", "parken", "bane", "banen", "course", "dgc", "dgp", "dg", "the", "og", "and",
    )

    /** Key used in the alias tables and for exact matching: trimmed, lower-cased, single spaces. */
    fun key(name: String): String = name.trim().lowercase().replace(WHITESPACE, " ")

    private fun tokens(name: String): List<String> = key(name).split(NON_WORD).filter { it.isNotEmpty() }

    /**
     * How likely two course names refer to the same place, from 0.0 to 1.0.
     * Ignores filler words and accepts prefix matches, so
     * "Ekebergsletta Discgolfpark" vs "Ekeberg Discgolf Park" scores 1.0.
     */
    fun courseSimilarity(a: String, b: String): Double {
        val ta = tokens(a).filterNot { it in COURSE_NOISE }.toSet()
        val tb = tokens(b).filterNot { it in COURSE_NOISE }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val (small, large) = if (ta.size <= tb.size) ta to tb else tb to ta
        val hits = small.count { s -> large.any { l -> tokenMatch(s, l) } }
        return hits.toDouble() / small.size
    }

    private fun tokenMatch(a: String, b: String) =
        a == b || (min(a.length, b.length) >= 4 && (a.startsWith(b) || b.startsWith(a)))

    /**
     * True when every word of the UDisc name is the start of a word in the player's name,
     * in order: "Seb D" and "Sebastian" both look like "Sebastian Dahl".
     */
    fun looksLikePlayer(udiscName: String, playerName: String): Boolean {
        val u = tokens(udiscName)
        val p = tokens(playerName)
        if (u.isEmpty() || u.first().length < 2 || u.size > p.size) return false
        var j = 0
        for (t in u) {
            while (j < p.size && !p[j].startsWith(t)) j++
            if (j == p.size) return false
            j++
        }
        return true
    }
}
