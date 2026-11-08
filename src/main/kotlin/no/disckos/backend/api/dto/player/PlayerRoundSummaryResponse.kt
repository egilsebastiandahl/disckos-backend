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
    /** Holes this player has a score on... */
    val holesPlayed: Int,
    /** ...out of the holes in the round. Equal means the player finished the round. */
    val roundHoles: Int
)
