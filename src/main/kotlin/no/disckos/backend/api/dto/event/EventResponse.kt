package no.disckos.backend.api.dto.event

import no.disckos.backend.api.dto.location.LocationResponse
import java.time.OffsetDateTime
import java.util.UUID

data class EventResponse(
    val id: UUID,
    val date: OffsetDateTime,
    val title: String,
    val description: String,
    val location: LocationResponse?,
    val teamEvent: Boolean,
    val rounds: Int
)
