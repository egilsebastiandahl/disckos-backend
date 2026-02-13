package no.disckos.backend.application.player

import no.disckos.backend.domain.Player
import no.disckos.backend.repository.PlayerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetPlayersHandler(
    private val playerRepository: PlayerRepository
) {
    @Transactional(readOnly = true)
    fun handle(): List<Player> {
        return playerRepository.findAll()
    }
}