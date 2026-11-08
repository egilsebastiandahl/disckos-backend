package no.disckos.backend.application.player

import no.disckos.backend.application.rank.CompletedRoundLoader
import no.disckos.backend.application.rank.PlayerCourseRound
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.repository.LocationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/** One round both players finished, from the first player's point of view. */
data class SharedRound(
    val roundId: UUID,
    val playedAt: OffsetDateTime,
    val locationId: UUID,
    val holes: Int,
    val toPar: Int,
    val opponentToPar: Int
)

data class HeadToHead(
    val wins: Int,
    val losses: Int,
    val ties: Int,
    /** Newest first. */
    val rounds: List<SharedRound>
)

object HeadToHeadCalculator {
    /**
     * Rounds both players finished (same round = same card or same event round), newest first.
     * Uses the same "complete individual stroke round" rule as ranks, so a player who quit
     * halfway doesn't lose (or win) the round.
     */
    fun compare(mine: List<PlayerCourseRound>, theirs: List<PlayerCourseRound>): HeadToHead {
        val theirsById = theirs.associateBy { it.round.roundId }
        val shared = mine.mapNotNull { m ->
            val t = theirsById[m.round.roundId] ?: return@mapNotNull null
            SharedRound(
                roundId = m.round.roundId,
                playedAt = m.round.playedAt,
                locationId = m.locationId,
                holes = m.round.holes,
                toPar = m.round.toPar,
                opponentToPar = t.round.toPar
            )
        }.sortedByDescending { it.playedAt }

        return HeadToHead(
            wins = shared.count { it.toPar < it.opponentToPar },
            losses = shared.count { it.toPar > it.opponentToPar },
            ties = shared.count { it.toPar == it.opponentToPar },
            rounds = shared
        )
    }
}

@Component
class GetHeadToHeadHandler(
    private val completedRoundLoader: CompletedRoundLoader,
    private val locationRepository: LocationRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID, opponentId: UUID): Pair<HeadToHead, Map<UUID, LocationEntity>> {
        val result = HeadToHeadCalculator.compare(
            completedRoundLoader.forPlayer(playerId),
            completedRoundLoader.forPlayer(opponentId)
        )
        val locations = locationRepository.findAllById(result.rounds.map { it.locationId }.toSet())
            .associateBy { it.id }
        return result to locations
    }
}
