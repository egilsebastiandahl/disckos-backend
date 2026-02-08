package no.disckos.backend.application.admin.event

import no.disckos.backend.repository.EventRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteEventHandler(
    private val eventRepository: EventRepository
) {
    @Transactional
    fun handle(id: UUID) {
        if (!eventRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")
        }
        eventRepository.deleteById(id)
    }
}
