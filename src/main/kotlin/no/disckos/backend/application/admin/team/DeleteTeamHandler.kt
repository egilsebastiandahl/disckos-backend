package no.disckos.backend.application.admin.team

import no.disckos.backend.repository.TeamRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteTeamHandler(
    private val teamRepository: TeamRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val team = teamRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found") }
        teamRepository.delete(team)
    }
}
