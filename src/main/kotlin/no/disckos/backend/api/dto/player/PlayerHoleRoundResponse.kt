package no.disckos.backend.api.dto.player

import java.time.OffsetDateTime
import java.util.UUID

/** One of a player's rounds on a course, hole by hole. */
data class PlayerHoleRoundResponse(
    val roundId: UUID,
    /** The event's date, or a standalone round's own start time. */
    val playedAt: OffsetDateTime,
    /** UDisc layout of an imported round; null for rounds created in Disckos. */
    val layoutName: String?,
    /** Par per hole, hole 1 first. */
    val pars: List<Int>,
    /** The player's throws per hole, null where they have no score. Same length as [pars]. */
    val throws: List<Int?>
)
