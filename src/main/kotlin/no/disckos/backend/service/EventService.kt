package no.disckos.backend.service

import no.disckos.backend.api.dto.admin.event.CreateEventRequest
import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.LocationRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository
) {
    fun getAll(): List<EventEntity> =
        eventRepository.findAll(Sort.by(Sort.Direction.ASC, "date"))

    fun create(request: CreateEventRequest): EventEntity =
        if (!locationRepository.existsById(request.locationId)) {
            throw IllegalArgumentException("Location not found")
        } else {
        eventRepository.save(
            EventEntity(
                date = request.date,
                title = request.title.trim(),
                description = request.description.trim(),
                locationId = request.locationId,
                teamEvent = request.teamEvent,
                rounds = request.rounds
            )
        )
        }
}
