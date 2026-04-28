package no.disckos.backend.api

import no.disckos.backend.api.dto.ProfileDto
import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.ProfileRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/profiles")
@PreAuthorize("hasRole('admin')")
class AdminProfileController(
    private val profileRepository: ProfileRepository,
) {

    @GetMapping
    fun getAllProfiles(): List<ProfileDto> {
        return profileRepository.findAll().map { it.toDto() }
    }

    private fun Profile.toDto() = ProfileDto(
        id = id!!,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        isAdmin = isAdmin ?: false,
        playerId = player?.id,
        playerName = player?.name,
    )
}
