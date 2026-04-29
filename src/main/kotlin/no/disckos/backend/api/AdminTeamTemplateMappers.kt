package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.teamtemplate.CreateTeamTemplateRequest
import no.disckos.backend.api.dto.admin.teamtemplate.UpdateTeamTemplateRequest
import no.disckos.backend.application.admin.teamtemplate.CreateTeamTemplateInput
import no.disckos.backend.application.admin.teamtemplate.UpdateTeamTemplateInput

fun CreateTeamTemplateRequest.toInput(): CreateTeamTemplateInput =
    CreateTeamTemplateInput(
        name = name
    )

fun UpdateTeamTemplateRequest.toInput(id: java.util.UUID): UpdateTeamTemplateInput =
    UpdateTeamTemplateInput(
        id = id,
        name = name
    )
