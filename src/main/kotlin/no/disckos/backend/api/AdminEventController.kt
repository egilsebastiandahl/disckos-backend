package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.event.AdminEventResponse
import no.disckos.backend.api.dto.admin.event.CreateEventRequest
import no.disckos.backend.api.dto.admin.event.UpdateEventRequest
import no.disckos.backend.application.admin.event.CreateEventHandler
import no.disckos.backend.application.admin.event.DeleteEventHandler
import no.disckos.backend.application.admin.event.GetAllEventsHandler
import no.disckos.backend.application.admin.event.PublishEventHandler
import no.disckos.backend.application.admin.event.UnpublishEventHandler
import no.disckos.backend.application.admin.event.UpdateEventHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/event")
@PreAuthorize("hasRole('admin')")
class AdminEventController(
    private val createEventHandler: CreateEventHandler,
    private val updateEventHandler: UpdateEventHandler,
    private val publishEventHandler: PublishEventHandler,
    private val unpublishEventHandler: UnpublishEventHandler,
    private val deleteEventHandler: DeleteEventHandler,
    private val getAllEventsHandler: GetAllEventsHandler
) {

    @GetMapping
    fun getEvents(): ResponseEntity<List<AdminEventResponse>> {
        return ResponseEntity.ok(getAllEventsHandler.handle().map { it.toAdminResponse() })
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(@Valid @RequestBody request: CreateEventRequest): AdminEventResponse {
        val created = createEventHandler.handle(request.toInput())
        return created.toAdminResponse()
    }

    @PutMapping("/{id}")
    fun updateEvent(
        @PathVariable id: java.util.UUID,
        @RequestBody request: UpdateEventRequest
    ): AdminEventResponse {
        val updated = updateEventHandler.handle(request.toInput(id))
        return updated.toAdminResponse()
    }

    @PostMapping("/{id}/publish")
    fun publishEvent(@PathVariable id: java.util.UUID): AdminEventResponse =
        publishEventHandler.handle(id).toAdminResponse()

    @PostMapping("/{id}/unpublish")
    fun unpublishEvent(@PathVariable id: java.util.UUID): AdminEventResponse =
        unpublishEventHandler.handle(id).toAdminResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable id: java.util.UUID) {
        deleteEventHandler.handle(id)
    }
}
