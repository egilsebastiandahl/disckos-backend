package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/** A member uploaded a UDisc card, which added their own score to [roundId] (migration 019). */
@Entity
@Table(name = "member_uploads")
class MemberUploadEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "round_id", nullable = false)
    var roundId: UUID,

    /** Null once the uploader's profile is deleted. */
    @Column(name = "profile_id")
    var profileId: UUID?,

    /** The uploader's player, whose score the upload added. */
    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "card_key", nullable = false)
    var cardKey: String,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)

@Suppress("EnumEntryName")
enum class ConfirmationStatus { pending, confirmed, rejected }

/** Another club member's score on a member upload, waiting for them or an admin to confirm it. */
@Entity
@Table(name = "score_confirmations")
class ScoreConfirmationEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    /** The member upload that asked for this; the row is deleted when that upload is undone. */
    @Column(name = "upload_id")
    var uploadId: UUID?,

    @Column(name = "round_id", nullable = false)
    var roundId: UUID,

    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "udisc_name", nullable = false)
    var udiscName: String,

    /** Throws per hole, comma-separated, hole 1 first. See [throwsList]. */
    @Column(name = "throws", nullable = false)
    var throws: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ConfirmationStatus = ConfirmationStatus.pending,

    @Column(name = "decided_by")
    var decidedBy: UUID? = null,

    @Column(name = "decided_at")
    var decidedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
) {
    val throwsList: List<Int> get() = throws.split(",").map { it.trim().toInt() }

    companion object {
        fun encodeThrows(throws: List<Int>): String = throws.joinToString(",")
    }
}
