package no.disckos.backend.api.dto.admin.teamtemplate

import jakarta.validation.constraints.NotBlank

data class CreateTeamTemplateRequest(
    @field:NotBlank
    val name: String
)
