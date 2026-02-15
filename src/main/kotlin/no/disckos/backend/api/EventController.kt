package no.disckos.backend.api

import no.disckos.backend.api.dto.event.EventResponse
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.application.event.GetEventsHandler
import no.disckos.backend.domain.EventEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/event")
class EventController(
    private val getEventsHandler: GetEventsHandler,
    private val locationRepository: LocationRepository
) {
    // Gets all the events
    @GetMapping
    fun getAll(): List<EventResponse> =
        getEventsHandler.handle().let { events ->
            val locationIds = events.mapNotNull { it.locationId }.distinct()
            val locationMap = if (locationIds.isEmpty()) {
                emptyMap()
            } else {
                locationRepository.findAllById(locationIds).associateBy { it.id }
            }
            events.map { it.toResponse(locationMap[it.locationId]) }
        }


    private fun EventEntity.toResponse(location: no.disckos.backend.domain.LocationEntity?): EventResponse =
        EventResponse(
            id = id,
            date = date,
            title = title,
            description = description,
            location = location?.toResponse(),
            teamEvent = teamEvent,
            rounds = rounds
        )
}
