package no.disckos.backend.api

import no.disckos.backend.api.dto.rating.CourseDifficultyResponse
import no.disckos.backend.api.dto.rating.PlayerRatingDetailResponse
import no.disckos.backend.api.dto.rating.PlayerRatingResponse
import no.disckos.backend.api.dto.rating.RatingPointResponse
import no.disckos.backend.api.dto.rating.RoundRatingResponse
import no.disckos.backend.application.rating.CourseDifficulty
import no.disckos.backend.application.rating.PlayerRatingDetail
import no.disckos.backend.application.rating.RatingEntry
import no.disckos.backend.application.rating.RatingRules
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.roundToInt

fun RatingEntry.toResponse(): PlayerRatingResponse =
    PlayerRatingResponse(
        playerId = player.id!!,
        playerName = player.name ?: "Ukjent",
        rating = rating.rating?.roundToInt(),
        roundsPlayed = rating.roundsPlayed,
        roundsToRating = rating.roundsToRating,
        provisional = rating.provisional,
        active = rating.active,
        position = rating.position,
        lastPlayedAt = rating.lastPlayedAt
    )

fun PlayerRatingDetail.toResponse(playerId: UUID): PlayerRatingDetailResponse {
    val points = history.map { RatingPointResponse(it.roundId, it.playedAt, it.rating.roundToInt()) }
    return PlayerRatingDetailResponse(
        playerId = playerId,
        rating = rating?.rating?.roundToInt(),
        roundsPlayed = rating?.roundsPlayed ?: 0,
        roundsToRating = rating?.roundsToRating ?: RatingRules.RATED_AFTER_ROUNDS,
        provisional = rating?.provisional ?: false,
        active = rating?.active ?: false,
        position = rating?.position,
        ratedPlayers = ratedPlayers,
        lastChange = if (points.size >= 2) points.last().rating - points[points.size - 2].rating else null,
        history = points,
        rounds = roundRatings.map { (roundId, value) -> RoundRatingResponse(roundId, value.roundToInt()) }
    )
}

fun CourseDifficulty.toResponse(): CourseDifficultyResponse =
    CourseDifficultyResponse(
        locationId = location.id,
        locationName = location.name,
        rounds = rounds,
        difficulty = BigDecimal(difficulty).setScale(1, RoundingMode.HALF_UP),
        suggestedRankOffset = suggestedRankOffset,
        rankOffset = location.rankOffset
    )
