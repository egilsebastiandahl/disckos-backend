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
    name = "team_member_scores",
    uniqueConstraints = [UniqueConstraint(columnNames = ["team_score_id", "player_id"])]
)
class TeamMemberScoreEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "team_score_id", nullable = false)
    var teamScoreId: UUID,

    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "throws", nullable = false)
    var throws: Int,

    @Column(name = "is_counted", nullable = false)
    var isCounted: Boolean = false,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
