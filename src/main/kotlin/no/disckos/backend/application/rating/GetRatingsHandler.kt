package no.disckos.backend.application.rating

import no.disckos.backend.domain.Player
import no.disckos.backend.repository.PlayerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

data class RatingEntry(val player: Player, val rating: PlayerRating)

/** The club-wide Disckos Rating list, in standings order (see [PlayerRatings.standings]). */
@Component
class GetRatingsHandler(
    private val ratingInputLoader: RatingInputLoader,
    private val playerRepository: PlayerRepository
) {
    @Transactional(readOnly = true)
    fun handle(): List<RatingEntry> {
        val input = ratingInputLoader.load()
        if (input.rounds.isEmpty()) return emptyList()

        val standings = PlayerRatings.standings(input.rounds, input.fit(), OffsetDateTime.now())
        val players = playerRepository.findAllById(standings.map { it.playerId }).associateBy { it.id }
        return standings.mapNotNull { r -> players[r.playerId]?.let { RatingEntry(it, r) } }
    }
}
