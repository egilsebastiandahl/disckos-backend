package no.disckos.backend.repository

import no.disckos.backend.domain.TeamEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeamRepository : JpaRepository<TeamEntity, UUID> {
    fun findByEventId(eventId: UUID): List<TeamEntity>
}
