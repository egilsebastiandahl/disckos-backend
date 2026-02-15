package no.disckos.backend.api.dto.admin.score

import jakarta.validation.constraints.Min

data class UpdateTeamScoreRequest(
    @field:Min(1)
    val teamThrows: Int
)
