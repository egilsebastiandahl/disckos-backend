package no.disckos.backend.application.rating

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.application.rank.CompletedRoundLoader
import no.disckos.backend.application.rank.PlayerCourseRound
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.repository.LocationRepository
import org.springframework.stereotype.Component
import java.util.UUID

/** Everything a rating fit needs, read once per request. */
class RatingInput(
    /** Every complete individual round, the source of [rounds]. */
    val completed: List<PlayerCourseRound>,
    val rounds: List<RatingRound>,
    val locations: Map<UUID, LocationEntity>
) {
    /** A location's rank_offset is the starting guess for its difficulty (easy course = positive offset). */
    val locationPriors: Map<UUID, Double> get() = locations.mapValues { (_, l) -> -l.rankOffset }

    fun fit(): RatingFit = RatingModel.fit(rounds, locationPriors)
}

/**
 * Loads the rounds the rating is built from: the same complete individual stroke-play rounds as
 * course ranks. Unlike ranks, every layout counts: the model learns each layout's difficulty on
 * its own, so a short or odd layout can't skew anyone's rating.
 */
@Component
class RatingInputLoader(
    private val completedRoundLoader: CompletedRoundLoader,
    private val locationRepository: LocationRepository
) {
    fun load(): RatingInput {
        val completed = completedRoundLoader.forEveryone()
        return RatingInput(
            completed = completed,
            rounds = completed.map { it.toRatingRound() },
            locations = locationRepository.findAll().associateBy { it.id }
        )
    }
}

fun PlayerCourseRound.toRatingRound(): RatingRound =
    RatingRound(
        playerId = playerId,
        roundId = round.roundId,
        course = RatingCourse(locationId, layoutName?.let(ImportNames::key)),
        playedAt = round.playedAt,
        toPar = round.toPar,
        holes = round.holes
    )
