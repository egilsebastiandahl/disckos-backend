package no.disckos.backend.application.admin.player

import no.disckos.backend.domain.Player
import no.disckos.backend.repository.PlayerRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Component
class UpdatePlayerHandler(
    private val playerRepository: PlayerRepository
) {
    @Transactional
    fun handle(cmd: UpdatePlayerInput): Player {
        val player = playerRepository.findById(cmd.id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found") }

        cmd.name?.let { player.name = it.trim() }
        cmd.gender?.let { player.gender = it }
        cmd.catchphrase?.let { player.catchphrase = it }

        return playerRepository.save(player)
    }
}
