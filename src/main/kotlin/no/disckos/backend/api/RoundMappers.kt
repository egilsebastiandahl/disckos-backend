package no.disckos.backend.api

import no.disckos.backend.api.dto.round.HoleResponse
import no.disckos.backend.api.dto.round.PlayerScoreResponse
import no.disckos.backend.api.dto.round.RoundResponse
import no.disckos.backend.api.dto.round.TeamMemberScoreResponse
import no.disckos.backend.api.dto.round.TeamScoreResponse
import no.disckos.backend.application.admin.round.RoundDetails

fun RoundDetails.toResponse(): RoundResponse =
    RoundResponse(
        id = id,
        eventId = eventId,
        eventType = eventType,
        scoringFormat = scoringFormat,
        holes = holes.map { hole ->
            HoleResponse(
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride,
                playerScores = hole.playerScores?.map { score ->
                    PlayerScoreResponse(
                        playerId = score.playerId,
                        throws = score.throws
                    )
                },
                teamScores = hole.teamScores?.map { teamScore ->
                    TeamScoreResponse(
                        teamId = teamScore.teamId,
                        teamThrows = teamScore.teamThrows,
                        memberScores = teamScore.memberScores.map { member ->
                            TeamMemberScoreResponse(
                                playerId = member.playerId,
                                throws = member.throws,
                                isCounted = member.isCounted
                            )
                        }
                    )
                }
            )
        }
    )
