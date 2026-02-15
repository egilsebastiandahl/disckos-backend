package no.disckos.backend.application.admin.location

import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.repository.LocationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateLocationHandler(
    private val locationRepository: LocationRepository
) {
    @Transactional
    fun handle(id: UUID, cmd: UpdateLocationInput): LocationEntity {
        val location = locationRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found") }

        cmd.name?.let { location.name = it.trim() }
        cmd.description?.let { location.description = it.trim() }
        cmd.lat?.let { location.lat = it }
        cmd.lon?.let { location.lon = it }

        return locationRepository.save(location)
    }
}
