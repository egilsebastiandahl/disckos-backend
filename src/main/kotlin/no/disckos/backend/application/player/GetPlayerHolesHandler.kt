package no.disckos.backend.application.player

import no.disckos.backend.api.dto.player.PlayerHoleRoundResponse
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** A player's individual rounds on one course, hole by hole, oldest first. */
@Component
class GetPlayerHolesHandler(
    private val playerScoreRepository: PlayerScoreRepository,
    private val holeRepository: HoleRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID, locationId: UUID): List<PlayerHoleRoundResponse> {
        val rows = playerScoreRepository.findHoleRowsByPlayerIdAndLocationId(playerId, locationId)
        if (rows.isEmpty()) return emptyList()
        // Every hole of each round, so holes the player didn't score still get a par and a slot.
        val holesByRound = holeRepository.findByRoundIdIn(rows.map { it.roundId }.toSet()).groupBy { it.roundId }

        return rows.groupBy { it.roundId }
            .map { (roundId, scored) ->
                val holes = holesByRound[roundId].orEmpty().sortedBy { it.holeNumber }
                val throwsByHole = scored.associate { it.holeNumber to it.throws }
                PlayerHoleRoundResponse(
                    roundId = roundId,
                    playedAt = scored.first().playedAt,
                    layoutName = scored.first().layoutName,
                    pars = holes.map { it.par },
                    throws = holes.map { throwsByHole[it.holeNumber] }
                )
            }
            .sortedBy { it.playedAt }
    }
}
