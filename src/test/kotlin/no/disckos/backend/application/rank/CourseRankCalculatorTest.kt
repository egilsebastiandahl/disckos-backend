package no.disckos.backend.application.rank

import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseRankCalculatorTest {

    private val start = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC)
    private val now = OffsetDateTime.of(2026, 11, 8, 12, 0, 0, 0, ZoneOffset.UTC)

    /** Rounds played one day apart, oldest first, ending just before [now]. */
    private fun rounds(vararg toPar: Int, holes: Int = 18): List<RankRound> =
        toPar.mapIndexed { i, score ->
            RankRound(UUID.randomUUID(), now.minusDays((toPar.size - i).toLong()), score, holes)
        }

    private fun tier(name: String) = RankLadder.tiers.single { it.name == name }

    @Test
    fun `unranked until the placement rounds are played`() {
        val rank = CourseRankCalculator.calculate(rounds(4, 6), offset = 0.0, now = now)

        assertNull(rank.tier)
        assertNull(rank.rating)
        assertEquals(2, rank.roundsPlayed)
        assertEquals(RankLadder.PLACEMENT_ROUNDS - 2, rank.roundsToPlacement)
        assertNull(rank.lastChange)
    }

    @Test
    fun `placement averages the rounds played so far and counts as a rank up`() {
        val rank = CourseRankCalculator.calculate(rounds(4, 6, 5), offset = 0.0, now = now)

        assertEquals(5.0, rank.rating)
        assertEquals(tier("Chain Master III"), rank.tier)
        assertEquals(0, rank.roundsToPlacement)
        val change = assertNotNull(rank.lastChange)
        assertNull(change.from)
        assertTrue(change.up)
    }

    @Test
    fun `best five of the last ten count, older rounds drop out`() {
        // Two great rounds long ago, then ten recent ones: five at +2 and five at +20.
        val rank = CourseRankCalculator.calculate(
            rounds(-10, -10, 20, 2, 20, 2, 20, 2, 20, 2, 20, 2),
            offset = 0.0, now = now
        )

        assertEquals(2.0, rank.rating)
        assertEquals(5, rank.countedRounds)
        assertEquals(12, rank.roundsPlayed)
    }

    @Test
    fun `a bad day barely hurts`() {
        val steady = CourseRankCalculator.calculate(rounds(3, 3, 3, 3, 3), 0.0, now)
        val withDisaster = CourseRankCalculator.calculate(rounds(3, 3, 3, 3, 3, 40), 0.0, now)

        assertEquals(steady.tier, withDisaster.tier)
        assertEquals(steady.rating, withDisaster.rating)
    }

    @Test
    fun `thresholds are inclusive`() {
        assertEquals(tier("Chain Master III"), RankLadder.tierFor(5.0))
        assertEquals(tier("Chain Master II"), RankLadder.tierFor(5.2))
        assertEquals(tier("Chain Master III"), RankLadder.tierFor(23.0 / 5 + 0.4)) // floating-point noise
    }

    @Test
    fun `short courses are compared per 18 holes but shown in their own terms`() {
        // +3 on 9 holes is +6 per 18: Chain Master II (needs +7), next is Chain Master III (+5).
        val rank = CourseRankCalculator.calculate(rounds(3, 3, 3, holes = 9), 0.0, now)

        assertEquals(tier("Chain Master II"), rank.tier)
        assertEquals(3.0, rank.rating)
        assertEquals(2.5, rank.nextTierTarget)
        assertEquals(0.5, rank.progress)
    }

    @Test
    fun `a positive course offset makes ranks harder to reach`() {
        val fair = CourseRankCalculator.calculate(rounds(4, 4, 4), offset = 0.0, now = now)
        val easyCourse = CourseRankCalculator.calculate(rounds(4, 4, 4), offset = 2.0, now = now)

        assertEquals(tier("Chain Master III"), fair.tier)
        assertEquals(tier("Chain Master II"), easyCourse.tier)
        assertEquals(4.0, easyCourse.rating, "the shown average is the real score, not the adjusted one")
        assertEquals(3.0, easyCourse.nextTierTarget, "Chain Master III needs +5, minus the 2 stroke offset")
    }

    @Test
    fun `bottom and top of the ladder`() {
        val bottom = CourseRankCalculator.calculate(rounds(50, 50, 50), 0.0, now)
        assertEquals(RankLadder.tiers.first(), bottom.tier)
        assertEquals(0.0, bottom.progress)

        val top = CourseRankCalculator.calculate(rounds(-10, -10, -10), 0.0, now)
        assertEquals(RankLadder.tiers.last(), top.tier)
        assertNull(top.nextTier)
        assertNull(top.nextTierTarget)
        assertEquals(1.0, top.progress)
    }

    @Test
    fun `losing good rounds out of the window is a rank down`() {
        val rank = CourseRankCalculator.calculate(
            rounds(0, 0, 0, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10),
            0.0, now
        )

        assertEquals(tier("Circle Crusher III"), rank.tier)
        val change = assertNotNull(rank.lastChange)
        assertEquals(tier("Circle Crusher III"), change.to)
        assertFalse(change.up)
    }

    @Test
    fun `rank goes inactive after a long break but is kept`() {
        val old = (0 until 3).map { RankRound(UUID.randomUUID(), start.plusDays(it.toLong()), 5, 18) }
        val rank = CourseRankCalculator.calculate(old, 0.0, now)

        assertTrue(rank.inactive)
        assertEquals(tier("Chain Master III"), rank.tier)
        assertFalse(CourseRankCalculator.calculate(rounds(5, 5, 5), 0.0, now).inactive)
    }

    @Test
    fun `ladder is ordered with strictly falling thresholds`() {
        val thresholds = RankLadder.tiers.drop(1).map { it.maxToParPer18!! }
        assertEquals(thresholds.sortedDescending(), thresholds)
        assertEquals(thresholds.size, thresholds.toSet().size)
        assertEquals(RankLadder.tiers.indices.toList(), RankLadder.tiers.map { it.index })
        assertNull(RankLadder.tiers.first().maxToParPer18)
    }
}
