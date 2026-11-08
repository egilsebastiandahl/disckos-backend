package no.disckos.backend.application.player

import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerStatsAggregatorTest {

    private val base = OffsetDateTime.parse("2026-06-01T12:00:00+02:00")

    /** One round: [scores] are (throws, par) per hole the player scored, out of [roundHoles] holes. */
    private fun round(course: String, day: Long, roundHoles: Int, vararg scores: Pair<Int, Int>): Pair<List<PlayerScoreRow>, Pair<UUID, Int>> {
        val id = UUID.randomUUID()
        val rows = scores.map { (throws, par) ->
            PlayerScoreRow(throws, par, id, null, base.plusDays(day), UUID.randomUUID(), course)
        }
        return rows to (id to roundHoles)
    }

    @Test
    fun `best and worst are by score to par among finished rounds, with their course`() {
        val rounds = listOf(
            round("Holmenkollen", 1, 3, 3 to 3, 2 to 3, 3 to 3), // -1
            round("Krokhol", 2, 3, 5 to 3, 4 to 3, 4 to 3), // +4
            round("Muselunden", 3, 3, 2 to 3, 2 to 3), // -2 but only 2 of 3 holes: abandoned
        )
        val totals = PlayerStatsAggregator.roundTotals(
            rounds.flatMap { it.first },
            rounds.associate { it.second }
        )

        val best = PlayerStatsAggregator.bestRound(totals)!!
        assertEquals(-1, best.toPar)
        assertEquals("Holmenkollen", best.locationName)

        val worst = PlayerStatsAggregator.worstRound(totals)!!
        assertEquals(4, worst.toPar)
        assertEquals(13, worst.totalStrokes)
        assertEquals("Krokhol", worst.locationName)
    }

    @Test
    fun `no finished rounds means no best or worst`() {
        val (rows, holes) = round("Vollen", 1, 18, 3 to 3)
        val totals = PlayerStatsAggregator.roundTotals(rows, mapOf(holes))

        assertNull(PlayerStatsAggregator.bestRound(totals))
        assertNull(PlayerStatsAggregator.worstRound(totals))
    }
}
