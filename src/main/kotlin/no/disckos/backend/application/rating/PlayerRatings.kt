package no.disckos.backend.application.rating

import java.time.OffsetDateTime
import java.util.UUID

/** One player's Disckos Rating. */
data class PlayerRating(
    val playerId: UUID,
    /** Complete rounds behind the rating. */
    val roundsPlayed: Int,
    /** Null until [RatingRules.RATED_AFTER_ROUNDS] complete rounds. */
    val rating: Double?,
    /** Rated, but on fewer than [RatingRules.PROVISIONAL_UNTIL_ROUNDS] rounds. */
    val provisional: Boolean,
    /** Played within [RatingRules.INACTIVE_AFTER_DAYS]. */
    val active: Boolean,
    val lastPlayedAt: OffsetDateTime,
    /** 1-based place among active rated players; null for everyone else. */
    val position: Int?
) {
    val roundsToRating: Int get() = maxOf(0, RatingRules.RATED_AFTER_ROUNDS - roundsPlayed)
}

/** The player's rating right after one of their rounds. */
data class RatingPoint(val roundId: UUID, val playedAt: OffsetDateTime, val rating: Double)

/** Turns a [RatingFit] into what players see. Pure, like the model. */
object PlayerRatings {

    /**
     * Everyone with a complete round: active rated players by rating (these get a position),
     * then inactive rated players by rating, then players still earning a rating, most rounds first.
     */
    fun standings(rounds: List<RatingRound>, fit: RatingFit, now: OffsetDateTime): List<PlayerRating> {
        val inactiveBefore = now.minusDays(RatingRules.INACTIVE_AFTER_DAYS)
        val unplaced = rounds.groupBy { it.playerId }.map { (playerId, mine) ->
            val rated = mine.size >= RatingRules.RATED_AFTER_ROUNDS
            val lastPlayedAt = mine.maxOf { it.playedAt }
            PlayerRating(
                playerId = playerId,
                roundsPlayed = mine.size,
                rating = if (rated) RatingRules.rating(fit.skill.getValue(playerId)) else null,
                provisional = rated && mine.size < RatingRules.PROVISIONAL_UNTIL_ROUNDS,
                active = lastPlayedAt.isAfter(inactiveBefore),
                lastPlayedAt = lastPlayedAt,
                position = null
            )
        }
        val (rated, unrated) = unplaced.partition { it.rating != null }
        val (active, inactive) = rated.sortedByDescending { it.rating }.partition { it.active }
        return active.mapIndexed { i, r -> r.copy(position = i + 1) } +
            inactive +
            unrated.sortedWith(compareByDescending<PlayerRating> { it.roundsPlayed }.thenByDescending { it.lastPlayedAt })
    }

    /**
     * The player's rating after each of their rounds, from the round that earned the rating.
     * Each point is refitted with only the rounds played up to then (everyone's), so it shows what
     * the rating was at the time. The last point is the current rating, so the line ends where
     * the number on the page is.
     */
    fun history(
        playerId: UUID,
        rounds: List<RatingRound>,
        locationPriors: Map<UUID, Double>,
        current: RatingFit
    ): List<RatingPoint> {
        val mine = rounds.filter { it.playerId == playerId }.sortedWith(compareBy({ it.playedAt }, { it.roundId }))
        return mine.mapIndexedNotNull { i, round ->
            if (i + 1 < RatingRules.RATED_AFTER_ROUNDS) return@mapIndexedNotNull null
            val fit = if (i == mine.lastIndex) current
            else RatingModel.fit(rounds.filter { !it.playedAt.isAfter(round.playedAt) }, locationPriors)
            val skill = fit.skill[playerId] ?: return@mapIndexedNotNull null
            RatingPoint(round.roundId, round.playedAt, RatingRules.rating(skill))
        }
    }

    /** What each of the player's complete rounds was worth on the rating scale, by round id. */
    fun roundRatings(playerId: UUID, rounds: List<RatingRound>, fit: RatingFit): Map<UUID, Double> =
        rounds.filter { it.playerId == playerId }
            .associate { it.roundId to RatingRules.rating(fit.adjustedToParPer18(it)) }
}
