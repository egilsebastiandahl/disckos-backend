package no.disckos.backend.application.player

import no.disckos.backend.application.rank.PlayerCourseRound
import no.disckos.backend.application.rank.RankRound
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class HeadToHeadCalculatorTest {

    private val me = UUID.randomUUID()
    private val them = UUID.randomUUID()
    private val course = UUID.randomUUID()
    private val base = OffsetDateTime.parse("2026-06-01T12:00:00+02:00")

    private fun round(player: UUID, roundId: UUID, toPar: Int, day: Long) =
        PlayerCourseRound(player, course, RankRound(roundId, base.plusDays(day), toPar, 18))

    @Test
    fun `counts wins, losses and ties on rounds both finished, newest first`() {
        val win = UUID.randomUUID()
        val loss = UUID.randomUUID()
        val tie = UUID.randomUUID()
        val onlyMine = UUID.randomUUID()
        val onlyTheirs = UUID.randomUUID()

        val result = HeadToHeadCalculator.compare(
            mine = listOf(
                round(me, win, 2, day = 1),
                round(me, loss, 9, day = 2),
                round(me, tie, 5, day = 3),
                round(me, onlyMine, -3, day = 4),
            ),
            theirs = listOf(
                round(them, win, 6, day = 1),
                round(them, loss, 4, day = 2),
                round(them, tie, 5, day = 3),
                round(them, onlyTheirs, 0, day = 5),
            )
        )

        assertEquals(1, result.wins)
        assertEquals(1, result.losses)
        assertEquals(1, result.ties)
        assertEquals(listOf(tie, loss, win), result.rounds.map { it.roundId })
        assertEquals(9 to 4, result.rounds[1].let { it.toPar to it.opponentToPar })
    }

    @Test
    fun `no shared rounds`() {
        val result = HeadToHeadCalculator.compare(
            listOf(round(me, UUID.randomUUID(), 0, 1)),
            listOf(round(them, UUID.randomUUID(), 0, 1))
        )
        assertEquals(0, result.wins + result.losses + result.ties)
        assertEquals(emptyList(), result.rounds)
    }
}
