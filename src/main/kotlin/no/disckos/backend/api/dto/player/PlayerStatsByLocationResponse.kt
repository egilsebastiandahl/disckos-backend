package no.disckos.backend.api.dto.player

import java.math.BigDecimal
import java.util.UUID

data class PlayerStatsByLocationResponse(
    val playerId: UUID,
    val locations: List<PlayerStatsLocationItem>
)

data class PlayerStatsLocationItem(
    val locationId: UUID?,
    val locationName: String?,
    val roundsPlayed: Int,
    val holesPlayed: Int,
    val avgRoundScore: BigDecimal,
    val avgRoundToPar: BigDecimal,
    val bestRoundScore: Int?,
    val bestRoundToPar: Int?
)
