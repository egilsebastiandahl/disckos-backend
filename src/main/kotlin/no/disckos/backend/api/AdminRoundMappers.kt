package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.round.AdminHoleResponse
import no.disckos.backend.api.dto.admin.round.AdminPlayerScoreResponse
import no.disckos.backend.api.dto.admin.round.AdminRoundResponse
import no.disckos.backend.api.dto.admin.round.AdminTeamMemberScoreResponse
import no.disckos.backend.api.dto.admin.round.AdminTeamScoreResponse
import no.disckos.backend.api.dto.admin.round.CreateIndividualRoundRequest
import no.disckos.backend.api.dto.admin.round.CreateTeamRoundRequest
import no.disckos.backend.api.dto.admin.round.UpdateIndividualRoundRequest
import no.disckos.backend.api.dto.admin.round.UpdateTeamRoundRequest
import no.disckos.backend.application.admin.round.CreateIndividualRoundInput
import no.disckos.backend.application.admin.round.CreateTeamRoundInput
import no.disckos.backend.application.admin.round.IndividualHoleInput
import no.disckos.backend.application.admin.round.PlayerScoreInput
import no.disckos.backend.application.admin.round.RoundDetails
import no.disckos.backend.application.admin.round.TeamHoleInput
import no.disckos.backend.application.admin.round.TeamMemberScoreInput
import no.disckos.backend.application.admin.round.TeamScoreInput
import no.disckos.backend.application.admin.round.UpdateIndividualRoundInput
import no.disckos.backend.application.admin.round.UpdateTeamRoundInput

fun CreateTeamRoundRequest.toInput(): CreateTeamRoundInput =
    CreateTeamRoundInput(
        eventId = eventId,
        scoringFormat = scoringFormat,
        holes = holes.map { hole ->
            TeamHoleInput(
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride,
                teamScores = hole.teamScores.map { score ->
                    TeamScoreInput(
                        teamId = score.teamId,
                        teamThrows = score.teamThrows,
                        memberScores = score.memberScores.map { member ->
                            TeamMemberScoreInput(
                                playerId = member.playerId,
                                throws = member.throws,
                                isCounted = member.isCounted ?: false
                            )
                        }
                    )
                }
            )
        }
    )

fun CreateIndividualRoundRequest.toInput(): CreateIndividualRoundInput =
    CreateIndividualRoundInput(
        eventId = eventId,
        scoringFormat = scoringFormat,
        holes = holes.map { hole ->
            IndividualHoleInput(
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride,
                playerScores = hole.playerScores.map { score ->
                    PlayerScoreInput(
                        playerId = score.playerId,
                        throws = score.throws
                    )
                }
            )
        }
    )

fun UpdateTeamRoundRequest.toInput(): UpdateTeamRoundInput =
    UpdateTeamRoundInput(
        scoringFormat = scoringFormat,
        holes = holes.map { hole ->
            TeamHoleInput(
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride,
                teamScores = hole.teamScores.map { score ->
                    TeamScoreInput(
                        teamId = score.teamId,
                        teamThrows = score.teamThrows,
                        memberScores = score.memberScores.map { member ->
                            TeamMemberScoreInput(
                                playerId = member.playerId,
                                throws = member.throws,
                                isCounted = member.isCounted ?: false
                            )
                        }
                    )
                }
            )
        }
    )

fun UpdateIndividualRoundRequest.toInput(): UpdateIndividualRoundInput =
    UpdateIndividualRoundInput(
        scoringFormat = scoringFormat,
        holes = holes.map { hole ->
            IndividualHoleInput(
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride,
                playerScores = hole.playerScores.map { score ->
                    PlayerScoreInput(
                        playerId = score.playerId,
                        throws = score.throws
                    )
                }
            )
        }
    )

fun RoundDetails.toAdminResponse(): AdminRoundResponse =
    AdminRoundResponse(
        id = id,
        eventId = eventId,
        eventType = eventType,
        scoringFormat = scoringFormat,
        holes = holes.map { hole ->
            AdminHoleResponse(
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride,
                playerScores = hole.playerScores?.map { score ->
                    AdminPlayerScoreResponse(
                        playerId = score.playerId,
                        throws = score.throws
                    )
                },
                teamScores = hole.teamScores?.map { teamScore ->
                    AdminTeamScoreResponse(
                        teamId = teamScore.teamId,
                        teamThrows = teamScore.teamThrows,
                        memberScores = teamScore.memberScores.map { member ->
                            AdminTeamMemberScoreResponse(
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
