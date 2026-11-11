package no.disckos.backend.application.memberimport

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class MemberImportRateLimiterTest {

    @Test
    fun `allows the limit per hour per member, then refuses until the hour has passed`() {
        var t = 0L
        val limiter = MemberImportRateLimiter().apply { now = { t } }
        val member = UUID.randomUUID()

        repeat(MemberImportRateLimiter.LIMIT) { limiter.acquire(member) }
        assertThrows<ResponseStatusException> { limiter.acquire(member) }
        limiter.acquire(UUID.randomUUID()) // someone else is unaffected

        t += MemberImportRateLimiter.WINDOW_MS
        limiter.acquire(member)
    }
}
