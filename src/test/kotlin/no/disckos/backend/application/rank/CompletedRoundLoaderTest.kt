package no.disckos.backend.application.rank

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class CompletedRoundLoaderTest {

    private val round = UUID.randomUUID()
    private val course = UUID.randomUUID()
    private val playedAt = OffsetDateTime.parse("2026-10-31T14:11:00+01:00")
    private val finisher = UUID.randomUUID()
    private val quitter = UUID.randomUUID()

    private fun row(player: UUID, throws: Int, par: Int = 3, location: UUID? = course) =
        RankScoreRow(player, throws, par, round, playedAt, location)

    private fun loader(rows: List<RankScoreRow>, holes: Long): CompletedRoundLoader {
        val scores = mock<PlayerScoreRepository> { on { findRankRows() } doReturn rows }
        val holeRepo = mock<HoleRepository> {
            on { countHolesByRoundIds(any()) } doReturn listOf(RoundHoleCount(round, holes))
        }
        return CompletedRoundLoader(scores, holeRepo)
    }

    @Test
    fun `only players with a score on every hole get the round`() {
        // 3-hole round; the quitter's last hole was never scored (filtered out as throws = 0 in SQL).
        val rows = listOf(
            row(finisher, 3), row(finisher, 2), row(finisher, 5, par = 4),
            row(quitter, 4), row(quitter, 3),
        )

        val result = loader(rows, holes = 3).forEveryone()

        assertEquals(1, result.size)
        val only = result.single()
        assertEquals(finisher, only.playerId)
        assertEquals(course, only.locationId)
        assertEquals(RankRound(round, playedAt, toPar = 0, holes = 3), only.round)
    }

    @Test
    fun `rounds without a course are skipped`() {
        val rows = listOf(row(finisher, 3, location = null))

        assertEquals(emptyList(), loader(rows, holes = 1).forEveryone())
    }
}
