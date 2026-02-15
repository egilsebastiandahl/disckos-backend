package no.disckos.backend.application.admin.location

import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.repository.LocationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class GetLocationsHandler(
    private val locationRepository: LocationRepository
) {
    fun handle(): List<LocationEntity> = locationRepository.findAll()

    fun handle(id: UUID): LocationEntity =
        locationRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found") }
}
