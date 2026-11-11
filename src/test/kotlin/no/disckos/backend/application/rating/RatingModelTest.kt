package no.disckos.backend.application.rating

import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RatingModelTest {

    private val start = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC)
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val carol = UUID.randomUUID()
    private val easy = RatingCourse(UUID.randomUUID(), null)
    private val hard = RatingCourse(UUID.randomUUID(), null)

    private var day = 0L
    private fun round(player: UUID, course: RatingCourse, toPar: Int, holes: Int = 18, at: OffsetDateTime? = null) =
        RatingRound(player, UUID.randomUUID(), course, at ?: start.plusDays(day++), toPar, holes)

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 0.3) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected ± $tolerance, was $actual")

    @Test
    fun `the rating scale is 1000 at par and 10 points per stroke`() {
        assertEquals(1000.0, RatingRules.rating(0.0))
        assertEquals(800.0, RatingRules.rating(20.0))
        assertEquals(1050.0, RatingRules.rating(-5.0))
    }

    @Test
    fun `learns how much harder a course is from players who play both`() {
        val rounds = (1..4).flatMap {
            listOf(round(alice, easy, 2), round(alice, hard, 8), round(bob, easy, 12), round(bob, hard, 18))
        }
        val fit = RatingModel.fit(rounds)

        assertNear(10.0, fit.skill.getValue(bob) - fit.skill.getValue(alice))
        assertNear(6.0, fit.difficulty.getValue(hard) - fit.difficulty.getValue(easy))
    }

    @Test
    fun `players who never meet are compared through a player who plays both courses`() {
        // Bob only plays the easy course, Carol only the hard one, and both score like Alice does there.
        val rounds = (1..5).flatMap {
            listOf(round(alice, easy, 2), round(alice, hard, 8), round(bob, easy, 2), round(carol, hard, 8))
        }
        val fit = RatingModel.fit(rounds)

        // Raw scores are 6 apart; through Alice they come out level, give or take the priors' light pull.
        assertNear(fit.skill.getValue(bob), fit.skill.getValue(carol), tolerance = 0.5)
    }

    @Test
    fun `a location's rank_offset sets the scale`() {
        // rank_offset +2 = soft par: shooting +8 there is worth +10 on a par-fair course.
        val rounds = (1..5).map { round(alice, easy, 8) }
        val fit = RatingModel.fit(rounds, locationPriors = mapOf(easy.locationId to -2.0))

        assertNear(10.0, fit.skill.getValue(alice), tolerance = 0.01)
        assertNear(-2.0, fit.difficulty.getValue(easy), tolerance = 0.01)
    }

    @Test
    fun `a 9-hole round counts per 18 holes`() {
        val fit = RatingModel.fit((1..5).map { round(alice, easy, 3, holes = 9) })

        assertNear(6.0, fit.skill.getValue(alice) + fit.difficulty.getValue(easy), tolerance = 0.01)
    }

    @Test
    fun `layouts on the same location get their own difficulty`() {
        val location = UUID.randomUUID()
        val short = RatingCourse(location, "short")
        val long = RatingCourse(location, "long")
        val rounds = (1..4).flatMap { listOf(round(alice, short, 0), round(alice, long, 9), round(bob, short, 10), round(bob, long, 19)) }
        val fit = RatingModel.fit(rounds)

        assertNear(9.0, fit.difficulty.getValue(long) - fit.difficulty.getValue(short), tolerance = 0.5)
    }

    @Test
    fun `recent rounds count more than old ones`() {
        val rounds = (1..10).map { round(alice, easy, 20) } + (1..10).map { round(alice, easy, 10) }
        val fit = RatingModel.fit(rounds)

        // A plain average would say +15.
        assertTrue(fit.expectedToParPer18(alice, easy) < 14.0, "was ${fit.expectedToParPer18(alice, easy)}")
    }

    @Test
    fun `a disaster round barely moves the rating`() {
        val steady = (1..9).flatMap { listOf(round(alice, easy, 10 + it % 3), round(bob, easy, 20 + it % 3)) }
        val disaster = round(alice, easy, 45)
        val damped = RatingModel.fit(steady + disaster)
        val undamped = RatingModel.fit(steady + disaster, params = RatingRules.PARAMS.copy(outlierSd = null))
        val without = RatingModel.fit(steady)

        val dampedMove = damped.skill.getValue(alice) - without.skill.getValue(alice)
        val undampedMove = undamped.skill.getValue(alice) - without.skill.getValue(alice)
        assertTrue(dampedMove < undampedMove / 2, "damped $dampedMove, undamped $undampedMove")
    }

    @Test
    fun `a rating does not drift while the player is away`() {
        val aliceRounds = (1..6).map { round(alice, easy, 10) }
        val before = RatingModel.fit(aliceRounds + (1..6).map { round(bob, easy, 20) })
        // A year later Bob keeps playing; Alice doesn't.
        day += 365
        val after = RatingModel.fit(aliceRounds + (1..6).map { round(bob, easy, 20) } + (1..6).map { round(bob, easy, 20) })

        assertNear(before.skill.getValue(alice), after.skill.getValue(alice), tolerance = 0.01)
    }

    /** Guards the tuning: on real rounds, the model must keep beating the best simple baseline. */
    @Test
    fun `predicts real rounds better than a player's recent average`() {
        val rounds = RatingBacktestData.load(byLayout = true).rounds
        val model = RatingBacktestData.evaluate(rounds, RatingBacktestData.model(RatingRules.PARAMS))
        val baseline = RatingBacktestData.evaluate(rounds, RatingBacktestData.playerRecentAverage)

        assertTrue(model.mae < baseline.mae - 1.0, "model $model, baseline $baseline")
        assertTrue(model.jump < 10.0, "rating moves too much per round: $model")
    }
}
