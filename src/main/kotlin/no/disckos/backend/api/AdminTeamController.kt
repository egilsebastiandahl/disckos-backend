package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.team.AdminTeamResponse
import no.disckos.backend.api.dto.admin.team.CreateTeamRequest
import no.disckos.backend.api.dto.admin.team.UpdateTeamRequest
import no.disckos.backend.application.admin.team.CreateTeamHandler
import no.disckos.backend.application.admin.team.DeleteTeamHandler
import no.disckos.backend.application.admin.team.GetTeamsHandler
import no.disckos.backend.application.admin.team.UpdateTeamHandler
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin/team")
@PreAuthorize("hasRole('admin')")
class AdminTeamController(
    private val createTeamHandler: CreateTeamHandler,
    private val updateTeamHandler: UpdateTeamHandler,
    private val deleteTeamHandler: DeleteTeamHandler,
    private val getTeamsHandler: GetTeamsHandler
) {
    @GetMapping
    fun getTeams(@RequestParam(required = false) eventId: UUID?): List<AdminTeamResponse> =
        getTeamsHandler.handle(eventId).map { it.toAdminResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(@Valid @RequestBody request: CreateTeamRequest): AdminTeamResponse =
        createTeamHandler.handle(request.toInput()).toAdminResponse()

    @PutMapping("/{id}")
    fun updateTeam(@PathVariable id: UUID, @RequestBody request: UpdateTeamRequest): AdminTeamResponse =
        updateTeamHandler.handle(request.toInput(id)).toAdminResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeam(@PathVariable id: UUID) {
        deleteTeamHandler.handle(id)
    }
}
