package no.disckos.backend.api.dto.admin.event

import java.time.OffsetDateTime
import no.disckos.backend.api.dto.admin.location.AdminLocationResponse
import java.util.UUID

data class AdminEventResponse(
    val id: UUID,
    val date: OffsetDateTime,
    val title: String,
    val description: String,
    val location: AdminLocationResponse?,
    val teamEvent: Boolean,
    val rounds: Int,
    val published: Boolean
)
