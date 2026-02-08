package no.disckos.backend.application.admin.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Component
class UpdateEventHandler(
    private val eventRepository: EventRepository
) {
    @Transactional
    fun handle(cmd: UpdateEventInput): EventEntity {
        val event = eventRepository.findById(cmd.id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found") }

        cmd.date?.let { event.date = it }
        cmd.title?.let { event.title = it.trim() }
        cmd.description?.let { event.description = it.trim() }
        cmd.location?.let { event.location = it.trim() }
        cmd.teamEvent?.let { event.teamEvent = it }
        cmd.rounds?.let { event.rounds = it }

        return eventRepository.save(event)
    }
}
