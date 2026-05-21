package no.disckos.backend.api.dto.player

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerRoundSummaryResponse(
    val roundId: UUID,
    val eventId: UUID,
    val eventDate: OffsetDateTime,
    val locationId: UUID?,
    val locationName: String?,
    val totalStrokes: Int,
    val totalPar: Int,
    val toPar: Int,
    val holesPlayed: Int
)
