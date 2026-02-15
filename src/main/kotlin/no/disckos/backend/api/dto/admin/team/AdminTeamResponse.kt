package no.disckos.backend.api.dto.admin.team

import java.util.UUID

data class AdminTeamResponse(
    val id: UUID,
    val eventId: UUID,
    val name: String
)
