package no.disckos.backend.application.admin.score

import no.disckos.backend.repository.TeamMemberScoreRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteTeamScoreHandler(
    private val teamScoreRepository: TeamScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val score = teamScoreRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team score not found") }
        teamMemberScoreRepository.deleteByTeamScoreIdIn(listOf(id))
        teamScoreRepository.delete(score)
    }
}
