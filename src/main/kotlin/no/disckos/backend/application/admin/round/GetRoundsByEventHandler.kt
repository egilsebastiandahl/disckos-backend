package no.disckos.backend.application.admin.round

import no.disckos.backend.repository.RoundRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetRoundsByEventHandler(
    private val roundRepository: RoundRepository,
    private val roundDetailsLoader: RoundDetailsLoader
) {
    fun handle(eventId: UUID): List<RoundDetails> {
        val rounds = roundRepository.findByEventId(eventId)
        return roundDetailsLoader.load(rounds)
    }
}
