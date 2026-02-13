package no.disckos.backend.application.admin.player

import no.disckos.backend.domain.Player
import no.disckos.backend.repository.PlayerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Component
class CreatePlayerHandler(
    private val playerRepository: PlayerRepository
) {
    @Transactional
    fun handle(cmd: CreatePlayerInput): Player {

        return playerRepository.save(
            Player().apply {
                id = UUID.randomUUID()
                name = cmd.name.trim()
                gender = cmd.gender
                roundsPlayed = cmd.roundsPlayed ?: 0
                averageScore = cmd.averageScore
                bestScore = cmd.bestScore
                worstScore = cmd.worstScore
                singleBogeyCount = cmd.singleBogeyCount ?: 0
                doubleBogeyCount = cmd.doubleBogeyCount ?: 0
                tripleBogeyCount = cmd.tripleBogeyCount ?: 0
                parCount = cmd.parCount ?: 0
                birdieCount = cmd.birdieCount ?: 0
                aceCount = cmd.aceCount ?: 0
                eagleCount = cmd.eagleCount ?: 0
                throwsField = cmd.throws
                catchphrase = cmd.catchphrase
                createdAt = OffsetDateTime.now()
            }
        )
    }




}
