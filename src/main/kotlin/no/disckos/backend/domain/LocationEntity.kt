package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "locations")
class LocationEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description", nullable = false, columnDefinition = "text")
    var description: String,

    @Column(name = "lat", nullable = false)
    var lat: Double,

    @Column(name = "lon", nullable = false)
    var lon: Double,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)
