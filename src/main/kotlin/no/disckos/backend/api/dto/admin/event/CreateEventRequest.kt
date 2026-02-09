package no.disckos.backend.api.dto.admin.event

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

data class CreateEventRequest(
    @field:NotNull
    var date: OffsetDateTime,

    @field:NotBlank
    val title: String,

    @field:NotBlank
    val description: String,

    @field:NotBlank
    val location: String,

    val teamEvent: Boolean,

    @field:Min(1)
    val rounds: Int
)