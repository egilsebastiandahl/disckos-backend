package no.disckos.backend.application.admin.score

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateTeamScoreForHoleHandler(
    private val holeRepository: HoleRepository,
    private val teamScoreRepository: TeamScoreRepository
) {
    @Transactional
    fun handle(roundId: UUID, holeNumber: Int, teamId: UUID, teamThrows: Int) =
        holeRepository.findByRoundIdAndHoleNumber(roundId, holeNumber)
            ?.let { hole ->
                val score = teamScoreRepository.findByHoleIdAndTeamId(hole.id, teamId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Team score not found")
                score.teamThrows = teamThrows
                teamScoreRepository.save(score)
            }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Hole not found")
}
