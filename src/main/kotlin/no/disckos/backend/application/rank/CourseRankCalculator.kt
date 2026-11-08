package no.disckos.backend.application.rank

import java.time.OffsetDateTime
import java.util.UUID

/** One complete stroke-play round by one player on one course. */
data class RankRound(
    val roundId: UUID,
    val playedAt: OffsetDateTime,
    val toPar: Int,
    val holes: Int
) {
    val toParPer18: Double get() = toPar * 18.0 / holes
}

/** The most recent time a player's tier on a course changed, placement included. */
data class RankChange(
    /** Null when this change was the placement. */
    val from: RankTier?,
    val to: RankTier,
    val roundId: UUID,
    val playedAt: OffsetDateTime
) {
    val up: Boolean get() = from == null || to.index > from.index
}

data class CourseRank(
    val roundsPlayed: Int,
    /** 0 once placed. */
    val roundsToPlacement: Int,
    /** Null until placed. */
    val tier: RankTier?,
    /** Average to par of the counted rounds, in this course's own terms (not per 18). */
    val rating: Double?,
    val countedRounds: Int,
    val nextTier: RankTier?,
    /** The average to par, in this course's own terms, that reaches [nextTier]. */
    val nextTierTarget: Double?,
    /** 0..1 from the current tier's threshold towards the next one. */
    val progress: Double?,
    val lastPlayedAt: OffsetDateTime,
    val inactive: Boolean,
    val lastChange: RankChange?
)

/**
 * Turns one player's rounds on one course into a rank. Pure: no state is stored anywhere,
 * a rank is always recomputed from the rounds (and so are past rank changes).
 */
object CourseRankCalculator {

    /**
     * @param rounds the player's complete rounds on the course, any order, at least one.
     * @param offset the course's difficulty adjustment, strokes per 18 holes.
     */
    fun calculate(rounds: List<RankRound>, offset: Double, now: OffsetDateTime): CourseRank {
        require(rounds.isNotEmpty()) { "A course rank needs at least one round" }
        val ordered = rounds.sortedWith(compareBy<RankRound>({ it.playedAt }, { it.roundId }))
        val latest = ordered.last()
        val inactive = latest.playedAt.plusDays(RankLadder.INACTIVE_AFTER_DAYS).isBefore(now)

        if (ordered.size < RankLadder.PLACEMENT_ROUNDS) {
            return CourseRank(
                roundsPlayed = ordered.size,
                roundsToPlacement = RankLadder.PLACEMENT_ROUNDS - ordered.size,
                tier = null,
                rating = null,
                countedRounds = 0,
                nextTier = null,
                nextTierTarget = null,
                progress = null,
                lastPlayedAt = latest.playedAt,
                inactive = inactive,
                lastChange = null
            )
        }

        val counted = counted(ordered)
        val ratingPer18 = counted.average()
        val adjusted = ratingPer18 + offset
        val tier = RankLadder.tierFor(adjusted)
        val next = RankLadder.next(tier)
        // Shown in the course's own terms so a 9-hole course says "+3", not "+6 per 18".
        val toCourseTerms = latest.holes / 18.0

        return CourseRank(
            roundsPlayed = ordered.size,
            roundsToPlacement = 0,
            tier = tier,
            rating = ratingPer18 * toCourseTerms,
            countedRounds = counted.size,
            nextTier = next,
            nextTierTarget = next?.maxToParPer18?.let { (it - offset) * toCourseTerms },
            progress = progress(tier, adjusted),
            lastPlayedAt = latest.playedAt,
            inactive = inactive,
            lastChange = lastChange(ordered, offset)
        )
    }

    /** Per-18 scores that count: the best [RankLadder.COUNTED_ROUNDS] of the last [RankLadder.WINDOW_ROUNDS]. */
    internal fun counted(ordered: List<RankRound>): List<Double> =
        ordered.takeLast(RankLadder.WINDOW_ROUNDS)
            .map { it.toParPer18 }
            .sorted()
            .take(RankLadder.COUNTED_ROUNDS)

    private fun progress(tier: RankTier, adjusted: Double): Double {
        val next = RankLadder.next(tier) ?: return 1.0
        val nextMax = next.maxToParPer18!!
        val upper = tier.maxToParPer18 ?: run {
            // The bottom tier has no threshold of its own; treat it as wide as the step above.
            val stepAbove = nextMax - RankLadder.next(next)!!.maxToParPer18!!
            nextMax + stepAbove
        }
        return ((upper - adjusted) / (upper - nextMax)).coerceIn(0.0, 1.0)
    }

    /** Replays the rounds in order and returns the latest tier change. */
    private fun lastChange(ordered: List<RankRound>, offset: Double): RankChange? {
        var previous: RankTier? = null
        var change: RankChange? = null
        for (n in RankLadder.PLACEMENT_ROUNDS..ordered.size) {
            val prefix = ordered.subList(0, n)
            val tier = RankLadder.tierFor(counted(prefix).average() + offset)
            if (tier != previous) {
                val round = prefix.last()
                change = RankChange(from = previous, to = tier, roundId = round.roundId, playedAt = round.playedAt)
                previous = tier
            }
        }
        return change
    }
}
