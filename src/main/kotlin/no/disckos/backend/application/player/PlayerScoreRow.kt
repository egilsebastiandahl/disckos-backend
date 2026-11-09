package no.disckos.backend.application.player

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerScoreRow(
    val throws: Int,
    val par: Int,
    val roundId: UUID,
    /** Null for standalone rounds. */
    val eventId: UUID?,
    /** The event's date, or a standalone round's own start time. */
    val playedAt: OffsetDateTime,
    val locationId: UUID?,
    val locationName: String?
)

/** One hole a player scored, for per-hole development on one course. */
data class PlayerHoleScoreRow(
    val roundId: UUID,
    val holeNumber: Int,
    val par: Int,
    val throws: Int,
    /** The event's date, or a standalone round's own start time. */
    val playedAt: OffsetDateTime,
    /** UDisc layout of an imported round; null for rounds created in Disckos. */
    val layoutName: String?
)
