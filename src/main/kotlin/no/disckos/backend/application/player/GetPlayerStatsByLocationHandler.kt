package no.disckos.backend.application.player

import no.disckos.backend.api.dto.player.PlayerStatsByLocationResponse
import no.disckos.backend.api.dto.player.PlayerStatsLocationItem
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class GetPlayerStatsByLocationHandler(
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID): PlayerStatsByLocationResponse {
        val rows = playerScoreRepository.findScoreRowsByPlayerId(playerId)

        val byLocation = rows.groupBy { it.locationId }
            .map { (locationId, locRows) ->
                val rounds = PlayerStatsAggregator.roundTotals(locRows)
                PlayerStatsLocationItem(
                    locationId = locationId,
                    locationName = locRows.first().locationName,
                    roundsPlayed = rounds.size,
                    holesPlayed = locRows.size,
                    avgRoundScore = PlayerStatsAggregator.decimal(
                        rounds.sumOf { it.totalStrokes }, rounds.size
                    ),
                    avgRoundToPar = PlayerStatsAggregator.decimal(
                        rounds.sumOf { it.toPar }, rounds.size
                    ),
                    bestRoundScore = rounds.minOfOrNull { it.totalStrokes },
                    bestRoundToPar = rounds.minOfOrNull { it.toPar }
                )
            }
            .sortedByDescending { it.roundsPlayed }

        return PlayerStatsByLocationResponse(
            playerId = playerId,
            locations = byLocation
        )
    }
}
