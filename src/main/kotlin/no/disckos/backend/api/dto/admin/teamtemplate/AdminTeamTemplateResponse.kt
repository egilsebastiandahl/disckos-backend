package no.disckos.backend.api.dto.admin.teamtemplate

import java.util.UUID

data class TeamTemplateMemberResponse(
    val playerId: UUID,
    val playerName: String
)

data class AdminTeamTemplateResponse(
    val id: UUID,
    val name: String,
    val members: List<TeamTemplateMemberResponse>
)
