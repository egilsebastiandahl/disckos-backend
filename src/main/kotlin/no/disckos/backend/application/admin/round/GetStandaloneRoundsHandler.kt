package no.disckos.backend.application.admin.round

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class StandaloneRound(
    val roundId: UUID,
    val playedAt: OffsetDateTime?,
    val locationId: UUID?,
    val locationName: String?,
    val pars: List<Int>,
    val players: List<StandaloneRoundPlayer>,
)

data class StandaloneRoundPlayer(
    val playerId: UUID,
    val name: String,
    val totalStrokes: Int,
    val holesPlayed: Int,
)

/** Rounds that aren't part of an event (see migration 015), newest first. */
@Component
class GetStandaloneRoundsHandler(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val playerRepository: PlayerRepository,
    private val locationRepository: LocationRepository,
) {
    @Transactional(readOnly = true)
    fun handle(): List<StandaloneRound> {
        val rounds = roundRepository.findByEventIdIsNullOrderByPlayedAtDesc()
        if (rounds.isEmpty()) return emptyList()

        val holes = holeRepository.findByRoundIdIn(rounds.map { it.id })
        val holesByRound = holes.groupBy { it.roundId }
        val roundOfHole = holes.associate { it.id to it.roundId }
        val scores = if (holes.isEmpty()) emptyList() else playerScoreRepository.findByHoleIdIn(holes.map { it.id })
        val scoresByRound = scores.groupBy { roundOfHole.getValue(it.holeId) }
        val playerNames = playerRepository.findAllById(scores.map { it.playerId }.toSet())
            .associate { it.id to it.name.orEmpty() }
        val locationNames = locationRepository.findAllById(rounds.mapNotNull { it.locationId }.toSet())
            .associate { it.id to it.name }

        return rounds.map { round ->
            StandaloneRound(
                roundId = round.id,
                playedAt = round.playedAt,
                locationId = round.locationId,
                locationName = round.locationId?.let { locationNames[it] },
                pars = holesByRound[round.id].orEmpty().sortedBy { it.holeNumber }.map { it.par },
                players = scoresByRound[round.id].orEmpty()
                    .groupBy { it.playerId }
                    .map { (playerId, playerScores) ->
                        StandaloneRoundPlayer(
                            playerId = playerId,
                            name = playerNames[playerId] ?: "Ukjent spiller",
                            totalStrokes = playerScores.sumOf { it.throws },
                            holesPlayed = playerScores.size,
                        )
                    }
                    .sortedBy { it.totalStrokes },
            )
        }
    }
}
