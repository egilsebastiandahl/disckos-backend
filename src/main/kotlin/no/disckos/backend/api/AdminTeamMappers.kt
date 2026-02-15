package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.team.AdminTeamResponse
import no.disckos.backend.api.dto.admin.team.CreateTeamRequest
import no.disckos.backend.api.dto.admin.team.UpdateTeamRequest
import no.disckos.backend.application.admin.team.CreateTeamInput
import no.disckos.backend.application.admin.team.UpdateTeamInput
import no.disckos.backend.domain.TeamEntity

fun CreateTeamRequest.toInput(): CreateTeamInput =
    CreateTeamInput(
        eventId = eventId,
        name = name
    )

fun UpdateTeamRequest.toInput(id: java.util.UUID): UpdateTeamInput =
    UpdateTeamInput(
        id = id,
        name = name
    )

fun TeamEntity.toAdminResponse(): AdminTeamResponse =
    AdminTeamResponse(
        id = id,
        eventId = eventId,
        name = name
    )
