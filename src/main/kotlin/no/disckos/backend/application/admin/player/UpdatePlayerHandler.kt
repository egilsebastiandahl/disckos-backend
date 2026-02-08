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
        cmd.roundsPlayed?.let { player.roundsPlayed = it }
        cmd.averageScore?.let { player.averageScore = it }
        cmd.bestScore?.let { player.bestScore = it }
        cmd.worstScore?.let { player.worstScore = it }
        cmd.singleBogeyCount?.let { player.singleBogeyCount = it }
        cmd.doubleBogeyCount?.let { player.doubleBogeyCount = it }
        cmd.tripleBogeyCount?.let { player.tripleBogeyCount = it }
        cmd.parCount?.let { player.parCount = it }
        cmd.birdieCount?.let { player.birdieCount = it }
        cmd.aceCount?.let { player.aceCount = it }
        cmd.eagleCount?.let { player.eagleCount = it }
        cmd.throws?.let { player.throwsField = it }
        cmd.catchphrase?.let { player.catchphrase = it }

        return playerRepository.save(player)
    }
}
