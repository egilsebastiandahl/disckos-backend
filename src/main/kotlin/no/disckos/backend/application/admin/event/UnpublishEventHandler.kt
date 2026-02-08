package no.disckos.backend.application.admin.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UnpublishEventHandler(
    private val eventRepository: EventRepository
) {
    @Transactional
    fun handle(id: UUID): EventEntity {
        val event = eventRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }
        event.published = false
        return eventRepository.save(event)
    }
}
