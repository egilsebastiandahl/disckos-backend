package no.disckos.backend.api

import no.disckos.backend.api.dto.rating.CourseDifficultyResponse
import no.disckos.backend.api.dto.rating.PlayerRatingResponse
import no.disckos.backend.application.rating.GetCourseDifficultiesHandler
import no.disckos.backend.application.rating.GetRatingsHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Disckos Rating. A single player's rating, history and round ratings live under GET /api/player/{id}/rating. */
@RestController
@RequestMapping("/api/rating")
class RatingController(
    private val getRatingsHandler: GetRatingsHandler,
    private val getCourseDifficultiesHandler: GetCourseDifficultiesHandler
) {
    /** Every player with a complete round, in standings order. */
    @GetMapping
    fun getRatings(): List<PlayerRatingResponse> =
        getRatingsHandler.handle().map { it.toResponse() }

    /** How hard each course plays according to the model, with a suggested rank_offset. */
    @GetMapping("/courses")
    fun getCourseDifficulties(): List<CourseDifficultyResponse> =
        getCourseDifficultiesHandler.handle().map { it.toResponse() }
}
