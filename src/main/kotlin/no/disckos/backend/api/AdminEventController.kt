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
import no.disckos.backend.repository.LocationRepository
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
    private val getAllEventsHandler: GetAllEventsHandler,
    private val locationRepository: LocationRepository
) {

    @GetMapping
    fun getEvents(): ResponseEntity<List<AdminEventResponse>> {
        val events = getAllEventsHandler.handle()
        val locationIds = events.mapNotNull { it.locationId }.distinct()
        val locationMap = if (locationIds.isEmpty()) {
            emptyMap()
        } else {
            locationRepository.findAllById(locationIds).associateBy { it.id }
        }
        return ResponseEntity.ok(events.map { it.toAdminResponse(locationMap[it.locationId]) })
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(@Valid @RequestBody request: CreateEventRequest): AdminEventResponse {
        val created = createEventHandler.handle(request.toInput())
        val location = created.locationId?.let { locationRepository.findById(it).orElse(null) }
        return created.toAdminResponse(location)
    }

    @PutMapping("/{id}")
    fun updateEvent(
        @PathVariable id: java.util.UUID,
        @RequestBody request: UpdateEventRequest
    ): AdminEventResponse {
        val updated = updateEventHandler.handle(request.toInput(id))
        val location = updated.locationId?.let { locationRepository.findById(it).orElse(null) }
        return updated.toAdminResponse(location)
    }

    @PostMapping("/{id}/publish")
    fun publishEvent(@PathVariable id: java.util.UUID): AdminEventResponse =
        publishEventHandler.handle(id).let { updated ->
            val location = updated.locationId?.let { locationRepository.findById(it).orElse(null) }
            updated.toAdminResponse(location)
        }

    @PostMapping("/{id}/unpublish")
    fun unpublishEvent(@PathVariable id: java.util.UUID): AdminEventResponse =
        unpublishEventHandler.handle(id).let { updated ->
            val location = updated.locationId?.let { locationRepository.findById(it).orElse(null) }
            updated.toAdminResponse(location)
        }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable id: java.util.UUID) {
        deleteEventHandler.handle(id)
    }
}
