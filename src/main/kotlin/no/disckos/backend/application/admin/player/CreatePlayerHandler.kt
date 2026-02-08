package no.disckos.backend.application.admin.player

import no.disckos.backend.domain.Player
import no.disckos.backend.repository.PlayerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreatePlayerHandler(
    private val playerRepository: PlayerRepository
) {
    @Transactional
    fun handle(cmd: CreatePlayerInput): Player =
        playerRepository.save(
            Player().apply {
                name = cmd.name.trim()
                gender = cmd.gender
                roundsPlayed = cmd.roundsPlayed
                averageScore = cmd.averageScore
                bestScore = cmd.bestScore
                worstScore = cmd.worstScore
                singleBogeyCount = cmd.singleBogeyCount
                doubleBogeyCount = cmd.doubleBogeyCount
                tripleBogeyCount = cmd.tripleBogeyCount
                parCount = cmd.parCount
                birdieCount = cmd.birdieCount
                aceCount = cmd.aceCount
                eagleCount = cmd.eagleCount
                throwsField = cmd.throws
                catchphrase = cmd.catchphrase
            }
        )
}
