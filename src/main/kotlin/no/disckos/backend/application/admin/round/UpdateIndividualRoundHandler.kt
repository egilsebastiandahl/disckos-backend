package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateIndividualRoundHandler(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional
    fun handle(id: UUID, cmd: UpdateIndividualRoundInput): RoundDetails {
        val round = roundRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found") }
        if (round.eventType != EventType.individual) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Round is not an individual round")
        }

        val existingHoles = holeRepository.findByRoundId(id)
        val holeIds = existingHoles.map { it.id }
        if (holeIds.isNotEmpty()) {
            playerScoreRepository.deleteByHoleIdIn(holeIds)
            holeRepository.deleteByRoundId(id)
        }

        round.scoringFormat = cmd.scoringFormat
        roundRepository.save(round)

        val holes = cmd.holes.map { hole ->
            HoleEntity(
                id = UUID.randomUUID(),
                roundId = id,
                holeNumber = hole.holeNumber,
                par = hole.par,
                scoringFormatOverride = hole.scoringFormatOverride
            ) to hole
        }
        holeRepository.saveAll(holes.map { it.first })

        val playerScores = mutableListOf<PlayerScoreEntity>()
        holes.forEach { (holeEntity, holeInput) ->
            holeInput.playerScores.forEach { score ->
                playerScores.add(
                    PlayerScoreEntity(
                        id = UUID.randomUUID(),
                        holeId = holeEntity.id,
                        playerId = score.playerId,
                        throws = score.throws
                    )
                )
            }
        }
        playerScoreRepository.saveAll(playerScores)

        return RoundDetails(
            id = id,
            eventId = round.eventId,
            eventType = round.eventType,
            scoringFormat = round.scoringFormat,
            holes = holes.map { (holeEntity, holeInput) ->
                HoleDetails(
                    holeNumber = holeEntity.holeNumber,
                    par = holeEntity.par,
                    scoringFormatOverride = holeEntity.scoringFormatOverride,
                    playerScores = holeInput.playerScores.map { score ->
                        PlayerScoreDetails(
                            playerId = score.playerId,
                            throws = score.throws
                        )
                    },
                    teamScores = null
                )
            }
        )
    }
}
