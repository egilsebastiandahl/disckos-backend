package no.disckos.backend.api.dto.event

import java.util.UUID

data class EventSignupResponse(
    val profileId: UUID,
    val displayName: String?,
    val avatarUrl: String?,
)
