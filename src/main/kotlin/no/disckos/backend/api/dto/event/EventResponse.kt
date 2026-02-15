package no.disckos.backend.api.dto.event

import java.time.OffsetDateTime
import java.util.UUID

data class EventResponse(
    val id: UUID,
    val date: OffsetDateTime,
    val title: String,
    val description: String,
    val locationId: UUID?,
    val teamEvent: Boolean,
    val rounds: Int
)
