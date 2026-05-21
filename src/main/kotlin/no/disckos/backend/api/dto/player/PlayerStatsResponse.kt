package no.disckos.backend.api.dto.player

import java.math.BigDecimal
import java.util.UUID

data class PlayerStatsResponse(
    val playerId: UUID,
    val holesPlayed: Int,
    val roundsPlayed: Int,
    val totalStrokes: Int,
    val totalPar: Int,
    val avgStrokesPerHole: BigDecimal,
    val avgToParPerHole: BigDecimal,
    val avgRoundScore: BigDecimal,
    val avgRoundToPar: BigDecimal,
    val bestRoundScore: Int?,
    val worstRoundScore: Int?,
    val bestRoundToPar: Int?,
    val worstRoundToPar: Int?,
    val aceCount: Int,
    val eagleCount: Int,
    val birdieCount: Int,
    val parCount: Int,
    val singleBogeyCount: Int,
    val doubleBogeyCount: Int,
    val tripleBogeyCount: Int,
    val worseThanTripleBogeyCount: Int
)
