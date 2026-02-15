package no.disckos.backend.application.admin.score

import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdatePlayerScoreHandler(
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional
    fun handle(id: UUID, throws: Int): PlayerScoreEntity {
        val score = playerScoreRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Player score not found") }
        score.throws = throws
        return playerScoreRepository.save(score)
    }
}
