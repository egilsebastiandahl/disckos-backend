package no.disckos.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import no.disckos.backend.config.AdminAuthConfig
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
 class JwtService(private val adminAuthConfig: AdminAuthConfig) {
    private val accessKey = Keys.hmacShaKeyFor(adminAuthConfig.jwtSecret.toByteArray(StandardCharsets.UTF_8))
    private val refreshKey = Keys.hmacShaKeyFor(adminAuthConfig.refreshJwtSecret.toByteArray(StandardCharsets.UTF_8))

     fun createAccessToken(): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject("admin")
            .claim("roles", listOf("admin"))
            .claim("tokenType", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(adminAuthConfig.accessTokenExpirySeconds)))
            .signWith(accessKey, Jwts.SIG.HS256)
            .compact()
    }

     fun createRefreshToken(): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject("admin")
            .claim("tokenType", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(adminAuthConfig.refreshTokenExpirySeconds)))
            .signWith(refreshKey, Jwts.SIG.HS256)
            .compact()
    }

     fun parseAccessToken(token: String): Claims = parseToken(token, accessKey, "access")

     fun parseRefreshToken(token: String): Claims = parseToken(token, refreshKey, "refresh")

     fun accessTokenExpirySeconds(): Long = adminAuthConfig.accessTokenExpirySeconds

    private fun parseToken(token: String, key: SecretKey, expectedType: String): Claims {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        val tokenType = claims["tokenType"]?.toString()
        require(tokenType == expectedType) { "Unexpected token type" }
        return claims
    }
}
