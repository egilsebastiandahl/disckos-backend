package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "players")
class Player {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    var id: UUID? = null

    @NotNull
    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    var name: String? = null

    @Column(name = "gender", length = Integer.MAX_VALUE)
    var gender: String? = null

    @ColumnDefault("0")
    @Column(name = "rounds_played")
    var roundsPlayed: Int? = null

    @Column(name = "average_score", precision = 8, scale = 3)
    var averageScore: BigDecimal? = null

    @Column(name = "best_score")
    var bestScore: Int? = null

    @Column(name = "worst_score")
    var worstScore: Int? = null

    @ColumnDefault("0")
    @Column(name = "single_bogey_count")
    var singleBogeyCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "double_bogey_count")
    var doubleBogeyCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "triple_bogey_count")
    var tripleBogeyCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "par_count")
    var parCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "birdie_count")
    var birdieCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "ace_count")
    var aceCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "eagle_count")
    var eagleCount: Int? = null

    @ColumnDefault("0")
    @Column(name = "throws")
    var throwsField: Int? = null

    @Column(name = "catchphrase", length = Integer.MAX_VALUE)
    var catchphrase: String? = null

    @ColumnDefault("now()")
    @Column(name = "created_at")
    var createdAt: OffsetDateTime? = null

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null

}