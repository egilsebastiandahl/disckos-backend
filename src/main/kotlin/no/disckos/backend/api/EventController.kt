package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.event.CreateEventRequest
import no.disckos.backend.api.dto.event.EventResponse
import no.disckos.backend.application.event.CreateEventInput
import no.disckos.backend.application.event.CreateEventHandler
import no.disckos.backend.application.event.GetEventsHandler
import no.disckos.backend.application.event.GetEventsQuery
import no.disckos.backend.domain.EventEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(
    private val getEventsHandler: GetEventsHandler,
    private val createEventHandler: CreateEventHandler
) {
    // Gets all the events
    @GetMapping
    fun getAll(): List<EventResponse> =
        getEventsHandler.handle().map { it.toResponse() }

    // Posts all the events
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateEventRequest): EventResponse =
        createEventHandler.handle(request.toInput()).toResponse()


    private fun CreateEventRequest.toInput(): CreateEventInput =
        CreateEventInput(
            date = date,
            title = title,
            description = description,
            location = location,
            teamEvent = teamEvent,
            rounds = rounds
        )

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