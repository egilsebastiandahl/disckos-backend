package no.disckos.backend.api.dto.admin.round

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class CreateIndividualRoundRequest(
    @field:NotNull
    val eventId: UUID,

    @field:NotNull
    val scoringFormat: ScoringFormat,

    @field:Size(min = 1)
    val holes: List<CreateIndividualHoleRequest>
)

data class CreateIndividualHoleRequest(
    @field:Min(1)
    val holeNumber: Int,

    @field:Min(1)
    val par: Int,

    val scoringFormatOverride: ScoringFormat?,

    @field:Size(min = 1)
    val playerScores: List<CreatePlayerScoreRequest>
)

data class CreatePlayerScoreRequest(
    @field:NotNull
    val playerId: UUID,

    @field:Min(1)
    val throws: Int
)
