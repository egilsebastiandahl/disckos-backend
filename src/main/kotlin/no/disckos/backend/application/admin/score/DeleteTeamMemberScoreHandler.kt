package no.disckos.backend.application.admin.score

import no.disckos.backend.repository.TeamMemberScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteTeamMemberScoreHandler(
    private val teamMemberScoreRepository: TeamMemberScoreRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val score = teamMemberScoreRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team member score not found") }
        teamMemberScoreRepository.delete(score)
    }
}
