package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("EnumEntryName")
enum class CourseRequestStatus { pending, created, linked, rejected }

/** A UDisc course a member uploaded rounds on that Disckos doesn't know yet (migration 021). */
@Entity
@Table(name = "course_requests")
class CourseRequestEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    /** As written in UDisc. */
    @Column(name = "udisc_course", nullable = false)
    var udiscCourse: String,

    /** [no.disckos.backend.application.admin.udiscimport.ImportNames.key] of [udiscCourse]. */
    @Column(name = "course_key", nullable = false)
    var courseKey: String,

    @Column(name = "requested_by")
    var requestedBy: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CourseRequestStatus = CourseRequestStatus.pending,

    /** The Disckos course it became or was linked to. */
    @Column(name = "location_id")
    var locationId: UUID? = null,

    @Column(name = "decided_by")
    var decidedBy: UUID? = null,

    @Column(name = "decided_at")
    var decidedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)

@Suppress("EnumEntryName")
enum class HeldUploadStatus { pending, discarded }

/** A member's round on a course waiting for an admin's decision. */
@Entity
@Table(name = "held_uploads")
class HeldUploadEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "course_request_id", nullable = false)
    var courseRequestId: UUID,

    @Column(name = "profile_id", nullable = false)
    var profileId: UUID,

    @Column(name = "player_id", nullable = false)
    var playerId: UUID,

    @Column(name = "uploader_udisc_name", nullable = false)
    var uploaderUdiscName: String,

    @Column(name = "card_key", nullable = false)
    var cardKey: String,

    @Column(name = "layout_name", nullable = false)
    var layoutName: String,

    /** Norwegian local time, as on the card. */
    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime,

    /** Par per hole, comma-separated, hole 1 first. */
    @Column(name = "pars", nullable = false)
    var pars: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: HeldUploadStatus = HeldUploadStatus.pending,

    @Column(name = "reason")
    var reason: String? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
) {
    val parList: List<Int> get() = pars.split(",").map { it.trim().toInt() }
}

/** One score on a held round: the uploader's or a club player's, never a guest's. */
@Entity
@Table(name = "held_upload_scores")
class HeldUploadScoreEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "held_upload_id", nullable = false)
    var heldUploadId: UUID,

    @Column(name = "udisc_name", nullable = false)
    var udiscName: String,

    /** Throws per hole, comma-separated, hole 1 first. */
    @Column(name = "throws", nullable = false)
    var throws: String
) {
    val throwsList: List<Int> get() = throws.split(",").map { it.trim().toInt() }
}
