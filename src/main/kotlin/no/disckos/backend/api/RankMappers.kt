package no.disckos.backend.api

import no.disckos.backend.api.dto.rank.CourseLeaderboardResponse
import no.disckos.backend.api.dto.rank.CourseRankResponse
import no.disckos.backend.api.dto.rank.LeaderboardEntryResponse
import no.disckos.backend.api.dto.rank.RankChangeResponse
import no.disckos.backend.api.dto.rank.RankLadderResponse
import no.disckos.backend.api.dto.rank.RankTierResponse
import no.disckos.backend.application.rank.CourseLeaderboard
import no.disckos.backend.application.rank.CourseRank
import no.disckos.backend.application.rank.RankChange
import no.disckos.backend.application.rank.RankLadder
import no.disckos.backend.application.rank.RankTier
import no.disckos.backend.domain.LocationEntity
import java.math.BigDecimal
import java.math.RoundingMode

fun rankLadderResponse(): RankLadderResponse =
    RankLadderResponse(
        placementRounds = RankLadder.PLACEMENT_ROUNDS,
        windowRounds = RankLadder.WINDOW_ROUNDS,
        countedRounds = RankLadder.COUNTED_ROUNDS,
        inactiveAfterDays = RankLadder.INACTIVE_AFTER_DAYS,
        tiers = RankLadder.tiers.map { it.toResponse() }
    )

fun RankTier.toResponse(): RankTierResponse {
    val sameGroup = RankLadder.tiers.filter { it.group == group }
    return RankTierResponse(
        index = index,
        name = name,
        group = group,
        level = sameGroup.count { it.index <= index },
        levelsInGroup = sameGroup.size,
        maxToParPer18 = maxToParPer18
    )
}

fun RankChange.toResponse(): RankChangeResponse =
    RankChangeResponse(fromTier = from?.toResponse(), toTier = to.toResponse(), up = up, playedAt = playedAt)

fun CourseRank.toResponse(location: LocationEntity): CourseRankResponse =
    CourseRankResponse(
        locationId = location.id,
        locationName = location.name,
        roundsPlayed = roundsPlayed,
        roundsToPlacement = roundsToPlacement,
        tier = tier?.toResponse(),
        rating = rating?.oneDecimal(),
        countedRounds = countedRounds,
        nextTier = nextTier?.toResponse(),
        nextTierTarget = nextTierTarget?.oneDecimal(),
        progress = progress,
        lastPlayedAt = lastPlayedAt,
        inactive = inactive,
        lastChange = lastChange?.toResponse()
    )

fun CourseLeaderboard.toResponse(): CourseLeaderboardResponse =
    CourseLeaderboardResponse(
        locationId = location.id,
        locationName = location.name,
        rankOffset = location.rankOffset,
        holes = holes,
        entries = entries.map {
            LeaderboardEntryResponse(
                playerId = it.player.id!!,
                playerName = it.player.name ?: "Ukjent",
                rank = it.rank.toResponse(location)
            )
        }
    )

private fun Double.oneDecimal(): BigDecimal =
    BigDecimal(this).setScale(1, RoundingMode.HALF_UP)
