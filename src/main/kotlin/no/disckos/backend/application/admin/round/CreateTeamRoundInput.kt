package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class CreateTeamRoundInput(
    val eventId: UUID,
    val scoringFormat: ScoringFormat,
    val holes: List<TeamHoleInput>
)

data class UpdateTeamRoundInput(
    val scoringFormat: ScoringFormat,
    val holes: List<TeamHoleInput>
)

data class TeamHoleInput(
    val holeNumber: Int,
    val par: Int,
    val scoringFormatOverride: ScoringFormat?,
    val teamScores: List<TeamScoreInput>
)

data class TeamScoreInput(
    val teamId: UUID,
    val teamThrows: Int,
    val memberScores: List<TeamMemberScoreInput>
)

data class TeamMemberScoreInput(
    val playerId: UUID,
    val throws: Int,
    val isCounted: Boolean
)
