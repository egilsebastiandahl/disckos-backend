package no.disckos.backend.application.admin.location

import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.repository.LocationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CreateLocationHandler(
    private val locationRepository: LocationRepository
) {
    @Transactional
    fun handle(cmd: CreateLocationInput): LocationEntity =
        locationRepository.save(
            LocationEntity(
                id = UUID.randomUUID(),
                name = cmd.name.trim(),
                description = cmd.description.trim(),
                lat = cmd.lat,
                lon = cmd.lon
            )
        )
}
