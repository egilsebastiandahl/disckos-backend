package no.disckos.backend.application.rating

import no.disckos.backend.application.rank.RankLayoutFilter
import no.disckos.backend.domain.LocationEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

data class CourseDifficulty(
    val location: LocationEntity,
    /** Complete rounds there that count for ranks. */
    val rounds: Int,
    /** Fitted strokes per 18 over a par-fair course, averaged over those rounds' layouts. */
    val difficulty: Double
) {
    /** The rank_offset that makes this course par-fair for ranks, to the nearest half stroke. */
    val suggestedRankOffset: Double get() = (-difficulty * 2).roundToInt() / 2.0
}

/**
 * How hard each course plays according to the rating model, for setting rank_offset by data
 * instead of by feel. Only rounds that count for ranks are averaged, since that's what the
 * offset is applied to.
 */
@Component
class GetCourseDifficultiesHandler(
    private val ratingInputLoader: RatingInputLoader,
    private val rankLayoutFilter: RankLayoutFilter
) {
    @Transactional(readOnly = true)
    fun handle(): List<CourseDifficulty> {
        val input = ratingInputLoader.load()
        if (input.rounds.isEmpty()) return emptyList()
        val fit = input.fit()

        return rankLayoutFilter.ranked(input.completed)
            .map { it.toRatingRound() }
            .groupBy { it.course.locationId }
            .mapNotNull { (locationId, rounds) ->
                val location = input.locations[locationId] ?: return@mapNotNull null
                CourseDifficulty(location, rounds.size, rounds.map { fit.difficulty.getValue(it.course) }.average())
            }
            .sortedWith(compareByDescending<CourseDifficulty> { it.rounds }.thenBy { it.location.name })
    }
}
