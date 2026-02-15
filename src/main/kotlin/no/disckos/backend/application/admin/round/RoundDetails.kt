package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class RoundDetails(
    val id: UUID,
    val eventId: UUID,
    val eventType: EventType,
    val scoringFormat: ScoringFormat,
    val holes: List<HoleDetails>
)

data class HoleDetails(
    val holeNumber: Int,
    val par: Int,
    val scoringFormatOverride: ScoringFormat?,
    val playerScores: List<PlayerScoreDetails>?,
    val teamScores: List<TeamScoreDetails>?
)

data class PlayerScoreDetails(
    val playerId: UUID,
    val throws: Int
)

data class TeamScoreDetails(
    val teamId: UUID,
    val teamThrows: Int,
    val memberScores: List<TeamMemberScoreDetails>
)

data class TeamMemberScoreDetails(
    val playerId: UUID,
    val throws: Int,
    val isCounted: Boolean
)
