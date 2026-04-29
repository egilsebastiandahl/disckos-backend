package no.disckos.backend.api.dto.admin.teamtemplate

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AddTeamTemplateMemberRequest(
    @field:NotNull
    val playerId: UUID
)
