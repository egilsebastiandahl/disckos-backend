package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull
import org.hibernate.Hibernate
import java.io.Serializable
import java.util.Objects
import java.util.UUID

@Embeddable
class PlacementPlayerId : Serializable {
    @NotNull
    @Column(name = "placement_id", nullable = false)
    var placementId: UUID? = null

    @NotNull
    @Column(name = "player_id", nullable = false)
    var playerId: UUID? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as PlacementPlayerId

        return placementId == other.placementId &&
                playerId == other.playerId
    }

    override fun hashCode(): Int = Objects.hash(placementId, playerId)

    companion object {
        private const val serialVersionUID = 0L
    }
}