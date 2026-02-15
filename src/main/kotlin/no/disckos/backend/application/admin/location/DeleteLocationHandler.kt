package no.disckos.backend.application.admin.location

import no.disckos.backend.repository.LocationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeleteLocationHandler(
    private val locationRepository: LocationRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val location = locationRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found") }
        locationRepository.delete(location)
    }
}
