package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class TeamTemplateMemberId(
    var teamTemplateId: UUID = UUID.randomUUID(),
    var playerId: UUID = UUID.randomUUID()
) : Serializable

@Entity
@Table(name = "team_template_members")
@IdClass(TeamTemplateMemberId::class)
class TeamTemplateMemberEntity(
    @Id
    @Column(name = "team_template_id", nullable = false)
    var teamTemplateId: UUID,

    @Id
    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "added_at", nullable = false, insertable = false, updatable = false)
    var addedAt: OffsetDateTime? = null
)
