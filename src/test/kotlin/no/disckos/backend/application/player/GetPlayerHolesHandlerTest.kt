package no.disckos.backend.application.player

import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class GetPlayerHolesHandlerTest {

    private val player = UUID.randomUUID()
    private val course = UUID.randomUUID()
    private val older = UUID.randomUUID()
    private val newer = UUID.randomUUID()
    private val olderAt = OffsetDateTime.parse("2026-05-19T18:00:00+02:00")
    private val newerAt = OffsetDateTime.parse("2026-09-25T16:58:00+02:00")

    private fun handler(rows: List<PlayerHoleScoreRow>, holes: List<HoleEntity>): GetPlayerHolesHandler {
        val scores = mock<PlayerScoreRepository> { on { findHoleRowsByPlayerIdAndLocationId(player, course) } doReturn rows }
        val holeRepo = mock<HoleRepository> { on { findByRoundIdIn(any()) } doReturn holes }
        return GetPlayerHolesHandler(scores, holeRepo)
    }

    @Test
    fun `returns each round hole by hole, oldest first, with gaps where the player has no score`() {
        val rows = listOf(
            PlayerHoleScoreRow(newer, 2, 3, 2, newerAt, "Normalt oppsett"),
            PlayerHoleScoreRow(newer, 1, 3, 4, newerAt, "Normalt oppsett"),
            PlayerHoleScoreRow(older, 1, 3, 3, olderAt, null),
            PlayerHoleScoreRow(older, 3, 4, 5, olderAt, null),
        )
        // Hole order in the repository is not guaranteed.
        val holes = listOf(
            HoleEntity(roundId = newer, holeNumber = 2, par = 3),
            HoleEntity(roundId = newer, holeNumber = 1, par = 3),
            HoleEntity(roundId = older, holeNumber = 3, par = 4),
            HoleEntity(roundId = older, holeNumber = 2, par = 3),
            HoleEntity(roundId = older, holeNumber = 1, par = 3),
        )

        val result = handler(rows, holes).handle(player, course)

        assertEquals(listOf(older, newer), result.map { it.roundId })
        assertEquals(listOf(3, 3, 4), result[0].pars)
        assertEquals(listOf(3, null, 5), result[0].throws, "hole 2 wasn't scored")
        assertEquals(null, result[0].layoutName)
        assertEquals(listOf(4, 2), result[1].throws)
        assertEquals("Normalt oppsett", result[1].layoutName)
        assertEquals(newerAt, result[1].playedAt)
    }

    @Test
    fun `no rounds on the course gives an empty list`() {
        assertEquals(emptyList(), handler(emptyList(), emptyList()).handle(player, course))
    }
}
