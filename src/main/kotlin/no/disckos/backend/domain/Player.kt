package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
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

    @Column(name = "catchphrase", length = Integer.MAX_VALUE)
    var catchphrase: String? = null

    @ColumnDefault("now()")
    @Column(name = "created_at")
    var createdAt: OffsetDateTime? = null

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null

}
