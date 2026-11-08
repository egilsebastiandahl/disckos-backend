package no.disckos.backend.application.player

import no.disckos.backend.api.dto.player.PlayerRoundSummaryResponse
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class GetPlayerRoundsHandler(
    private val playerScoreRepository: PlayerScoreRepository,
    private val holeRepository: HoleRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID, limit: Int): List<PlayerRoundSummaryResponse> {
        val rows = playerScoreRepository.findScoreRowsByPlayerId(playerId)
        if (rows.isEmpty()) return emptyList()
        val roundHoles = PlayerStatsAggregator.roundHoles(holeRepository, rows)

        return rows.groupBy { it.roundId }
            .map { (roundId, group) ->
                val first = group.first()
                val totalStrokes = group.sumOf { it.throws }
                val totalPar = group.sumOf { it.par }
                PlayerRoundSummaryResponse(
                    roundId = roundId,
                    eventId = first.eventId,
                    playedAt = first.playedAt,
                    locationId = first.locationId,
                    locationName = first.locationName,
                    totalStrokes = totalStrokes,
                    totalPar = totalPar,
                    toPar = totalStrokes - totalPar,
                    holesPlayed = group.size,
                    roundHoles = roundHoles[roundId] ?: group.size
                )
            }
            .sortedByDescending { it.playedAt }
            .take(limit.coerceAtLeast(1))
    }
}
