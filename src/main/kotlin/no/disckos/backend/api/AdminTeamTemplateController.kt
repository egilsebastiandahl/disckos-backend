package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.teamtemplate.AddTeamTemplateMemberRequest
import no.disckos.backend.api.dto.admin.teamtemplate.AdminTeamTemplateResponse
import no.disckos.backend.api.dto.admin.teamtemplate.CreateTeamTemplateRequest
import no.disckos.backend.api.dto.admin.teamtemplate.TeamTemplateStatsResponse
import no.disckos.backend.api.dto.admin.teamtemplate.UpdateTeamTemplateRequest
import no.disckos.backend.application.admin.teamtemplate.AddTeamTemplateMemberHandler
import no.disckos.backend.application.admin.teamtemplate.CreateTeamTemplateHandler
import no.disckos.backend.application.admin.teamtemplate.DeleteTeamTemplateHandler
import no.disckos.backend.application.admin.teamtemplate.GetTeamTemplateStatsHandler
import no.disckos.backend.application.admin.teamtemplate.GetTeamTemplatesHandler
import no.disckos.backend.application.admin.teamtemplate.RemoveTeamTemplateMemberHandler
import no.disckos.backend.application.admin.teamtemplate.UpdateTeamTemplateHandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin/team-template")
@PreAuthorize("hasRole('admin')")
class AdminTeamTemplateController(
    private val createTeamTemplateHandler: CreateTeamTemplateHandler,
    private val updateTeamTemplateHandler: UpdateTeamTemplateHandler,
    private val deleteTeamTemplateHandler: DeleteTeamTemplateHandler,
    private val getTeamTemplatesHandler: GetTeamTemplatesHandler,
    private val addTeamTemplateMemberHandler: AddTeamTemplateMemberHandler,
    private val removeTeamTemplateMemberHandler: RemoveTeamTemplateMemberHandler,
    private val getTeamTemplateStatsHandler: GetTeamTemplateStatsHandler
) {

    @GetMapping
    fun getTeamTemplates(): ResponseEntity<List<AdminTeamTemplateResponse>> {
        return ResponseEntity.ok(getTeamTemplatesHandler.handle())
    }

    @GetMapping("/{id}")
    fun getTeamTemplate(@PathVariable id: UUID): ResponseEntity<AdminTeamTemplateResponse> {
        val template = getTeamTemplatesHandler.handleSingle(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(template)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeamTemplate(@Valid @RequestBody request: CreateTeamTemplateRequest): AdminTeamTemplateResponse {
        val created = createTeamTemplateHandler.handle(request.toInput())
        return AdminTeamTemplateResponse(
            id = created.id,
            name = created.name,
            members = emptyList()
        )
    }

    @PutMapping("/{id}")
    fun updateTeamTemplate(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTeamTemplateRequest
    ): ResponseEntity<AdminTeamTemplateResponse> {
        updateTeamTemplateHandler.handle(request.toInput(id))
        val updated = getTeamTemplatesHandler.handleSingle(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeamTemplate(@PathVariable id: UUID) {
        deleteTeamTemplateHandler.handle(id)
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun addMember(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddTeamTemplateMemberRequest
    ): ResponseEntity<AdminTeamTemplateResponse> {
        addTeamTemplateMemberHandler.handle(id, request.playerId)
        val updated = getTeamTemplatesHandler.handleSingle(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}/members/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(@PathVariable id: UUID, @PathVariable playerId: UUID) {
        removeTeamTemplateMemberHandler.handle(id, playerId)
    }

    @GetMapping("/{id}/stats")
    fun getStats(@PathVariable id: UUID): ResponseEntity<TeamTemplateStatsResponse> {
        return ResponseEntity.ok(getTeamTemplateStatsHandler.handle(id))
    }
}
