package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.TeamMemberScoreRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.stereotype.Component

@Component
class RoundDetailsLoader(
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val teamScoreRepository: TeamScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository
) {
    fun load(rounds: List<RoundEntity>): List<RoundDetails> {
        if (rounds.isEmpty()) return emptyList()

        val roundIds = rounds.map { it.id }
        val holes = holeRepository.findByRoundIdIn(roundIds)
        val holesByRound = holes.groupBy { it.roundId }
        val holeIds = holes.map { it.id }

        val playerScoresByHole = if (holeIds.isEmpty()) {
            emptyMap()
        } else {
            playerScoreRepository.findByHoleIdIn(holeIds).groupBy { it.holeId }
        }

        val teamScoresByHole = if (holeIds.isEmpty()) {
            emptyMap()
        } else {
            teamScoreRepository.findByHoleIdIn(holeIds).groupBy { it.holeId }
        }

        val teamScoreIds = teamScoresByHole.values.flatten().map { it.id }
        val teamMemberScoresByTeamScore = if (teamScoreIds.isEmpty()) {
            emptyMap()
        } else {
            teamMemberScoreRepository.findByTeamScoreIdIn(teamScoreIds).groupBy { it.teamScoreId }
        }

        return rounds.map { round ->
            val roundHoles = holesByRound[round.id].orEmpty().sortedBy { it.holeNumber }
            val holeDetails = roundHoles.map { hole ->
                when (round.eventType) {
                    EventType.individual -> {
                        val scores = playerScoresByHole[hole.id].orEmpty().map { score ->
                            PlayerScoreDetails(
                                playerId = score.playerId,
                                throws = score.throws
                            )
                        }
                        HoleDetails(
                            holeNumber = hole.holeNumber,
                            par = hole.par,
                            scoringFormatOverride = hole.scoringFormatOverride,
                            playerScores = scores,
                            teamScores = null
                        )
                    }
                    EventType.team -> {
                        val scores = teamScoresByHole[hole.id].orEmpty().map { teamScore ->
                            val members = teamMemberScoresByTeamScore[teamScore.id].orEmpty().map { member ->
                                TeamMemberScoreDetails(
                                    playerId = member.playerId,
                                    throws = member.throws,
                                    isCounted = member.isCounted
                                )
                            }
                            TeamScoreDetails(
                                teamId = teamScore.teamId,
                                teamThrows = teamScore.teamThrows,
                                memberScores = members
                            )
                        }
                        HoleDetails(
                            holeNumber = hole.holeNumber,
                            par = hole.par,
                            scoringFormatOverride = hole.scoringFormatOverride,
                            playerScores = null,
                            teamScores = scores
                        )
                    }
                }
            }

            RoundDetails(
                id = round.id,
                eventId = round.eventId,
                eventType = round.eventType,
                scoringFormat = round.scoringFormat,
                holes = holeDetails
            )
        }
    }
}
