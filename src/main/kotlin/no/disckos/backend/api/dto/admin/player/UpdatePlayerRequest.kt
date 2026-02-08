package no.disckos.backend.api.dto.admin.player

import java.math.BigDecimal

data class UpdatePlayerRequest(
    val name: String? = null,
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
    val catchphrase: String? = null
)
