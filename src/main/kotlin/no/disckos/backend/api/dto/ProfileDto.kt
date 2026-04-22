package no.disckos.backend.api.dto

import java.util.UUID

data class ProfileDto(
    val id: UUID,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val isAdmin: Boolean,
)
