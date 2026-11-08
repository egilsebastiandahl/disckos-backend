package no.disckos.backend.application.rank

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

/** One hole score row, as read for ranks. */
data class RankScoreRow(
    val playerId: UUID,
    val throws: Int,
    val par: Int,
    val roundId: UUID,
    /** The event's date, or a standalone round's own start time. */
    val playedAt: OffsetDateTime,
    val locationId: UUID?
)

data class RoundHoleCount(val roundId: UUID, val holeCount: Long)

data class PlayerCourseRound(val playerId: UUID, val locationId: UUID, val round: RankRound)

/**
 * Loads the rounds that count towards ranks: individual stroke-play rounds (event or standalone)
 * where the player has a real score on every hole. Live rounds in progress and abandoned rounds
 * are left out until they're complete, since the live scorecard stores 0 for unscored holes.
 */
@Component
class CompletedRoundLoader(
    private val playerScoreRepository: PlayerScoreRepository,
    private val holeRepository: HoleRepository
) {
    fun forPlayer(playerId: UUID): List<PlayerCourseRound> =
        complete(playerScoreRepository.findRankRowsByPlayerId(playerId))

    fun forEveryone(): List<PlayerCourseRound> =
        complete(playerScoreRepository.findRankRows())

    private fun complete(rows: List<RankScoreRow>): List<PlayerCourseRound> {
        val located = rows.filter { it.locationId != null }
        if (located.isEmpty()) return emptyList()

        val holeCounts = holeRepository.countHolesByRoundIds(located.map { it.roundId }.toSet())
            .associate { it.roundId to it.holeCount.toInt() }

        return located.groupBy { it.playerId to it.roundId }
            .mapNotNull { (key, holes) ->
                val (playerId, roundId) = key
                val holeCount = holeCounts[roundId] ?: return@mapNotNull null
                // (hole, player) is unique, so this counts distinct holes with a real score.
                if (holes.size != holeCount) return@mapNotNull null
                val first = holes.first()
                PlayerCourseRound(
                    playerId = playerId,
                    locationId = first.locationId!!,
                    round = RankRound(
                        roundId = roundId,
                        playedAt = first.playedAt,
                        toPar = holes.sumOf { it.throws - it.par },
                        holes = holeCount
                    )
                )
            }
    }
}
