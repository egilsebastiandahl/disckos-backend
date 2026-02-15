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
    name = "team_scores",
    uniqueConstraints = [UniqueConstraint(columnNames = ["hole_id", "team_id"])]
)
class TeamScoreEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "hole_id", nullable = false)
    var holeId: UUID,

    @Column(name = "team_id", nullable = false)
    var teamId: UUID,

    @Column(name = "team_throws", nullable = false)
    var teamThrows: Int,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
