package no.disckos.backend.application.rating

import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerRatingsTest {

    private val now = OffsetDateTime.of(2026, 11, 11, 12, 0, 0, 0, ZoneOffset.UTC)
    private val course = RatingCourse(UUID.randomUUID(), null)

    /** [count] rounds at [toPar], one a day, the last one [daysAgo] days before [now]. */
    private fun rounds(player: UUID, count: Int, toPar: Int, daysAgo: Long = 1) =
        (1..count).map { i ->
            RatingRound(player, UUID.randomUUID(), course, now.minusDays(daysAgo + count - i), toPar, 18)
        }

    @Test
    fun `standings rank active rated players and leave out the rest`() {
        val good = UUID.randomUUID()
        val okay = UUID.randomUUID()
        val newcomer = UUID.randomUUID()
        val retired = UUID.randomUUID()
        val all = rounds(good, 10, 5) + rounds(okay, 4, 15) + rounds(newcomer, 2, 0) +
            rounds(retired, 10, 0, daysAgo = RatingRules.INACTIVE_AFTER_DAYS + 10)

        val standings = PlayerRatings.standings(all, RatingModel.fit(all), now)

        assertEquals(listOf(good, okay, retired, newcomer), standings.map { it.playerId })
        val (first, second, third, fourth) = standings
        assertEquals(1, first.position)
        assertFalse(first.provisional)
        assertEquals(2, second.position)
        assertTrue(second.provisional)
        // Inactive: keeps the rating, loses the position.
        assertFalse(third.active)
        assertNotNull(third.rating)
        assertNull(third.position)
        // Not rated yet.
        assertNull(fourth.rating)
        assertNull(fourth.position)
        assertEquals(RatingRules.RATED_AFTER_ROUNDS - 2, fourth.roundsToRating)
    }

    @Test
    fun `history starts at the round that earned the rating and ends at the current rating`() {
        val alice = UUID.randomUUID()
        val all = rounds(alice, 3, 20) + rounds(alice, 3, 10).map { it.copy(playedAt = it.playedAt.plusHours(1)) }
        val fit = RatingModel.fit(all)

        val history = PlayerRatings.history(alice, all, emptyMap(), fit)

        assertEquals(6 - RatingRules.RATED_AFTER_ROUNDS + 1, history.size)
        assertEquals(RatingRules.rating(fit.skill.getValue(alice)), history.last().rating)
        assertTrue(history.first().rating < history.last().rating, "improving player: $history")
    }

    @Test
    fun `a round's rating takes the course out of the score`() {
        val alice = UUID.randomUUID()
        val all = rounds(alice, 5, 10)
        val fit = RatingModel.fit(all, locationPriors = mapOf(course.locationId to 4.0))

        val ratings = PlayerRatings.roundRatings(alice, all, fit)

        // +10 on a course that plays 4 strokes hard is worth +6 on a par-fair course.
        ratings.values.forEach { assertTrue(abs(it - RatingRules.rating(6.0)) < 0.01, "was $it") }
    }
}
