package no.disckos.backend.api.dto.round

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class RoundResponse(
    val id: UUID,
    val eventId: UUID,
    val eventType: EventType,
    val scoringFormat: ScoringFormat,
    val holes: List<HoleResponse>
)

data class HoleResponse(
    val holeNumber: Int,
    val par: Int,
    val scoringFormatOverride: ScoringFormat?,
    val playerScores: List<PlayerScoreResponse>?,
    val teamScores: List<TeamScoreResponse>?
)

data class PlayerScoreResponse(
    val playerId: UUID,
    val throws: Int
)

data class TeamScoreResponse(
    val teamId: UUID,
    val teamThrows: Int,
    val memberScores: List<TeamMemberScoreResponse>
)

data class TeamMemberScoreResponse(
    val playerId: UUID,
    val throws: Int,
    val isCounted: Boolean
)
