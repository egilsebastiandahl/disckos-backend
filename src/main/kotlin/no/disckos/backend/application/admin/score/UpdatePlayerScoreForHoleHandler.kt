package no.disckos.backend.application.admin.score

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdatePlayerScoreForHoleHandler(
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional
    fun handle(roundId: UUID, holeNumber: Int, playerId: UUID, throws: Int) =
        holeRepository.findByRoundIdAndHoleNumber(roundId, holeNumber)
            ?.let { hole ->
                val score = playerScoreRepository.findByHoleIdAndPlayerId(hole.id, playerId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Player score not found")
                score.throws = throws
                playerScoreRepository.save(score)
            }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Hole not found")
}
