package no.disckos.backend.api.dto.admin.player

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PlayerResponse(
    val id: UUID,
    val name: String,
    val gender: String?,
    val roundsPlayed: Int?,
    val averageScore: BigDecimal?,
    val bestScore: Int?,
    val worstScore: Int?,
    val singleBogeyCount: Int?,
    val doubleBogeyCount: Int?,
    val tripleBogeyCount: Int?,
    val parCount: Int?,
    val birdieCount: Int?,
    val aceCount: Int?,
    val eagleCount: Int?,
    val throws: Int?,
    val catchphrase: String?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?
)
