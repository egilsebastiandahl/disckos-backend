package no.disckos.backend.api.dto

data class UpdateProfileDto(
    val username: String?,
    val displayName: String?,
    val bio: String?,
)
