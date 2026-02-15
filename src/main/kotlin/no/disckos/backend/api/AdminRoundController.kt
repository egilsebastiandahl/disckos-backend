package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.round.AdminRoundResponse
import no.disckos.backend.api.dto.admin.round.CreateIndividualRoundRequest
import no.disckos.backend.api.dto.admin.round.CreateTeamRoundRequest
import no.disckos.backend.api.dto.admin.round.UpdateHoleRequest
import no.disckos.backend.api.dto.admin.round.UpdateIndividualRoundRequest
import no.disckos.backend.api.dto.admin.round.UpdatePlayerScoreForHoleRequest
import no.disckos.backend.api.dto.admin.round.UpdateTeamRoundRequest
import no.disckos.backend.api.dto.admin.round.UpdateTeamMemberScoreForHoleRequest
import no.disckos.backend.api.dto.admin.round.UpdateTeamScoreForHoleRequest
import no.disckos.backend.application.admin.round.CreateIndividualRoundHandler
import no.disckos.backend.application.admin.round.CreateTeamRoundHandler
import no.disckos.backend.application.admin.round.DeleteRoundHandler
import no.disckos.backend.application.admin.round.GetRoundHandler
import no.disckos.backend.application.admin.round.GetRoundsByEventHandler
import no.disckos.backend.application.admin.round.UpdateHoleHandler
import no.disckos.backend.application.admin.round.UpdateIndividualRoundHandler
import no.disckos.backend.application.admin.round.UpdateTeamRoundHandler
import no.disckos.backend.application.admin.score.UpdatePlayerScoreForHoleHandler
import no.disckos.backend.application.admin.score.UpdateTeamMemberScoreForHoleHandler
import no.disckos.backend.application.admin.score.UpdateTeamScoreForHoleHandler
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin/round")
@PreAuthorize("hasRole('admin')")
class AdminRoundController(
    private val createTeamRoundHandler: CreateTeamRoundHandler,
    private val createIndividualRoundHandler: CreateIndividualRoundHandler,
    private val updateTeamRoundHandler: UpdateTeamRoundHandler,
    private val updateIndividualRoundHandler: UpdateIndividualRoundHandler,
    private val deleteRoundHandler: DeleteRoundHandler,
    private val getRoundsByEventHandler: GetRoundsByEventHandler,
    private val getRoundHandler: GetRoundHandler,
    private val updateHoleHandler: UpdateHoleHandler,
    private val updatePlayerScoreForHoleHandler: UpdatePlayerScoreForHoleHandler,
    private val updateTeamScoreForHoleHandler: UpdateTeamScoreForHoleHandler,
    private val updateTeamMemberScoreForHoleHandler: UpdateTeamMemberScoreForHoleHandler
) {
    @PostMapping("/team")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeamRound(@Valid @RequestBody request: CreateTeamRoundRequest): AdminRoundResponse =
        createTeamRoundHandler.handle(request.toInput()).toAdminResponse()

    @PostMapping("/individual")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIndividualRound(@Valid @RequestBody request: CreateIndividualRoundRequest): AdminRoundResponse =
        createIndividualRoundHandler.handle(request.toInput()).toAdminResponse()

    @PutMapping("/{id}/team")
    fun updateTeamRound(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTeamRoundRequest
    ): AdminRoundResponse =
        updateTeamRoundHandler.handle(id, request.toInput()).toAdminResponse()

    @PutMapping("/{id}/individual")
    fun updateIndividualRound(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateIndividualRoundRequest
    ): AdminRoundResponse =
        updateIndividualRoundHandler.handle(id, request.toInput()).toAdminResponse()

    @GetMapping
    fun getByEvent(@RequestParam eventId: UUID): List<AdminRoundResponse> =
        getRoundsByEventHandler.handle(eventId).map { it.toAdminResponse() }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): AdminRoundResponse =
        getRoundHandler.handle(id).toAdminResponse()

    @PatchMapping("/{id}/hole/{holeNumber}")
    fun updateHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @Valid @RequestBody request: UpdateHoleRequest
    ): AdminRoundResponse {
        updateHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            par = request.par,
            scoringFormatOverride = request.scoringFormatOverride,
            clearScoringFormatOverride = request.clearScoringFormatOverride
        )
        return getRoundHandler.handle(id).toAdminResponse()
    }

    @PatchMapping("/{id}/hole/{holeNumber}/player-score/{playerId}")
    fun updatePlayerScoreForHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @PathVariable playerId: UUID,
        @Valid @RequestBody request: UpdatePlayerScoreForHoleRequest
    ): AdminRoundResponse {
        updatePlayerScoreForHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            playerId = playerId,
            throws = request.throws
        )
        return getRoundHandler.handle(id).toAdminResponse()
    }

    @PatchMapping("/{id}/hole/{holeNumber}/team-score/{teamId}")
    fun updateTeamScoreForHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @PathVariable teamId: UUID,
        @Valid @RequestBody request: UpdateTeamScoreForHoleRequest
    ): AdminRoundResponse {
        updateTeamScoreForHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            teamId = teamId,
            teamThrows = request.teamThrows
        )
        return getRoundHandler.handle(id).toAdminResponse()
    }

    @PatchMapping("/{id}/hole/{holeNumber}/team-score/{teamId}/member/{playerId}")
    fun updateTeamMemberScoreForHole(
        @PathVariable id: UUID,
        @PathVariable holeNumber: Int,
        @PathVariable teamId: UUID,
        @PathVariable playerId: UUID,
        @Valid @RequestBody request: UpdateTeamMemberScoreForHoleRequest
    ): AdminRoundResponse {
        updateTeamMemberScoreForHoleHandler.handle(
            roundId = id,
            holeNumber = holeNumber,
            teamId = teamId,
            playerId = playerId,
            throws = request.throws,
            isCounted = request.isCounted
        )
        return getRoundHandler.handle(id).toAdminResponse()
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRound(@PathVariable id: UUID) {
        deleteRoundHandler.handle(id)
    }
}
