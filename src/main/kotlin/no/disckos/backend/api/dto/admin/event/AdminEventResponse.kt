package no.disckos.backend.api.dto.admin.event

import java.time.OffsetDateTime
import java.util.UUID

data class AdminEventResponse(
    val id: UUID,
    val date: OffsetDateTime,
    val title: String,
    val description: String,
    val locationId: UUID?,
    val teamEvent: Boolean,
    val rounds: Int,
    val published: Boolean
)
