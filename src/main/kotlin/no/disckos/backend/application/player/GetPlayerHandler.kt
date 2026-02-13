package no.disckos.backend.application.player

import no.disckos.backend.domain.Player
import no.disckos.backend.repository.PlayerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class GetPlayerHandler(
    private val playerRepository: PlayerRepository
) {
    @Transactional(readOnly = true)
    fun handle(id: UUID): Player? {
        return playerRepository.findById(id).orElse(null)
    }
}