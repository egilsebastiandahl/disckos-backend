package no.disckos.backend.api

import no.disckos.backend.api.dto.location.LocationResponse
import no.disckos.backend.application.admin.location.GetLocationsHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Public list of courses, e.g. for the weather pages. Editing stays under /admin/location. */
@RestController
@RequestMapping("/api/location")
class LocationController(
    private val getLocationsHandler: GetLocationsHandler
) {
    @GetMapping
    fun getLocations(): List<LocationResponse> =
        getLocationsHandler.handle()
            .sortedBy { it.name.lowercase() }
            .map { it.toResponse() }
}
