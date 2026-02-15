package no.disckos.backend.application.admin.round

import no.disckos.backend.repository.RoundRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class GetRoundHandler(
    private val roundRepository: RoundRepository,
    private val roundDetailsLoader: RoundDetailsLoader
) {
    fun handle(id: UUID): RoundDetails {
        val round = roundRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found") }
        return roundDetailsLoader.load(listOf(round)).first()
    }
}
