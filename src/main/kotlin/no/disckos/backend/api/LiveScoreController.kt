package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.round.UpdatePlayerScoreForHoleRequest
import no.disckos.backend.api.dto.admin.round.UpdateTeamMemberScoreForHoleRequest
import no.disckos.backend.api.dto.admin.round.UpdateTeamScoreForHoleRequest
import no.disckos.backend.api.dto.round.RoundResponse
import no.disckos.backend.application.admin.round.GetRoundHandler
import no.disckos.backend.application.admin.score.UpdatePlayerScoreForHoleHandler
import no.disckos.backend.application.admin.score.UpdateTeamMemberScoreForHoleHandler
import no.disckos.backend.application.admin.score.UpdateTeamScoreForHoleHandler
import no.disckos.backend.application.live.ParticipationGate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/round")
@PreAuthorize("isAuthenticated()")
class LiveScoreController(
    private val participationGate: ParticipationGate,
    private val updatePlayerScoreForHoleHandler: UpdatePlayerScoreForHoleHandler,
    private val updateTeamScoreForHoleHandler: UpdateTeamScoreForHoleHandler,
    private val updateTeamMemberScoreForHoleHandler: UpdateTeamMemberScoreForHoleHandler,
    private val getRoundHandler: GetRoundHandler,
) {

    @PatchMapping("/{id}/hole/{holeNumber}/player-score/{playerId}")
    fun updatePlayerScoreForHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @PathVariable playerId: UUID,
        @Valid @RequestBody request: UpdatePlayerScoreForHoleRequest
    ): RoundResponse {
        participationGate.verifyCanScore(id)
        updatePlayerScoreForHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            playerId = playerId,
            throws = request.throws
        )
        return getRoundHandler.handle(id).toResponse()
    }

    @PatchMapping("/{id}/hole/{holeNumber}/team-score/{teamId}")
    fun updateTeamScoreForHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @PathVariable teamId: UUID,
        @Valid @RequestBody request: UpdateTeamScoreForHoleRequest
    ): RoundResponse {
        participationGate.verifyCanScore(id)
        updateTeamScoreForHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            teamId = teamId,
            teamThrows = request.teamThrows
        )
        return getRoundHandler.handle(id).toResponse()
    }

    @PatchMapping("/{id}/hole/{holeNumber}/team-score/{teamId}/member/{playerId}")
    fun updateTeamMemberScoreForHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @PathVariable teamId: UUID,
        @PathVariable playerId: UUID,
        @Valid @RequestBody request: UpdateTeamMemberScoreForHoleRequest
    ): RoundResponse {
        participationGate.verifyCanScore(id)
        updateTeamMemberScoreForHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            teamId = teamId,
            playerId = playerId,
            throws = request.throws,
            isCounted = request.isCounted
        )
        return getRoundHandler.handle(id).toResponse()
    }
}
