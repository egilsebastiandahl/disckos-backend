package no.disckos.backend.api

import no.disckos.backend.api.dto.event.EventResponse
import no.disckos.backend.application.event.GetEventsHandler
import no.disckos.backend.domain.EventEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/event")
class EventController(
    private val getEventsHandler: GetEventsHandler,
) {
    // Gets all the events
    @GetMapping
    fun getAll(): List<EventResponse> =
        getEventsHandler.handle().map { it.toResponse() }


    private fun EventEntity.toResponse(): EventResponse =
        EventResponse(
            id = id,
            date = date,
            title = title,
            description = description,
            location = location,
            teamEvent = teamEvent,
            rounds = rounds
        )
}