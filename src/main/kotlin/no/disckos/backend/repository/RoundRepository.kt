package no.disckos.backend.repository

import no.disckos.backend.domain.RoundEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RoundRepository : JpaRepository<RoundEntity, UUID> {
    fun findByEventId(eventId: UUID): List<RoundEntity>
    fun findByEventIdOrderByCreatedAtAsc(eventId: UUID): List<RoundEntity>
}
