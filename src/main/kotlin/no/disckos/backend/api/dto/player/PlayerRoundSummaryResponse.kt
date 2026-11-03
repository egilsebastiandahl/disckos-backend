package no.disckos.backend.api.dto.player

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerRoundSummaryResponse(
    val roundId: UUID,
    /** Null for standalone rounds (not part of an event). */
    val eventId: UUID?,
    /** The event's date, or a standalone round's own start time. */
    val playedAt: OffsetDateTime,
    val locationId: UUID?,
    val locationName: String?,
    val totalStrokes: Int,
    val totalPar: Int,
    val toPar: Int,
    val holesPlayed: Int
)
