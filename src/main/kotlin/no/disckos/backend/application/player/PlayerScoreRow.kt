package no.disckos.backend.application.player

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerScoreRow(
    val throws: Int,
    val par: Int,
    val roundId: UUID,
    val eventId: UUID,
    val eventDate: OffsetDateTime,
    val locationId: UUID?,
    val locationName: String?
)
