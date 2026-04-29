package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.domain.TeamTemplateEntity
import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Component
class UpdateTeamTemplateHandler(
    private val teamTemplateRepository: TeamTemplateRepository
) {
    @Transactional
    fun handle(cmd: UpdateTeamTemplateInput): TeamTemplateEntity {
        val template = teamTemplateRepository.findById(cmd.id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team template not found") }

        cmd.name?.let { template.name = it.trim() }

        return teamTemplateRepository.save(template)
    }
}
