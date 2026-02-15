package no.disckos.backend.application.admin.team

import java.util.UUID

data class UpdateTeamInput(
    val id: UUID,
    val name: String?
)
