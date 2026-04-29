package no.disckos.backend.api

import no.disckos.backend.api.dto.LinkPlayerDto
import no.disckos.backend.api.dto.ProfileDto
import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.service.ProfileService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/admin/profiles")
@PreAuthorize("hasRole('admin')")
class AdminProfileController(
    private val profileRepository: ProfileRepository,
    private val profileService: ProfileService,
) {

    @GetMapping
    fun getAllProfiles(): List<ProfileDto> {
        return profileRepository.findAll().map { it.toDto() }
    }

    @PutMapping("/{profileId}/player")
    fun linkPlayerToProfile(
        @PathVariable profileId: UUID,
        @RequestBody body: LinkPlayerDto?,
    ): ProfileDto {
        try {
            val updated = profileService.linkPlayer(profileId, body?.playerId)
            return updated.toDto()
        } catch (ex: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message)
        } catch (ex: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, ex.message)
        }
    }

    @DeleteMapping("/{profileId}/player")
    fun unlinkPlayerFromProfile(@PathVariable profileId: UUID): ProfileDto {
        try {
            val updated = profileService.linkPlayer(profileId, null)
            return updated.toDto()
        } catch (ex: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message)
        }
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
