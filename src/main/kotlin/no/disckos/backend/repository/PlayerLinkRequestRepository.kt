package no.disckos.backend.repository

import no.disckos.backend.domain.LinkRequestStatus
import no.disckos.backend.domain.PlayerLinkRequestEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerLinkRequestRepository : JpaRepository<PlayerLinkRequestEntity, UUID> {
    fun findByProfileIdAndStatus(profileId: UUID, status: LinkRequestStatus): PlayerLinkRequestEntity?
    fun findByStatusOrderByCreatedAtAsc(status: LinkRequestStatus): List<PlayerLinkRequestEntity>
}
