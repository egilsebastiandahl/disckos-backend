package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.TeamMemberScoreEntity
import no.disckos.backend.domain.TeamScoreEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.TeamMemberScoreRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateTeamRoundHandler(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val teamScoreRepository: TeamScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository
) {
    @Transactional
    fun handle(id: UUID, cmd: UpdateTeamRoundInput): RoundDetails {
        val round = roundRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found") }
        if (round.eventType != EventType.team) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Round is not a team round")
        }

        val existingHoles = holeRepository.findByRoundId(id)
        val holeIds = existingHoles.map { it.id }
        if (holeIds.isNotEmpty()) {
            val teamScores = teamScoreRepository.findByHoleIdIn(holeIds)
            val teamScoreIds = teamScores.map { it.id }
            if (teamScoreIds.isNotEmpty()) {
                teamMemberScoreRepository.deleteByTeamScoreIdIn(teamScoreIds)
            }
            teamScoreRepository.deleteByHoleIdIn(holeIds)
            holeRepository.deleteByRoundId(id)
        }

        round.scoringFormat = cmd.scoringFormat
        roundRepository.save(round)

        val holes = cmd.holes.map { hole ->
            HoleEntity(
                id = UUID.randomUUID(),
                roundId = id,
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride
            ) to hole
        }
        holeRepository.saveAll(holes.map { it.first })

        val teamScores = mutableListOf<TeamScoreEntity>()
        val teamMemberScores = mutableListOf<TeamMemberScoreEntity>()

        holes.forEach { (holeEntity, holeInput) ->
            holeInput.teamScores.forEach { teamScore ->
                val teamScoreId = UUID.randomUUID()
                teamScores.add(
                    TeamScoreEntity(
                        id = teamScoreId,
                        holeId = holeEntity.id,
                        teamId = teamScore.teamId,
                        teamThrows = teamScore.teamThrows
                    )
                )
                teamScore.memberScores.forEach { memberScore ->
                    teamMemberScores.add(
                        TeamMemberScoreEntity(
                            id = UUID.randomUUID(),
                            teamScoreId = teamScoreId,
                            playerId = memberScore.playerId,
                            throws = memberScore.throws,
                            isCounted = memberScore.isCounted
                        )
                    )
                }
            }
        }

        teamScoreRepository.saveAll(teamScores)
        teamMemberScoreRepository.saveAll(teamMemberScores)

        return RoundDetails(
            id = id,
            eventId = round.eventId,
            eventType = round.eventType,
            scoringFormat = round.scoringFormat,
            holes = holes.map { (holeEntity, holeInput) ->
                HoleDetails(
                    holeNumber = holeEntity.holeNumber,
                    par = holeEntity.par,
                    scoringFormatOverride = holeEntity.scoringFormatOverride,
                    playerScores = null,
                    teamScores = holeInput.teamScores.map { teamScore ->
                        TeamScoreDetails(
                            teamId = teamScore.teamId,
                            teamThrows = teamScore.teamThrows,
                            memberScores = teamScore.memberScores.map { member ->
                                TeamMemberScoreDetails(
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
    }
}
