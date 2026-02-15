package no.disckos.backend.api.dto.admin.round

import jakarta.validation.constraints.Min

data class UpdatePlayerScoreForHoleRequest(
    @field:Min(1)
    val throws: Int
)

data class UpdateTeamScoreForHoleRequest(
    @field:Min(1)
    val teamThrows: Int
)

data class UpdateTeamMemberScoreForHoleRequest(
    @field:Min(1)
    val throws: Int? = null,

    val isCounted: Boolean? = null
)
