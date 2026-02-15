package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "teams")
class TeamEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "event_id", nullable = false)
    var eventId: UUID,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
