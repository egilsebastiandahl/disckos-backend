package no.disckos.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "profiles")
class Profile {
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID? = null

    @Column(name = "username", unique = true)
    var username: String? = null

    @Column(name = "display_name")
    var displayName: String? = null

    @Column(name = "avatar_url")
    var avatarUrl: String? = null

    @Column(name = "bio", length = Integer.MAX_VALUE)
    var bio: String? = null

    @Column(name = "is_admin")
    var isAdmin: Boolean? = false

    @Column(name = "settings", columnDefinition = "jsonb")
    var settings: String? = null

    @ColumnDefault("now()")
    @Column(name = "created_at")
    var createdAt: OffsetDateTime? = null

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null
}
