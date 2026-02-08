package no.disckos.backend.repository

import no.disckos.backend.domain.EventEntity
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventRepository : JpaRepository<EventEntity, UUID> {
    fun findAllByPublishedTrue(sort: Sort): List<EventEntity>
}
