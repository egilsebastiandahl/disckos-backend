package no.disckos.backend.application.admin.team

import java.util.UUID

data class CreateTeamInput(
    val eventId: UUID,
    val name: String
)
