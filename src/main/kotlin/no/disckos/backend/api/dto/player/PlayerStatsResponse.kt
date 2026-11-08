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
    /**
     * Best and worst rounds are picked by score to par, among rounds the player finished
     * (a score on every hole). Score, to-par and course always describe the same round.
     */
    val bestRoundScore: Int?,
    val worstRoundScore: Int?,
    val bestRoundToPar: Int?,
    val worstRoundToPar: Int?,
    val bestRoundLocationName: String?,
    val worstRoundLocationName: String?,
    val aceCount: Int,
    val eagleCount: Int,
    val birdieCount: Int,
    val parCount: Int,
    val singleBogeyCount: Int,
    val doubleBogeyCount: Int,
    val tripleBogeyCount: Int,
    val worseThanTripleBogeyCount: Int
)
