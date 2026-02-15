package no.disckos.backend.application.admin.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.LocationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class CreateEventHandler(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository
) {
    @Transactional
    fun handle(cmd: CreateEventInput): EventEntity {
        if (!locationRepository.existsById(cmd.locationId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Location not found")
        }
        return eventRepository.save(
            EventEntity(
                id = UUID.randomUUID(),
                date = cmd.date,
                title = cmd.title.trim(),
                description = cmd.description.trim(),
                locationId = cmd.locationId,
                teamEvent = cmd.teamEvent,
                rounds = cmd.rounds
            )
        )
    }
}
