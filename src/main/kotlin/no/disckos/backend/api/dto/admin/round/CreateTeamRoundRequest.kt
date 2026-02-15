package no.disckos.backend.api.dto.admin.round

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class CreateTeamRoundRequest(
    @field:NotNull
    val eventId: UUID,

    @field:NotNull
    val scoringFormat: ScoringFormat,

    @field:Size(min = 1)
    val holes: List<CreateTeamHoleRequest>
)

data class CreateTeamHoleRequest(
    @field:Min(1)
    val holeNumber: Int,

    @field:Min(1)
    val par: Int,

    val scoringFormatOverride: ScoringFormat?,

    @field:Size(min = 1)
    val teamScores: List<CreateTeamScoreRequest>
)

data class CreateTeamScoreRequest(
    @field:NotNull
    val teamId: UUID,

    @field:Min(1)
    val teamThrows: Int,

    @field:Size(min = 1)
    val memberScores: List<CreateTeamMemberScoreRequest>
)

data class CreateTeamMemberScoreRequest(
    @field:NotNull
    val playerId: UUID,

    @field:Min(1)
    val throws: Int,

    val isCounted: Boolean? = false
)
