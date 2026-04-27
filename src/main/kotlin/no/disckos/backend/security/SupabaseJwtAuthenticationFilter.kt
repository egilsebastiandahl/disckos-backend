package no.disckos.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.disckos.backend.repository.ProfileRepository
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class SupabaseJwtAuthenticationFilter(
    private val supabaseJwtService: SupabaseJwtService,
    @Lazy private val profileRepository: ProfileRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrBlank() || !header.startsWith("Bearer ") || SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.removePrefix("Bearer ").trim()
        try {
            val claims = supabaseJwtService.parseToken(token)
            val userId = claims.subject
            val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))

            // Check if the user is an admin based on their profile
            try {
                val uuid = UUID.fromString(userId)
                val profile = profileRepository.findById(uuid)
                if (profile.isPresent && profile.get().isAdmin == true) {
                    authorities.add(SimpleGrantedAuthority("ROLE_admin"))
                }
            } catch (_: Exception) {
                // If profile lookup fails, continue with basic USER role
            }

            val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        } catch (ex: Exception) {
            // Not a valid Supabase token — leave unauthenticated
        }
        filterChain.doFilter(request, response)
    }
}
