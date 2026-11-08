package no.disckos.backend.application.rank

import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

data class LeaderboardEntry(val player: Player, val rank: CourseRank)

data class CourseLeaderboard(
    val location: LocationEntity,
    /** Holes in the course's most recent complete round. */
    val holes: Int,
    val entries: List<LeaderboardEntry>
)

/** Every course that has at least one complete round, with every player's rank on it. */
@Component
class GetCourseLeaderboardsHandler(
    private val completedRoundLoader: CompletedRoundLoader,
    private val locationRepository: LocationRepository,
    private val playerRepository: PlayerRepository
) {
    @Transactional(readOnly = true)
    fun handle(): List<CourseLeaderboard> {
        val rounds = completedRoundLoader.forEveryone()
        if (rounds.isEmpty()) return emptyList()

        val locations = locationRepository.findAllById(rounds.map { it.locationId }.toSet()).associateBy { it.id }
        val players = playerRepository.findAllById(rounds.map { it.playerId }.toSet()).associateBy { it.id }
        val now = OffsetDateTime.now()

        return rounds.groupBy { it.locationId }
            .mapNotNull { (locationId, courseRounds) ->
                val location = locations[locationId] ?: return@mapNotNull null
                val entries = courseRounds.groupBy { it.playerId }
                    .mapNotNull { (playerId, playerRounds) ->
                        val player = players[playerId] ?: return@mapNotNull null
                        LeaderboardEntry(
                            player = player,
                            rank = CourseRankCalculator.calculate(
                                playerRounds.map { it.round }, location.rankOffset, now
                            )
                        )
                    }
                    .sortedWith(compareBy(CourseRankOrder) { it.rank })
                CourseLeaderboard(
                    location = location,
                    holes = courseRounds.maxBy { it.round.playedAt }.round.holes,
                    entries = entries
                )
            }
            .sortedWith(compareBy({ -it.entries.count { e -> e.rank.tier != null } }, { -it.entries.size }, { it.location.name }))
    }
}
