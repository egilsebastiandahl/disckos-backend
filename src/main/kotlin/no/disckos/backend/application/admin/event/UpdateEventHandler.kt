package no.disckos.backend.application.admin.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.LocationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Component
class UpdateEventHandler(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository
) {
    @Transactional
    fun handle(cmd: UpdateEventInput): EventEntity {
        val event = eventRepository.findById(cmd.id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }

        cmd.date?.let { event.date = it }
        cmd.title?.let { event.title = it.trim() }
        cmd.description?.let { event.description = it.trim() }
        cmd.locationId?.let {
            if (!locationRepository.existsById(it)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Location not found")
            }
            event.locationId = it
        }
        cmd.teamEvent?.let { event.teamEvent = it }
        cmd.rounds?.let { event.rounds = it }

        return eventRepository.save(event)
    }
}
