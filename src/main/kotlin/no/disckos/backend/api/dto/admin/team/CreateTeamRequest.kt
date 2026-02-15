package no.disckos.backend.api.dto.admin.team

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateTeamRequest(
    @field:NotNull
    val eventId: UUID,

    @field:NotBlank
    val name: String
)
