package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteTeamTemplateHandler(
    private val teamTemplateRepository: TeamTemplateRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val template = teamTemplateRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team template not found") }
        teamTemplateRepository.delete(template)
    }
}
