package no.disckos.backend.api.dto.admin.round

import java.time.OffsetDateTime
import java.util.UUID

data class AdminStandaloneRoundResponse(
    val roundId: UUID,
    val playedAt: OffsetDateTime?,
    val locationId: UUID?,
    val locationName: String?,
    val pars: List<Int>,
    val players: List<AdminStandaloneRoundPlayerResponse>,
)

data class AdminStandaloneRoundPlayerResponse(
    val playerId: UUID,
    val name: String,
    val totalStrokes: Int,
    val holesPlayed: Int,
)
