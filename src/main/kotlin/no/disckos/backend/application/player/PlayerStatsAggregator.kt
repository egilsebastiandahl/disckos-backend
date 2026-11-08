package no.disckos.backend.application.player

import no.disckos.backend.repository.HoleRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

internal data class RoundTotals(
    val roundId: UUID,
    val totalStrokes: Int,
    val totalPar: Int,
    /** Holes this player has a score on. */
    val holesPlayed: Int,
    val playedAt: OffsetDateTime,
    val locationName: String?,
    /** Holes in the round itself; null when not looked up. */
    val roundHoles: Int? = null
) {
    val toPar: Int get() = totalStrokes - totalPar

    /** The player has a score on every hole, so the total means something. */
    val complete: Boolean get() = roundHoles != null && holesPlayed == roundHoles
}

internal object PlayerStatsAggregator {

    fun roundTotals(rows: List<PlayerScoreRow>, roundHoles: Map<UUID, Int> = emptyMap()): List<RoundTotals> =
        rows.groupBy { it.roundId }
            .map { (roundId, group) ->
                val first = group.first()
                RoundTotals(
                    roundId = roundId,
                    totalStrokes = group.sumOf { it.throws },
                    totalPar = group.sumOf { it.par },
                    holesPlayed = group.size,
                    playedAt = first.playedAt,
                    locationName = first.locationName,
                    roundHoles = roundHoles[roundId]
                )
            }

    /** How many holes each round has, for telling finished rounds from abandoned ones. */
    fun roundHoles(holeRepository: HoleRepository, rows: List<PlayerScoreRow>): Map<UUID, Int> {
        val roundIds = rows.map { it.roundId }.toSet()
        if (roundIds.isEmpty()) return emptyMap()
        return holeRepository.countHolesByRoundIds(roundIds).associate { it.roundId to it.holeCount.toInt() }
    }

    /** Lowest score to par among finished rounds; ties go to the earliest. */
    fun bestRound(rounds: List<RoundTotals>): RoundTotals? =
        rounds.filter { it.complete }.minWithOrNull(compareBy<RoundTotals>({ it.toPar }, { it.playedAt }))

    /** Highest score to par among finished rounds; ties go to the earliest. */
    fun worstRound(rounds: List<RoundTotals>): RoundTotals? =
        rounds.filter { it.complete }.minWithOrNull(compareBy<RoundTotals>({ -it.toPar }, { it.playedAt }))

    fun decimal(numerator: Number, denominator: Int): BigDecimal =
        if (denominator == 0) BigDecimal.ZERO
        else BigDecimal(numerator.toDouble())
            .divide(BigDecimal(denominator), 3, RoundingMode.HALF_UP)
}
