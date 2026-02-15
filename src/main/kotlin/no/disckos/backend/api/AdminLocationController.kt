package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.location.AdminLocationResponse
import no.disckos.backend.api.dto.admin.location.CreateLocationRequest
import no.disckos.backend.api.dto.admin.location.UpdateLocationRequest
import no.disckos.backend.application.admin.location.CreateLocationHandler
import no.disckos.backend.application.admin.location.DeleteLocationHandler
import no.disckos.backend.application.admin.location.GetLocationsHandler
import no.disckos.backend.application.admin.location.UpdateLocationHandler
import org.springframework.http.HttpStatus
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
import java.util.UUID

@RestController
@RequestMapping("/admin/location")
@PreAuthorize("hasRole('admin')")
class AdminLocationController(
    private val createLocationHandler: CreateLocationHandler,
    private val updateLocationHandler: UpdateLocationHandler,
    private val deleteLocationHandler: DeleteLocationHandler,
    private val getLocationsHandler: GetLocationsHandler
) {
    @GetMapping
    fun getLocations(): List<AdminLocationResponse> =
        getLocationsHandler.handle().map { it.toAdminResponse() }

    @GetMapping("/{id}")
    fun getLocation(@PathVariable id: UUID): AdminLocationResponse =
        getLocationsHandler.handle(id).toAdminResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createLocation(@Valid @RequestBody request: CreateLocationRequest): AdminLocationResponse =
        createLocationHandler.handle(request.toInput()).toAdminResponse()

    @PutMapping("/{id}")
    fun updateLocation(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateLocationRequest
    ): AdminLocationResponse =
        updateLocationHandler.handle(id, request.toInput()).toAdminResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteLocation(@PathVariable id: UUID) {
        deleteLocationHandler.handle(id)
    }
}
