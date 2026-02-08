package no.disckos.backend.api.dto.admin.event

import jakarta.validation.constraints.Min
import java.time.OffsetDateTime

data class UpdateEventRequest(
    val date: OffsetDateTime? = null,
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val teamEvent: Boolean? = null,
    @field:Min(1)
    val rounds: Int? = null
)
