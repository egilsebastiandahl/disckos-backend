package no.disckos.backend.api.dto.admin.teamtemplate

import java.util.UUID

data class TeamTemplateStatsResponse(
    val teamTemplateId: UUID,
    val teamTemplateName: String,
    val eventsPlayed: Int,
    val members: List<TeamTemplateMemberResponse>
)
