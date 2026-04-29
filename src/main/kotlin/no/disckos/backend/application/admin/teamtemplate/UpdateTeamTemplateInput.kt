package no.disckos.backend.application.admin.teamtemplate

import java.util.UUID

data class UpdateTeamTemplateInput(
    val id: UUID,
    val name: String?
)
