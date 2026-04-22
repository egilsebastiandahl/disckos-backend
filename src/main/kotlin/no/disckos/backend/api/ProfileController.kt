package no.disckos.backend.api

import no.disckos.backend.api.dto.ProfileDto
import no.disckos.backend.api.dto.UpdateProfileDto
import no.disckos.backend.security.SupabaseJwtService
import no.disckos.backend.service.ProfileService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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
        return ProfileDto(
            id = profile.id!!,
            username = profile.username,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            bio = profile.bio,
            isAdmin = profile.isAdmin ?: false,
        )
    }

    @PatchMapping("/profile")
    fun updateProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String?, @RequestBody body: UpdateProfileDto?): ProfileDto {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? String ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
        val userId = try { UUID.fromString(principal) } catch (ex: Exception) { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id") }

        if (body == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing body")

        try {
            val updated = profileService.updateProfile(userId, body.username, body.displayName, body.bio)
            return ProfileDto(
                id = updated.id!!,
                username = updated.username,
                displayName = updated.displayName,
                avatarUrl = updated.avatarUrl,
                bio = updated.bio,
                isAdmin = updated.isAdmin ?: false,
            )
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken or invalid")
        } catch (ex: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found")
        }
    }
}
