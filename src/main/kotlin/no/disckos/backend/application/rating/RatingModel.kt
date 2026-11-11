package no.disckos.backend.application.rating

import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * What the model treats as one course: a location, split by UDisc layout where the round has one,
 * since layouts on the same location can play several strokes apart. Rounds created in Disckos
 * have no layout and share one course per location.
 */
data class RatingCourse(val locationId: UUID, val layout: String?)

/** One complete stroke-play round by one player, as the rating model sees it. */
data class RatingRound(
    val playerId: UUID,
    val roundId: UUID,
    val course: RatingCourse,
    val playedAt: OffsetDateTime,
    val toPar: Int,
    val holes: Int
) {
    val toParPer18: Double get() = toPar * 18.0 / holes
}

/** How the model weighs the evidence. The values in use live in [RatingRules.PARAMS]. */
data class RatingParams(
    /** A round this many days older than the player's latest round counts half... */
    val halfLifeDays: Double,
    /** ...and so does a round with this many of the player's rounds after it. Infinity = off. */
    val halfLifeRounds: Double,
    /** How hard a player's skill is pulled towards the club average, in 18-hole rounds of evidence. */
    val playerPrior: Double,
    /** How hard a course's difficulty is pulled towards its location's rank_offset, in 18-hole rounds. */
    val coursePrior: Double,
    /** Rounds further than this many standard deviations from what the model expected count less. Null = off. */
    val outlierSd: Double?
)

/**
 * The fitted model. Both numbers are strokes over par per 18 holes:
 * a player is expected to shoot [skill] + [difficulty] on a course.
 */
class RatingFit(
    /** Expected strokes over par per 18 on a course where par is fair. */
    val skill: Map<UUID, Double>,
    /** Strokes per 18 a course adds over a par-fair course. Negative = easy course / soft par. */
    val difficulty: Map<RatingCourse, Double>,
    /** Average skill across the players in the fit. */
    val clubAverage: Double
) {
    /** The round's score with the course taken out: what it would have been on a par-fair course. */
    fun adjustedToParPer18(round: RatingRound): Double =
        round.toParPer18 - (difficulty[round.course] ?: 0.0)

    fun expectedToParPer18(playerId: UUID, course: RatingCourse): Double =
        (skill[playerId] ?: clubAverage) + (difficulty[course] ?: 0.0)
}

/**
 * Fits every player's skill and every course's difficulty at once from all complete rounds:
 *
 *     score per 18 = skill[player] + difficulty[course] + noise
 *
 * Players who play the same courses tie the courses together, so a course's difficulty is
 * learned from the data instead of set by hand. Solved exactly as a ridge regression:
 * - each round is weighted by length (a 9-hole round is half the evidence) and by how recent it
 *   is next to the player's own latest round, in days and in rounds played since. Measuring
 *   against the player's latest round (not today) means a rating freezes while you don't play;
 * - skill is pulled lightly towards the club average and difficulty towards the location's
 *   rank_offset, which keeps players and courses with little data sensible;
 * - rounds far from what the model expected get less weight (Huber), refitted a few times;
 * - the scale is pinned so that, across all rounds, courses are as hard as their rank_offset
 *   says (par is fair on an average round). Adding a rarely played layout then barely moves anyone.
 *
 * Pure: nothing is stored, a fit is always recomputed from the rounds.
 */
object RatingModel {

    private const val ROBUST_PASSES = 3

    /** Never treat rounds as more consistent than this, in strokes per 18-hole round. */
    private const val MIN_SPREAD = 1.5

    /**
     * @param locationPriors difficulty to expect before there is data, per location (minus its
     *   rank_offset); a missing location is 0, par is fair.
     */
    fun fit(
        rounds: List<RatingRound>,
        locationPriors: Map<UUID, Double> = emptyMap(),
        params: RatingParams = RatingRules.PARAMS
    ): RatingFit {
        if (rounds.isEmpty()) return RatingFit(emptyMap(), emptyMap(), 0.0)

        val players = rounds.map { it.playerId }.distinct()
        val courses = rounds.map { it.course }.distinct()
        val playerIndex = players.withIndex().associate { (i, p) -> p to i }
        val courseIndex = courses.withIndex().associate { (i, c) -> c to players.size + i }
        val meanIndex = players.size + courses.size
        val n = meanIndex + 1
        fun prior(course: RatingCourse) = locationPriors[course.locationId] ?: 0.0

        val latest = rounds.groupBy { it.playerId }.mapValues { (_, rs) -> rs.maxOf { it.playedAt } }
        val roundsAfter = roundsAfter(rounds)
        val baseWeight = DoubleArray(rounds.size) { i ->
            val r = rounds[i]
            val ageDays = Duration.between(r.playedAt, latest.getValue(r.playerId)).toMinutes() / (60.0 * 24.0)
            r.holes / 18.0 * 0.5.pow(ageDays / params.halfLifeDays) * 0.5.pow(roundsAfter[i] / params.halfLifeRounds)
        }
        val robustWeight = DoubleArray(rounds.size) { 1.0 }
        val passes = if (params.outlierSd == null) 1 else ROBUST_PASSES

        var x = DoubleArray(n)
        repeat(passes) { pass ->
            val a = DoubleArray(n * n)
            val b = DoubleArray(n)
            rounds.forEachIndexed { i, r ->
                val w = baseWeight[i] * robustWeight[i]
                val p = playerIndex.getValue(r.playerId)
                val c = courseIndex.getValue(r.course)
                val s = r.toParPer18
                a[p * n + p] += w; a[p * n + c] += w; a[c * n + p] += w; a[c * n + c] += w
                b[p] += w * s; b[c] += w * s
            }
            val lp = params.playerPrior
            for (p in players.indices) {
                a[p * n + p] += lp; a[p * n + meanIndex] -= lp; a[meanIndex * n + p] -= lp; a[meanIndex * n + meanIndex] += lp
            }
            courses.forEach { course ->
                val c = courseIndex.getValue(course)
                a[c * n + c] += params.coursePrior
                b[c] += params.coursePrior * prior(course)
            }
            x = solve(a, b, n)

            if (pass < passes - 1) {
                // Residuals scaled to a full 18-hole round, where the noise is comparable.
                val residuals = DoubleArray(rounds.size) { i ->
                    val r = rounds[i]
                    val expected = x[playerIndex.getValue(r.playerId)] + x[courseIndex.getValue(r.course)]
                    (r.toParPer18 - expected) * sqrt(r.holes / 18.0)
                }
                val spread = maxOf(MIN_SPREAD, 1.4826 * median(residuals.map(::abs)))
                val limit = params.outlierSd!! * spread
                residuals.forEachIndexed { i, e -> robustWeight[i] = if (abs(e) <= limit) 1.0 else limit / abs(e) }
            }
        }

        // Skill and difficulty can trade strokes (all skills +1, all difficulties −1 fits the rounds
        // just as well), so pin the scale: on an average round, the course is as hard as its prior.
        val shift = rounds.sumOf { r -> r.holes / 18.0 * (x[courseIndex.getValue(r.course)] - prior(r.course)) } /
            rounds.sumOf { it.holes / 18.0 }

        return RatingFit(
            skill = players.associateWith { x[playerIndex.getValue(it)] + shift },
            difficulty = courses.associateWith { x[courseIndex.getValue(it)] - shift },
            clubAverage = x[meanIndex] + shift
        )
    }

    /** For each round, how many rounds the same player played after it. */
    private fun roundsAfter(rounds: List<RatingRound>): IntArray {
        val result = IntArray(rounds.size)
        rounds.indices.groupBy { rounds[it].playerId }.values.forEach { mine ->
            mine.sortedWith(compareBy({ rounds[it].playedAt }, { rounds[it].roundId }))
                .forEachIndexed { order, i -> result[i] = mine.size - 1 - order }
        }
        return result
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    /**
     * Solves a·x = b for a symmetric positive definite [n]×[n] matrix (row-major) by Cholesky.
     * The priors make the system positive definite: every player has at least one round.
     */
    private fun solve(a: DoubleArray, b: DoubleArray, n: Int): DoubleArray {
        for (j in 0 until n) {
            var d = a[j * n + j]
            for (k in 0 until j) d -= a[j * n + k] * a[j * n + k]
            check(d > 0) { "Rating system is not positive definite" }
            val diag = sqrt(d)
            a[j * n + j] = diag
            for (i in j + 1 until n) {
                var s = a[i * n + j]
                for (k in 0 until j) s -= a[i * n + k] * a[j * n + k]
                a[i * n + j] = s / diag
            }
        }
        val y = DoubleArray(n)
        for (i in 0 until n) {
            var s = b[i]
            for (k in 0 until i) s -= a[i * n + k] * y[k]
            y[i] = s / a[i * n + i]
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var s = y[i]
            for (k in i + 1 until n) s -= a[k * n + i] * x[k]
            x[i] = s / a[i * n + i]
        }
        return x
    }
}
