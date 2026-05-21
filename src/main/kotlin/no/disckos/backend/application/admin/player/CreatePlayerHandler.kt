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
                catchphrase = cmd.catchphrase
                createdAt = OffsetDateTime.now()
            }
        )
    }
}
