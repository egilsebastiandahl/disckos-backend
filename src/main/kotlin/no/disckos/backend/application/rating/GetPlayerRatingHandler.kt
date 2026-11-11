package no.disckos.backend.application.rating

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class PlayerRatingDetail(
    /** Null when the player has no complete round. */
    val rating: PlayerRating?,
    /** Active rated players, i.e. how many positions there are. */
    val ratedPlayers: Int,
    /** Oldest first. */
    val history: List<RatingPoint>,
    /** What each complete round was worth, by round id. */
    val roundRatings: Map<UUID, Double>
)

@Component
class GetPlayerRatingHandler(private val ratingInputLoader: RatingInputLoader) {

    @Transactional(readOnly = true)
    fun handle(playerId: UUID): PlayerRatingDetail {
        val input = ratingInputLoader.load()
        if (input.rounds.none { it.playerId == playerId }) return PlayerRatingDetail(null, 0, emptyList(), emptyMap())

        val fit = input.fit()
        val standings = PlayerRatings.standings(input.rounds, fit, OffsetDateTime.now())
        return PlayerRatingDetail(
            rating = standings.single { it.playerId == playerId },
            ratedPlayers = standings.count { it.position != null },
            history = PlayerRatings.history(playerId, input.rounds, input.locationPriors, fit),
            roundRatings = PlayerRatings.roundRatings(playerId, input.rounds, fit)
        )
    }
}
