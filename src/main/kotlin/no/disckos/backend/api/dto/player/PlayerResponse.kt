package no.disckos.backend.api.dto.player

import java.time.OffsetDateTime
import java.util.UUID

data class PlayerResponse(
    val id: UUID,
    val name: String,
    val gender: String? = null,
    val catchphrase: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null
)
