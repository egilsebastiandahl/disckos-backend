package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "player_scores",
    uniqueConstraints = [UniqueConstraint(columnNames = ["hole_id", "player_id"])]
)
class PlayerScoreEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "hole_id", nullable = false)
    var holeId: UUID,

    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "throws", nullable = false)
    var throws: Int,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
