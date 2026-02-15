package no.disckos.backend.application.admin.team

import no.disckos.backend.domain.TeamEntity
import no.disckos.backend.repository.TeamRepository
import org.springframework.stereotype.Component

@Component
class GetTeamsHandler(
    private val teamRepository: TeamRepository
) {
    fun handle(eventId: java.util.UUID?): List<TeamEntity> =
        if (eventId == null) {
            teamRepository.findAll()
        } else {
            teamRepository.findByEventId(eventId)
        }
}
