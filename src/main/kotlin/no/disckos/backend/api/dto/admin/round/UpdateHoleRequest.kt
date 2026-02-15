package no.disckos.backend.api.dto.admin.round

import jakarta.validation.constraints.Min
import no.disckos.backend.domain.ScoringFormat

data class UpdateHoleRequest(
    @field:Min(1)
    val par: Int? = null,

    val scoringFormatOverride: ScoringFormat? = null,

    val clearScoringFormatOverride: Boolean? = null
)
