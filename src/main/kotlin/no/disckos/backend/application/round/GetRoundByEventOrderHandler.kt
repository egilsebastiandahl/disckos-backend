package no.disckos.backend.application.round

import no.disckos.backend.application.admin.round.RoundDetails
import no.disckos.backend.application.admin.round.RoundDetailsLoader
import no.disckos.backend.repository.RoundRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class GetRoundByEventOrderHandler(
    private val roundRepository: RoundRepository,
    private val roundDetailsLoader: RoundDetailsLoader
) {
    fun handle(eventId: UUID, roundNumber: Int): RoundDetails {
        if (roundNumber < 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Round number must be >= 1")
        }
        val rounds = roundRepository.findByEventIdOrderByCreatedAtAsc(eventId)
        if (rounds.isEmpty() || roundNumber > rounds.size) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found")
        }
        val round = rounds[roundNumber - 1]
        return roundDetailsLoader.load(listOf(round)).first()
    }
}
