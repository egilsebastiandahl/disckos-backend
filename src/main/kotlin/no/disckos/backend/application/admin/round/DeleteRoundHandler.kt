package no.disckos.backend.application.admin.round

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.TeamMemberScoreRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteRoundHandler(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val teamScoreRepository: TeamScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val round = roundRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found") }

        val holes = holeRepository.findByRoundId(id)
        val holeIds = holes.map { it.id }
        if (holeIds.isNotEmpty()) {
            playerScoreRepository.deleteByHoleIdIn(holeIds)

            val teamScores = teamScoreRepository.findByHoleIdIn(holeIds)
            val teamScoreIds = teamScores.map { it.id }
            if (teamScoreIds.isNotEmpty()) {
                teamMemberScoreRepository.deleteByTeamScoreIdIn(teamScoreIds)
            }
            teamScoreRepository.deleteByHoleIdIn(holeIds)
            holeRepository.deleteByRoundId(id)
        }

        roundRepository.delete(round)
    }
}
