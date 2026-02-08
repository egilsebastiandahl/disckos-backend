package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "placement_players")
class PlacementPlayer {
    @EmbeddedId
    var id: PlacementPlayerId? = null

    @MapsId("placementId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "placement_id", nullable = false)
    var placement: Placement? = null

    @MapsId("playerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "player_id", nullable = false)
    var player: Player? = null

    @NotNull
    @ColumnDefault("1")
    @Column(name = "player_order", nullable = false)
    var playerOrder: Int? = null

}