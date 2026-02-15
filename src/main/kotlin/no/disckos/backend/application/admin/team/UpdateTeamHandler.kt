package no.disckos.backend.application.admin.team

import no.disckos.backend.domain.TeamEntity
import no.disckos.backend.repository.TeamRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Component
class UpdateTeamHandler(
    private val teamRepository: TeamRepository
) {
    @Transactional
    fun handle(cmd: UpdateTeamInput): TeamEntity {
        val team = teamRepository.findById(cmd.id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found") }

        cmd.name?.let { team.name = it.trim() }

        return teamRepository.save(team)
    }
}
