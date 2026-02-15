package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "rounds")
class RoundEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "event_id", nullable = false)
    var eventId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: EventType,

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_format", nullable = false)
    var scoringFormat: ScoringFormat,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
