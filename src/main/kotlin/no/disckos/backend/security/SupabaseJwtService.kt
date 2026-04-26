package no.disckos.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import no.disckos.backend.config.SupabaseAuthConfig
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.interfaces.ECPublicKey
import java.security.AlgorithmParameters
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import tools.jackson.databind.ObjectMapper

@Service
class SupabaseJwtService(private val supabaseAuthConfig: SupabaseAuthConfig) {

    private val hmacKey: SecretKey = SecretKeySpec(
        supabaseAuthConfig.jwtSecret.toByteArray(StandardCharsets.UTF_8),
        "HmacSHA256"
    )

    // Cache of kid -> EC public key, loaded from JWKS
    @Volatile
    private var ecKeys: Map<String, PublicKey> = emptyMap()

    init {
        if (supabaseAuthConfig.supabaseUrl.isNotBlank()) {
            try {
                ecKeys = fetchJwks(supabaseAuthConfig.supabaseUrl)
            } catch (ex: Exception) {
                // Will retry on first request
            }
        }
    }

    fun parseToken(token: String): Claims {
        // Peek at the header to determine the algorithm
        val headerEnd = token.indexOf('.')
        if (headerEnd < 0) throw IllegalArgumentException("Invalid JWT")
        val headerJson = String(Base64.getUrlDecoder().decode(token.substring(0, headerEnd)))
        val mapper = ObjectMapper()
        @Suppress("UNCHECKED_CAST")
        val header: Map<String, Any> = mapper.readValue(headerJson, Map::class.java) as Map<String, Any>
        val alg = header["alg"] as? String ?: "HS256"

        return if (alg.startsWith("ES")) {
            val kid = header["kid"] as? String
            val key = resolveEcKey(kid)
                ?: throw SecurityException("No matching public key found for kid=$kid")
            Jwts.parser()
                .verifyWith(key as ECPublicKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } else {
            Jwts.parser()
                .verifyWith(hmacKey)
                .build()
                .parseSignedClaims(token)
                .payload
        }
    }

    private fun resolveEcKey(kid: String?): PublicKey? {
        // Try cached keys first
        val cached = if (kid != null) ecKeys[kid] else ecKeys.values.firstOrNull()
        if (cached != null) return cached

        // Refresh JWKS and retry
        if (supabaseAuthConfig.supabaseUrl.isNotBlank()) {
            ecKeys = fetchJwks(supabaseAuthConfig.supabaseUrl)
            return if (kid != null) ecKeys[kid] else ecKeys.values.firstOrNull()
        }
        return null
    }

    private fun fetchJwks(supabaseUrl: String): Map<String, PublicKey> {
        val url = supabaseUrl.trimEnd('/') + "/auth/v1/.well-known/jwks.json"
        val json = URI(url).toURL().readText()
        val mapper = ObjectMapper()
        @Suppress("UNCHECKED_CAST")
        val jwks: Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>
        val keys = jwks["keys"] as? List<*> ?: return emptyMap()
        val result = mutableMapOf<String, PublicKey>()
        for (entry in keys) {
            val jwk = entry as? Map<*, *> ?: continue
            val kty = jwk["kty"] as? String ?: continue
            if (kty != "EC") continue
            val kid = jwk["kid"] as? String ?: continue
            val x = jwk["x"] as? String ?: continue
            val y = jwk["y"] as? String ?: continue
            val crv = jwk["crv"] as? String ?: "P-256"
            val pubKey = buildEcPublicKey(x, y, crv)
            result[kid] = pubKey
        }
        return result
    }

    private fun buildEcPublicKey(xB64: String, yB64: String, crv: String): PublicKey {
        val xBytes = Base64.getUrlDecoder().decode(xB64)
        val yBytes = Base64.getUrlDecoder().decode(yB64)
        val ecPoint = ECPoint(BigInteger(1, xBytes), BigInteger(1, yBytes))
        val curveName = when (crv) {
            "P-256" -> "secp256r1"
            "P-384" -> "secp384r1"
            "P-521" -> "secp521r1"
            else -> "secp256r1"
        }
        val params = AlgorithmParameters.getInstance("EC")
        params.init(ECGenParameterSpec(curveName))
        val ecSpec = params.getParameterSpec(ECParameterSpec::class.java)
        val keySpec = ECPublicKeySpec(ecPoint, ecSpec)
        return KeyFactory.getInstance("EC").generatePublic(keySpec)
    }
}
