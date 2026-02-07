package no.disckos.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class AdminAuthConfig(
    @Value("\${ADMIN_USERNAME}") val adminUsername: String,
    @Value("\${ADMIN_PASSWORD_HASH}") val adminPasswordHash: String,
    @Value("\${JWT_SECRET}") val jwtSecret: String,
    @Value("\${REFRESH_JWT_SECRET}") val refreshJwtSecret: String,
    @Value("\${ACCESS_TOKEN_EXPIRY_SECONDS}") val accessTokenExpirySeconds: Long,
    @Value("\${REFRESH_TOKEN_EXPIRY_SECONDS}") val refreshTokenExpirySeconds: Long,
)
