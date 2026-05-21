package no.disckos.backend.application.player

import no.disckos.backend.api.dto.player.PlayerRoundSummaryResponse
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class GetPlayerRoundsHandler(
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID, limit: Int): List<PlayerRoundSummaryResponse> {
        val rows = playerScoreRepository.findScoreRowsByPlayerId(playerId)
        if (rows.isEmpty()) return emptyList()

        return rows.groupBy { it.roundId }
            .map { (roundId, group) ->
                val first = group.first()
                val totalStrokes = group.sumOf { it.throws }
                val totalPar = group.sumOf { it.par }
                PlayerRoundSummaryResponse(
                    roundId = roundId,
                    eventId = first.eventId,
                    eventDate = first.eventDate,
                    locationId = first.locationId,
                    locationName = first.locationName,
                    totalStrokes = totalStrokes,
                    totalPar = totalPar,
                    toPar = totalStrokes - totalPar,
                    holesPlayed = group.size
                )
            }
            .sortedByDescending { it.eventDate }
            .take(limit.coerceAtLeast(1))
    }
}
