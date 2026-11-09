package no.disckos.backend.application.rank

import no.disckos.backend.domain.LocationLayoutEntity
import no.disckos.backend.repository.LocationLayoutRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class RankLayoutFilterTest {

    private val holmenkollen = UUID.randomUUID()
    private val vollen = UUID.randomUUID()
    private val player = UUID.randomUUID()

    private fun round(location: UUID, layout: String?) = PlayerCourseRound(
        playerId = player,
        locationId = location,
        round = RankRound(UUID.randomUUID(), OffsetDateTime.parse("2026-10-31T14:11:00+01:00"), toPar = 0, holes = 18),
        layoutName = layout,
    )

    private val filter = RankLayoutFilter(
        mock<LocationLayoutRepository> {
            on { findByCountsForRankFalse() } doReturn listOf(
                LocationLayoutEntity(locationId = holmenkollen, layoutKey = "winter layout 2025/2026", name = "Winter layout 2025/2026", countsForRank = false),
            )
        },
    )

    @Test
    fun `drops rounds on a layout that doesn't count on that course`() {
        val normal = round(holmenkollen, "Normalt oppsett")
        val winter = round(holmenkollen, " Winter  layout 2025/2026")

        assertEquals(listOf(normal), filter.ranked(listOf(normal, winter)))
    }

    @Test
    fun `keeps rounds without a layout, undecided layouts and the same layout name on another course`() {
        val disckosRound = round(holmenkollen, null)
        val undecided = round(holmenkollen, "Layout May/June")
        val elsewhere = round(vollen, "Winter layout 2025/2026")

        assertEquals(listOf(disckosRound, undecided, elsewhere), filter.ranked(listOf(disckosRound, undecided, elsewhere)))
    }
}
