package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.score.UpdatePlayerScoreRequest
import no.disckos.backend.api.dto.admin.score.UpdateTeamMemberScoreRequest
import no.disckos.backend.api.dto.admin.score.UpdateTeamScoreRequest
import no.disckos.backend.application.admin.score.DeletePlayerScoreHandler
import no.disckos.backend.application.admin.score.DeleteTeamMemberScoreHandler
import no.disckos.backend.application.admin.score.DeleteTeamScoreHandler
import no.disckos.backend.application.admin.score.UpdatePlayerScoreHandler
import no.disckos.backend.application.admin.score.UpdateTeamMemberScoreHandler
import no.disckos.backend.application.admin.score.UpdateTeamScoreHandler
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin/score")
@PreAuthorize("hasRole('admin')")
class AdminScoreController(
    private val updatePlayerScoreHandler: UpdatePlayerScoreHandler,
    private val deletePlayerScoreHandler: DeletePlayerScoreHandler,
    private val updateTeamScoreHandler: UpdateTeamScoreHandler,
    private val deleteTeamScoreHandler: DeleteTeamScoreHandler,
    private val updateTeamMemberScoreHandler: UpdateTeamMemberScoreHandler,
    private val deleteTeamMemberScoreHandler: DeleteTeamMemberScoreHandler
) {
    @PutMapping("/player/{id}")
    fun updatePlayerScore(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePlayerScoreRequest
    ) =
        updatePlayerScoreHandler.handle(id, request.throws)

    @DeleteMapping("/player/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlayerScore(@PathVariable id: UUID) {
        deletePlayerScoreHandler.handle(id)
    }

    @PutMapping("/team/{id}")
    fun updateTeamScore(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTeamScoreRequest
    ) =
        updateTeamScoreHandler.handle(id, request.teamThrows)

    @DeleteMapping("/team/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeamScore(@PathVariable id: UUID) {
        deleteTeamScoreHandler.handle(id)
    }

    @PutMapping("/team-member/{id}")
    fun updateTeamMemberScore(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTeamMemberScoreRequest
    ) =
        updateTeamMemberScoreHandler.handle(id, request.throws, request.isCounted)

    @DeleteMapping("/team-member/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeamMemberScore(@PathVariable id: UUID) {
        deleteTeamMemberScoreHandler.handle(id)
    }
}
