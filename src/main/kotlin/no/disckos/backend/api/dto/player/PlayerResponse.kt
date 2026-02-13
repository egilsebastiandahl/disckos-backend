package no.disckos.backend.api.dto.player

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PlayerResponse(
    val id: UUID,
    val name: String,
    val gender: String? = null,
    val roundsPlayed: Int? = null,
    val averageScore: BigDecimal? = null,
    val bestScore: Int? = null,
    val worstScore: Int? = null,
    val singleBogeyCount: Int? = null,
    val doubleBogeyCount: Int? = null,
    val tripleBogeyCount: Int? = null,
    val parCount: Int? = null,
    val birdieCount: Int? = null,
    val aceCount: Int? = null,
    val eagleCount: Int? = null,
    val throws: Int? = null,
    val catchphrase: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null
)