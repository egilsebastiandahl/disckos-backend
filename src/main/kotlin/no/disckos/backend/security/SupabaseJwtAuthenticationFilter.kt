package no.disckos.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SupabaseJwtAuthenticationFilter(private val supabaseJwtService: SupabaseJwtService) : OncePerRequestFilter() {
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
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        } catch (ex: Exception) {
            // Not a valid Supabase token — leave unauthenticated
        }
        filterChain.doFilter(request, response)
    }
}
