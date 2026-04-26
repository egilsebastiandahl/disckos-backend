package no.disckos.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class SupabaseAuthConfig(
    @Value("\${SUPABASE_JWT_SECRET}") val jwtSecret: String,
    @Value("\${SUPABASE_URL:}") val supabaseUrl: String = "",
)
