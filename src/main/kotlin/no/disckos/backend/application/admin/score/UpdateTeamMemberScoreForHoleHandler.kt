package no.disckos.backend.application.admin.score

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.TeamMemberScoreRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateTeamMemberScoreForHoleHandler(
    private val holeRepository: HoleRepository,
    private val teamScoreRepository: TeamScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository
) {
    @Transactional
    fun handle(
        roundId: UUID,
        holeNumber: Int,
        teamId: UUID,
        playerId: UUID,
        throws: Int?,
        isCounted: Boolean?
    ) =
        holeRepository.findByRoundIdAndHoleNumber(roundId, holeNumber)
            ?.let { hole ->
                val teamScore = teamScoreRepository.findByHoleIdAndTeamId(hole.id, teamId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Team score not found")
                val memberScore = teamMemberScoreRepository.findByTeamScoreIdAndPlayerId(teamScore.id, playerId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Team member score not found")
                throws?.let { memberScore.throws = it }
                isCounted?.let { memberScore.isCounted = it }
                teamMemberScoreRepository.save(memberScore)
            }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Hole not found")
}
