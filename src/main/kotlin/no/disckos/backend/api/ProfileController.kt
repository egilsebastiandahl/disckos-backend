package no.disckos.backend.api

import no.disckos.backend.api.dto.ProfileDto
import no.disckos.backend.security.SupabaseJwtService
import no.disckos.backend.service.ProfileService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
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
}
