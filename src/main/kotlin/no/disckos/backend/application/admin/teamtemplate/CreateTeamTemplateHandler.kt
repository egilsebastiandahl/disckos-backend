package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.domain.TeamTemplateEntity
import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CreateTeamTemplateHandler(
    private val teamTemplateRepository: TeamTemplateRepository
) {
    @Transactional
    fun handle(cmd: CreateTeamTemplateInput): TeamTemplateEntity {
        return teamTemplateRepository.save(
            TeamTemplateEntity(
                id = UUID.randomUUID(),
                name = cmd.name.trim()
            )
        )
    }
}
