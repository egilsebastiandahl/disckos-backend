package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("EnumEntryName")
enum class LinkRequestStatus { pending, approved, rejected }

/** A member asking to be linked to an existing player; an admin decides (migration 020). */
@Entity
@Table(name = "player_link_requests")
class PlayerLinkRequestEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "profile_id", nullable = false)
    var profileId: UUID,

    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: LinkRequestStatus = LinkRequestStatus.pending,

    @Column(name = "decided_by")
    var decidedBy: UUID? = null,

    @Column(name = "decided_at")
    var decidedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
