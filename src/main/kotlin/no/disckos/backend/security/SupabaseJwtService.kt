package no.disckos.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import no.disckos.backend.config.SupabaseAuthConfig
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

@Service
class SupabaseJwtService(private val supabaseAuthConfig: SupabaseAuthConfig) {
    private val key: SecretKey = Keys.hmacShaKeyFor(supabaseAuthConfig.jwtSecret.toByteArray(StandardCharsets.UTF_8))

    fun parseToken(token: String): Claims {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims
    }
}
