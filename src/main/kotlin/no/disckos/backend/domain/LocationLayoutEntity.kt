package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Whether rounds on one UDisc layout of a location count in the rank ladder (migration 017).
 * A layout without a row counts.
 */
@Entity
@Table(name = "location_layouts")
class LocationLayoutEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "location_id", nullable = false)
    var locationId: UUID,

    /** Normalized with ImportNames.key(). */
    @Column(name = "layout_key", nullable = false)
    var layoutKey: String,

    /** As written in UDisc. */
    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "counts_for_rank", nullable = false)
    var countsForRank: Boolean,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
