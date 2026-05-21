package no.disckos.backend.application.player

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

internal data class RoundTotals(
    val roundId: UUID,
    val totalStrokes: Int,
    val totalPar: Int,
    val holesPlayed: Int
) {
    val toPar: Int get() = totalStrokes - totalPar
}

internal object PlayerStatsAggregator {

    fun roundTotals(rows: List<PlayerScoreRow>): List<RoundTotals> =
        rows.groupBy { it.roundId }
            .map { (roundId, group) ->
                RoundTotals(
                    roundId = roundId,
                    totalStrokes = group.sumOf { it.throws },
                    totalPar = group.sumOf { it.par },
                    holesPlayed = group.size
                )
            }

    fun decimal(numerator: Number, denominator: Int): BigDecimal =
        if (denominator == 0) BigDecimal.ZERO
        else BigDecimal(numerator.toDouble())
            .divide(BigDecimal(denominator), 3, RoundingMode.HALF_UP)
}
