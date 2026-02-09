package no.disckos.backend.application.admin.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CreateEventHandler(
    private val eventRepository: EventRepository
) {
    @Transactional
    fun handle(cmd: CreateEventInput): EventEntity =
        eventRepository.save(
            EventEntity(
                id = UUID.randomUUID(),
                date = cmd.date,
                title = cmd.title.trim(),
                description = cmd.description.trim(),
                location = cmd.location.trim(),
                teamEvent = cmd.teamEvent,
                rounds = cmd.rounds
            )
        )
}