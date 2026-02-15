package no.disckos.backend.application.admin.score

import no.disckos.backend.domain.TeamScoreEntity
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateTeamScoreHandler(
    private val teamScoreRepository: TeamScoreRepository
) {
    @Transactional
    fun handle(id: UUID, teamThrows: Int): TeamScoreEntity {
        val score = teamScoreRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team score not found") }
        score.teamThrows = teamThrows
        return teamScoreRepository.save(score)
    }
}
