package no.disckos.backend.api.dto.admin.score

import jakarta.validation.constraints.Min

data class UpdateTeamMemberScoreRequest(
    @field:Min(1)
    val throws: Int? = null,

    val isCounted: Boolean? = null
)
