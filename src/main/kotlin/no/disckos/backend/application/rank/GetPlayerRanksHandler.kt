package no.disckos.backend.application.rank

import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.repository.LocationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PlayerCourseRank(val location: LocationEntity, val rank: CourseRank)

@Component
class GetPlayerRanksHandler(
    private val completedRoundLoader: CompletedRoundLoader,
    private val rankLayoutFilter: RankLayoutFilter,
    private val locationRepository: LocationRepository
) {
    @Transactional(readOnly = true)
    fun handle(playerId: UUID): List<PlayerCourseRank> {
        val byLocation = rankLayoutFilter.ranked(completedRoundLoader.forPlayer(playerId)).groupBy { it.locationId }
        if (byLocation.isEmpty()) return emptyList()

        val locations = locationRepository.findAllById(byLocation.keys).associateBy { it.id }

        return byLocation.mapNotNull { (locationId, rounds) ->
            val location = locations[locationId] ?: return@mapNotNull null
            PlayerCourseRank(
                location = location,
                rank = CourseRankCalculator.calculate(rounds.map { it.round }, location.rankOffset)
            )
        }.sortedWith(compareBy(CourseRankOrder) { it.rank })
    }
}

/** Ranked first (highest tier, then best average), then unranked (most rounds first). */
object CourseRankOrder : Comparator<CourseRank> by compareBy<CourseRank>(
    { if (it.tier == null) 1 else 0 },
    { -(it.tier?.index ?: 0) },
    { it.rating ?: Double.MAX_VALUE },
    { -it.roundsPlayed }
)
