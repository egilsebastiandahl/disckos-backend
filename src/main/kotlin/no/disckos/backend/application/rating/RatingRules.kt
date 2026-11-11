package no.disckos.backend.application.rating

/**
 * Disckos Rating: one number per player, across every course. Everything about it is tuned here.
 *
 * The scale is 1000 − 10 × (expected strokes over par per 18 holes on a course where par is fair):
 * par → 1000, +20 → 800, −5 → 1050. It looks a bit like a PDGA rating but is not one.
 */
object RatingRules {
    // Tuned 2026-11-11 on the real UDisc history in src/test/resources (see RatingBacktest), by
    // predicting each round from the rounds before it. Off by 4.2 strokes per round on average,
    // against 5.6 for a player's last-10 average. A 30-day half-life was a little more accurate
    // (4.1) but moved ratings ~11 points per round instead of ~8; a calmer number won.
    // Weak priors won clearly: with 3 rounds needed for a rating there's little left to shrink.
    val PARAMS = RatingParams(
        halfLifeDays = 180.0,
        halfLifeRounds = 10.0,
        playerPrior = 0.1,
        coursePrior = 0.25,
        outlierSd = 2.5
    )

    const val BASE = 1000.0
    const val POINTS_PER_STROKE = 10.0

    /** Complete rounds before a player gets a rating at all (same as placement on a course). */
    const val RATED_AFTER_ROUNDS = 3

    /** Below this many complete rounds the rating is shown as provisional. */
    const val PROVISIONAL_UNTIL_ROUNDS = 8

    /** Players whose latest round is older than this drop off the leaderboard (their rating is kept). */
    const val INACTIVE_AFTER_DAYS = 365L

    fun rating(toParPer18: Double): Double = BASE - POINTS_PER_STROKE * toParPer18
}
