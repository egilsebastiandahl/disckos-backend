package no.disckos.backend.application.player

import no.disckos.backend.api.dto.player.PlayerStatsResponse
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Component
class GetPlayerStatsHandler(
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID): PlayerStatsResponse {
        val rows = playerScoreRepository.findScoreRowsByPlayerId(playerId)

        if (rows.isEmpty()) {
            return PlayerStatsResponse(
                playerId = playerId,
                holesPlayed = 0,
                roundsPlayed = 0,
                totalStrokes = 0,
                totalPar = 0,
                avgStrokesPerHole = BigDecimal.ZERO,
                avgToParPerHole = BigDecimal.ZERO,
                avgRoundScore = BigDecimal.ZERO,
                avgRoundToPar = BigDecimal.ZERO,
                bestRoundScore = null,
                worstRoundScore = null,
                bestRoundToPar = null,
                worstRoundToPar = null,
                aceCount = 0,
                eagleCount = 0,
                birdieCount = 0,
                parCount = 0,
                singleBogeyCount = 0,
                doubleBogeyCount = 0,
                tripleBogeyCount = 0,
                worseThanTripleBogeyCount = 0
            )
        }

        val rounds = PlayerStatsAggregator.roundTotals(rows)
        val totalStrokes = rows.sumOf { it.throws }
        val totalPar = rows.sumOf { it.par }

        var ace = 0; var eagle = 0; var birdie = 0; var par = 0
        var bogey1 = 0; var bogey2 = 0; var bogey3 = 0; var worse = 0
        for (row in rows) {
            val diff = row.throws - row.par
            when {
                row.throws == 1 -> ace++
                diff == -2 -> eagle++
                diff == -1 -> birdie++
                diff == 0 -> par++
                diff == 1 -> bogey1++
                diff == 2 -> bogey2++
                diff == 3 -> bogey3++
                diff >= 4 -> worse++
                diff <= -3 -> eagle++
            }
        }

        return PlayerStatsResponse(
            playerId = playerId,
            holesPlayed = rows.size,
            roundsPlayed = rounds.size,
            totalStrokes = totalStrokes,
            totalPar = totalPar,
            avgStrokesPerHole = PlayerStatsAggregator.decimal(totalStrokes, rows.size),
            avgToParPerHole = PlayerStatsAggregator.decimal(totalStrokes - totalPar, rows.size),
            avgRoundScore = PlayerStatsAggregator.decimal(rounds.sumOf { it.totalStrokes }, rounds.size),
            avgRoundToPar = PlayerStatsAggregator.decimal(rounds.sumOf { it.toPar }, rounds.size),
            bestRoundScore = rounds.minOf { it.totalStrokes },
            worstRoundScore = rounds.maxOf { it.totalStrokes },
            bestRoundToPar = rounds.minOf { it.toPar },
            worstRoundToPar = rounds.maxOf { it.toPar },
            aceCount = ace,
            eagleCount = eagle,
            birdieCount = birdie,
            parCount = par,
            singleBogeyCount = bogey1,
            doubleBogeyCount = bogey2,
            tripleBogeyCount = bogey3,
            worseThanTripleBogeyCount = worse
        )
    }
}
