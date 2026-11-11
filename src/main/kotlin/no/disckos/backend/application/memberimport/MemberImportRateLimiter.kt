package no.disckos.backend.application.memberimport

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * At most [LIMIT] member import requests (previews and uploads) per member per hour. In memory,
 * which is enough for one backend instance; it only has to stop scripts, not a determined club.
 */
@Component
class MemberImportRateLimiter {

    /** Overridable in tests. */
    internal var now: () -> Long = System::currentTimeMillis

    private val calls = ConcurrentHashMap<UUID, ArrayDeque<Long>>()

    fun acquire(profileId: UUID) {
        val log = calls.computeIfAbsent(profileId) { ArrayDeque() }
        synchronized(log) {
            val t = now()
            while (log.isNotEmpty() && log.first() <= t - WINDOW_MS) log.removeFirst()
            if (log.size >= LIMIT) {
                throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "For mange opplastinger på kort tid. Prøv igjen om en stund.")
            }
            log.addLast(t)
        }
    }

    companion object {
        const val LIMIT = 30
        const val WINDOW_MS = 60 * 60 * 1000L
    }
}
