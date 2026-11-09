package no.disckos.backend.repository

import no.disckos.backend.domain.LocationLayoutEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationLayoutRepository : JpaRepository<LocationLayoutEntity, UUID> {
    fun findByLocationIdAndLayoutKey(locationId: UUID, layoutKey: String): LocationLayoutEntity?
    fun findByCountsForRankFalse(): List<LocationLayoutEntity>
}
