package no.disckos.backend.application.admin.event

import java.time.OffsetDateTime
import java.util.UUID

data class UpdateEventInput(
    val id: UUID,
    val date: OffsetDateTime? = null,
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val teamEvent: Boolean? = null,
    val rounds: Int? = null
)
