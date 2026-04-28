package no.disckos.backend.api

import no.disckos.backend.api.dto.LinkPlayerDto
import no.disckos.backend.api.dto.ProfileDto
import no.disckos.backend.api.dto.UpdateProfileDto
import no.disckos.backend.security.SupabaseJwtService
import no.disckos.backend.service.ProfileService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import no.disckos.backend.domain.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

@RestController
@RequestMapping("/api")
class ProfileController(
    private val profileService: ProfileService,
    private val supabaseJwtService: SupabaseJwtService,
) {

    @GetMapping("/profile")
    fun profile(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String?): ProfileDto {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? String ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
        val userId = try { UUID.fromString(principal) } catch (ex: Exception) { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id") }

        var email: String? = null
        if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ")) {
            val token = authorization.removePrefix("Bearer ").trim()
            try {
                val claims = supabaseJwtService.parseToken(token)
                email = claims["email"] as? String
            } catch (ex: Exception) {
                // ignore - email just won't be available
            }
        }

        val profile = profileService.getOrCreateProfile(userId, email)
        return profile.toDto()
    }

    @PatchMapping("/profile")
    fun updateProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String?, @RequestBody body: UpdateProfileDto?): ProfileDto {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? String ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
        val userId = try { UUID.fromString(principal) } catch (ex: Exception) { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id") }

        if (body == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing body")

        try {
            val updated = profileService.updateProfile(userId, body.username, body.displayName, body.bio)
            return updated.toDto()
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken or invalid")
        } catch (ex: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")
        }
    }

    @PutMapping("/profile/player")
    fun linkPlayer(@RequestBody body: LinkPlayerDto?): ProfileDto {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? String ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
        val userId = try { UUID.fromString(principal) } catch (ex: Exception) { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id") }

        try {
            val updated = profileService.linkPlayer(userId, body?.playerId)
            return updated.toDto()
        } catch (ex: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message)
        } catch (ex: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, ex.message)
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
