package no.disckos.backend.api.dto.admin.round

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class AdminRoundResponse(
    val id: UUID,
    val eventId: UUID,
    val eventType: EventType,
    val scoringFormat: ScoringFormat,
    val holes: List<AdminHoleResponse>
)

data class AdminHoleResponse(
    val holeNumber: Int,
    val par: Int,
    val scoringFormatOverride: ScoringFormat?,
    val playerScores: List<AdminPlayerScoreResponse>?,
    val teamScores: List<AdminTeamScoreResponse>?
)

data class AdminPlayerScoreResponse(
    val playerId: UUID,
    val throws: Int
)

data class AdminTeamScoreResponse(
    val teamId: UUID,
    val teamThrows: Int,
    val memberScores: List<AdminTeamMemberScoreResponse>
)

data class AdminTeamMemberScoreResponse(
    val playerId: UUID,
    val throws: Int,
    val isCounted: Boolean
)
