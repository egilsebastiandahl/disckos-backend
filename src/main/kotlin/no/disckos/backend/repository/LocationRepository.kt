package no.disckos.backend.repository

import no.disckos.backend.domain.LocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationRepository : JpaRepository<LocationEntity, UUID>
