package no.disckos.backend.application.admin.player

import no.disckos.backend.repository.PlayerRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class DeletePlayerHandler(
    private val playerRepository: PlayerRepository
) {
    @Transactional
    fun handle(id: UUID) {
        if (!playerRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found")
        }
        playerRepository.deleteById(id)
    }
}
