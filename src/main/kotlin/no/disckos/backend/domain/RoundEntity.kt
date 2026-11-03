package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A round either belongs to an event (date and location come from the event) or is a
 * standalone round with its own [locationId] and [playedAt] — see migration 015.
 */
@Entity
@Table(name = "rounds")
class RoundEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "event_id")
    var eventId: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: EventType,

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_format", nullable = false)
    var scoringFormat: ScoringFormat,

    /** Only set for standalone rounds. */
    @Column(name = "location_id")
    var locationId: UUID? = null,

    /** Only set for standalone rounds. */
    @Column(name = "played_at")
    var playedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
