package no.disckos.backend.api.dto.admin.score

import jakarta.validation.constraints.Min

data class UpdatePlayerScoreRequest(
    @field:Min(1)
    val throws: Int
)
