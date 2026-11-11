package no.disckos.backend.repository

import no.disckos.backend.domain.RoundEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface RoundRepository : JpaRepository<RoundEntity, UUID> {
    fun findByEventId(eventId: UUID): List<RoundEntity>
    fun findByEventIdOrderByCreatedAtAsc(eventId: UUID): List<RoundEntity>
    fun findByEventIdIsNullOrderByPlayedAtDesc(): List<RoundEntity>

    /** Standalone rounds on these courses in a time window, for spotting a round that's already in Disckos. */
    fun findByEventIdIsNullAndLocationIdInAndPlayedAtBetween(
        locationIds: Collection<UUID>,
        from: OffsetDateTime,
        to: OffsetDateTime
    ): List<RoundEntity>
}
