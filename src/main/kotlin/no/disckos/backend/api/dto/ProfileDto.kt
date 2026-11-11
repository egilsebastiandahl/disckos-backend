package no.disckos.backend.api.dto

import no.disckos.backend.domain.Profile
import java.util.UUID

data class ProfileDto(
    val id: UUID,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val isAdmin: Boolean,
    /** Can give and revoke the admin role. */
    val isOwner: Boolean = false,
    val playerId: UUID? = null,
    val playerName: String? = null,
)

fun Profile.toDto() = ProfileDto(
    id = id!!,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    isAdmin = isAdmin ?: false,
    isOwner = isOwner,
    playerId = player?.id,
    playerName = player?.name,
)
