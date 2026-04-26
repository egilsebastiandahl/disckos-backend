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
class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrBlank() || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.removePrefix("Bearer ").trim()
        try {
            val claims = jwtService.parseAccessToken(token)
            val roles = (claims["roles"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
            val auth = UsernamePasswordAuthenticationToken(claims.subject, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        } catch (ex: Exception) {
            // Not an admin token — let the next filter try
        }
        filterChain.doFilter(request, response)
    }
}
