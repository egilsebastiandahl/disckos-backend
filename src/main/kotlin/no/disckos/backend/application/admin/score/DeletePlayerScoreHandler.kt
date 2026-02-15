package no.disckos.backend.application.admin.score

import no.disckos.backend.repository.PlayerScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeletePlayerScoreHandler(
    private val playerScoreRepository: PlayerScoreRepository
) {
    @Transactional
    fun handle(id: UUID) {
        val score = playerScoreRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Player score not found") }
        playerScoreRepository.delete(score)
    }
}
