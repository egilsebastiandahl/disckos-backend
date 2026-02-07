package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "events")
class EventEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "date", nullable = false)
    var date: OffsetDateTime,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "description", nullable = false, columnDefinition = "text")
    var description: String,

    @Column(name = "location", nullable = false, length = 255)
    var location: String,

    @Column(name = "team_event", nullable = false)
    var teamEvent: Boolean,

    @Column(name = "rounds", nullable = false)
    var rounds: Int
) {
    override fun toString(): String {
        return "EventEntity(id=$id, date=$date, title='$title', description='$description', location='$location', teamEvent=$teamEvent, rounds=$rounds)"
    }
}