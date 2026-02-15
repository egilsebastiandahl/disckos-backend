package no.disckos.backend.application.round

import no.disckos.backend.application.admin.round.RoundDetails
import no.disckos.backend.application.admin.round.RoundDetailsLoader
import no.disckos.backend.repository.RoundRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetRoundsByEventOrderHandler(
    private val roundRepository: RoundRepository,
    private val roundDetailsLoader: RoundDetailsLoader
) {
    fun handle(eventId: UUID): List<RoundDetails> {
        val rounds = roundRepository.findByEventIdOrderByCreatedAtAsc(eventId)
        return roundDetailsLoader.load(rounds)
    }
}
