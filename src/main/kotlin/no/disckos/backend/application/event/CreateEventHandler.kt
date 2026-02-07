package no.disckos.backend.application.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateEventHandler(
    private val eventRepository: EventRepository
) {
    @Transactional
    fun handle(cmd: CreateEventInput): EventEntity =
        eventRepository.save(
            EventEntity(
                date = cmd.date,
                title = cmd.title.trim(),
                description = cmd.description.trim(),
                location = cmd.location.trim(),
                teamEvent = cmd.teamEvent,
                rounds = cmd.rounds
            )
        )
}