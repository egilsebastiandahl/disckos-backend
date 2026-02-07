package no.disckos.backend.service

import no.disckos.backend.api.dto.event.CreateEventRequest
import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class EventService(
    private val eventRepository: EventRepository
) {
    fun getAll(): List<EventEntity> =
        eventRepository.findAll(Sort.by(Sort.Direction.ASC, "date"))

    fun create(request: CreateEventRequest): EventEntity =
        eventRepository.save(
            EventEntity(
                date = request.date,
                title = request.title.trim(),
                description = request.description.trim(),
                location = request.location.trim(),
                teamEvent = request.teamEvent,
                rounds = request.rounds
            )
        )
}