package no.disckos.backend.application.admin.team

import no.disckos.backend.domain.TeamEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.TeamRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class CreateTeamHandler(
    private val teamRepository: TeamRepository,
    private val eventRepository: EventRepository
) {
    @Transactional
    fun handle(cmd: CreateTeamInput): TeamEntity {
        val event = eventRepository.findById(cmd.eventId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }

        if (!event.teamEvent) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not a team event")
        }

        return teamRepository.save(
            TeamEntity(
                id = UUID.randomUUID(),
                eventId = cmd.eventId,
                name = cmd.name.trim()
            )
        )
    }
}
