package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "holes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["round_id", "hole_number"])]
)
class HoleEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "round_id", nullable = false)
    var roundId: UUID,

    @Column(name = "hole_number", nullable = false)
    var holeNumber: Int,

    @Column(name = "par", nullable = false)
    var par: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_format_override")
    var scoringFormatOverride: ScoringFormat? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
