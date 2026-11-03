package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/** A UDisc player name (normalized) that an admin has linked to a Disckos player. */
@Entity
@Table(name = "udisc_player_aliases")
class UdiscPlayerAliasEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "udisc_name", nullable = false, unique = true)
    var udiscName: String,

    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)

/** A UDisc course name (normalized) that an admin has linked to a Disckos location. */
@Entity
@Table(name = "udisc_course_aliases")
class UdiscCourseAliasEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "udisc_course", nullable = false, unique = true)
    var udiscCourse: String,

    @Column(name = "location_id", nullable = false)
    var locationId: UUID,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)

/** A UDisc scorecard (by [no.disckos.backend.application.admin.udiscimport.UdiscCard.key]) that has been imported. */
@Entity
@Table(name = "udisc_imported_cards")
class UdiscImportedCardEntity(
    @Id
    @Column(name = "card_key", nullable = false)
    var cardKey: String,

    @Column(name = "round_id", nullable = false)
    var roundId: UUID,

    @Column(name = "imported_at", nullable = false, insertable = false, updatable = false)
    var importedAt: OffsetDateTime? = null
)
