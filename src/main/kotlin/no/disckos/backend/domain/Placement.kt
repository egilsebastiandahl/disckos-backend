package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.util.UUID

@Entity
@Table(name = "placements")
class Placement {
    @Id
    @ColumnDefault("uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "event_id")
    var event: EventEntity? = null

    @NotNull
    @Column(name = "\"position\"", nullable = false)
    var position: Int? = null

    @NotNull
    @Column(name = "players", nullable = false)
    var players: List<String>? = null

    @Size(max = 50)
    @NotNull
    @Column(name = "score", nullable = false, length = 50)
    var score: String? = null

    @Column(name = "quote", length = Integer.MAX_VALUE)
    var quote: String? = null

}