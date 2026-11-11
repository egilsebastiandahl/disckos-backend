package no.disckos.backend.api.dto.rating

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PlayerRatingResponse(
    val playerId: UUID,
    val playerName: String,
    /** Disckos Rating, rounded. Null until the player has [roundsToRating] more complete rounds. */
    val rating: Int?,
    val roundsPlayed: Int,
    /** 0 once rated. */
    val roundsToRating: Int,
    val provisional: Boolean,
    /** Played within the last year. Inactive players keep their rating but get no position. */
    val active: Boolean,
    /** 1-based place among active rated players. */
    val position: Int?,
    val lastPlayedAt: OffsetDateTime
)

data class RatingPointResponse(val roundId: UUID, val playedAt: OffsetDateTime, val rating: Int)

data class RoundRatingResponse(val roundId: UUID, val rating: Int)

data class PlayerRatingDetailResponse(
    val playerId: UUID,
    /** Null when the player has no complete round. */
    val rating: Int?,
    val roundsPlayed: Int,
    val roundsToRating: Int,
    val provisional: Boolean,
    val active: Boolean,
    val position: Int?,
    /** How many active rated players there are. */
    val ratedPlayers: Int,
    /** Change from the rating after the previous round. Null with fewer than two rated rounds. */
    val lastChange: Int?,
    /** Oldest first, from the round that earned the rating. */
    val history: List<RatingPointResponse>,
    /** What each complete round was worth, in no particular order. */
    val rounds: List<RoundRatingResponse>
)

data class CourseDifficultyResponse(
    val locationId: UUID,
    val locationName: String,
    /** Complete rounds there that count for ranks. */
    val rounds: Int,
    /** Strokes per 18 holes the course plays over a par-fair course. Negative = soft par. */
    val difficulty: BigDecimal,
    /** rank_offset that would make par fair for ranks, to the nearest half stroke. */
    val suggestedRankOffset: Double,
    val rankOffset: Double
)
