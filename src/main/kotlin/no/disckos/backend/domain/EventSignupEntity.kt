package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class EventSignupId(
    var eventId: UUID = UUID.randomUUID(),
    var profileId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "event_signups")
@IdClass(EventSignupId::class)
class EventSignupEntity(
    @Id
    @Column(name = "event_id", nullable = false)
    var eventId: UUID,

    @Id
    @Column(name = "profile_id", nullable = false)
    var profileId: UUID,

    @Column(name = "signed_up_at", nullable = false)
    var signedUpAt: OffsetDateTime = OffsetDateTime.now(),
)
