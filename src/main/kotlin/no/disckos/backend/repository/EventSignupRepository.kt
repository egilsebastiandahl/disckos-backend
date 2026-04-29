package no.disckos.backend.repository

import no.disckos.backend.domain.EventSignupEntity
import no.disckos.backend.domain.EventSignupId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventSignupRepository : JpaRepository<EventSignupEntity, EventSignupId> {
    fun findAllByEventId(eventId: UUID): List<EventSignupEntity>
    fun findAllByEventIdIn(eventIds: List<UUID>): List<EventSignupEntity>
    fun existsByEventIdAndProfileId(eventId: UUID, profileId: UUID): Boolean
    fun deleteByEventIdAndProfileId(eventId: UUID, profileId: UUID)
}
