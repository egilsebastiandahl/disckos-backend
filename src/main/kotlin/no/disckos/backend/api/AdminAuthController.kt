package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.AdminUserDto
import no.disckos.backend.api.dto.admin.ErrorResponse
import no.disckos.backend.api.dto.admin.LoginRequest
import no.disckos.backend.api.dto.admin.LoginResponse
import no.disckos.backend.api.dto.admin.LogoutRequest
import no.disckos.backend.api.dto.admin.RefreshRequest
import no.disckos.backend.api.dto.admin.RefreshResponse
import no.disckos.backend.config.AdminAuthConfig
import no.disckos.backend.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminAuthController(
    private val adminAuthConfig: AdminAuthConfig,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        // TODO: add rate limiter here (Bucket4j/Resilience4j) to protect login attempts.
        val usernameMatches = request.username == adminAuthConfig.adminUsername
        val passwordMatches = passwordEncoder.matches(request.password, adminAuthConfig.adminPasswordHash)
        if (!usernameMatches || !passwordMatches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid credentials"))
        }

        val accessToken = jwtService.createAccessToken()
        val refreshToken = jwtService.createRefreshToken()
        val user = AdminUserDto(
            id = "admin",
            name = "Your Name",
            roles = listOf("admin"),
        )
        return ResponseEntity.ok(
            LoginResponse(
                user = user,
                token = accessToken,
                refreshToken = refreshToken,
                expiresIn = jwtService.accessTokenExpirySeconds(),
            ),
        )
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<Any> {
        // TODO: add rate limiter here (Bucket4j/Resilience4j) to protect refresh spam.
        // TODO: when adding a DB or multi-admin, store and revoke refresh tokens server-side.
        return try {
            jwtService.parseRefreshToken(request.refreshToken)
            val newAccess = jwtService.createAccessToken()
            val newRefresh = jwtService.createRefreshToken()
            ResponseEntity.ok(
                RefreshResponse(
                    accessToken = newAccess,
                    refreshToken = newRefresh,
                    expiresIn = jwtService.accessTokenExpirySeconds(),
                ),
            )
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid refresh token"))
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest): ResponseEntity<Map<String, String>> {
        // No server-side revocation without a DB; "client" should discard tokens.
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
