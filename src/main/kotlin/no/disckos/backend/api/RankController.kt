package no.disckos.backend.api

import no.disckos.backend.api.dto.rank.CourseLeaderboardResponse
import no.disckos.backend.api.dto.rank.RankLadderResponse
import no.disckos.backend.application.rank.GetCourseLeaderboardsHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Course ranks. A single player's ranks live under GET /api/player/{id}/ranks. */
@RestController
@RequestMapping("/api/rank")
class RankController(
    private val getCourseLeaderboardsHandler: GetCourseLeaderboardsHandler
) {
    @GetMapping("/ladder")
    fun getLadder(): RankLadderResponse = rankLadderResponse()

    @GetMapping("/courses")
    fun getCourseLeaderboards(): List<CourseLeaderboardResponse> =
        getCourseLeaderboardsHandler.handle().map { it.toResponse() }
}
