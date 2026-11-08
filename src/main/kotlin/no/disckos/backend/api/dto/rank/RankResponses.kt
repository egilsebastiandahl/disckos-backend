package no.disckos.backend.api.dto.rank

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class RankTierResponse(
    val index: Int,
    val name: String,
    /** Stable key for the badge's look, e.g. "chain-master". */
    val group: String,
    /** 1-based step within the group (Chain Master II = 2)... */
    val level: Int,
    /** ...out of this many (Chain Master has 3). */
    val levelsInGroup: Int,
    /** Average strokes to par per 18 holes needed for this tier, before course adjustment. Null for the bottom tier. */
    val maxToParPer18: Double?
)

data class RankLadderResponse(
    val placementRounds: Int,
    val windowRounds: Int,
    val countedRounds: Int,
    val tiers: List<RankTierResponse>
)

data class RankChangeResponse(
    /** Null when the change was the placement. */
    val fromTier: RankTierResponse?,
    val toTier: RankTierResponse,
    val up: Boolean,
    val playedAt: OffsetDateTime
)

data class CourseRankResponse(
    val locationId: UUID,
    val locationName: String,
    val roundsPlayed: Int,
    /** 0 once placed. */
    val roundsToPlacement: Int,
    /** Null until placed. */
    val tier: RankTierResponse?,
    /** Average to par of the counted rounds, in the course's own terms. */
    val rating: BigDecimal?,
    val countedRounds: Int,
    val nextTier: RankTierResponse?,
    /** Average to par, in the course's own terms, that reaches [nextTier]. */
    val nextTierTarget: BigDecimal?,
    /** 0..1 towards [nextTier]. */
    val progress: Double?,
    val lastPlayedAt: OffsetDateTime,
    val lastChange: RankChangeResponse?
)

data class PlayerRanksResponse(
    val playerId: UUID,
    val courses: List<CourseRankResponse>
)

data class LeaderboardEntryResponse(
    val playerId: UUID,
    val playerName: String,
    val rank: CourseRankResponse
)

data class CourseLeaderboardResponse(
    val locationId: UUID,
    val locationName: String,
    /** The course's difficulty adjustment, strokes per 18 holes. */
    val rankOffset: Double,
    val holes: Int,
    val entries: List<LeaderboardEntryResponse>
)
