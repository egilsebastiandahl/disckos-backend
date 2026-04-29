package no.disckos.backend.api

import no.disckos.backend.domain.EventSignupEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.EventSignupRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/event/{eventId}/signup")
class EventSignupController(
    private val eventSignupRepository: EventSignupRepository,
    private val eventRepository: EventRepository,
) {

    @PostMapping
    @Transactional
    fun signup(@PathVariable eventId: UUID): ResponseEntity<Void> {
        val profileId = getAuthenticatedUserId()

        if (!eventRepository.existsById(eventId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")
        }

        if (eventSignupRepository.existsByEventIdAndProfileId(eventId, profileId)) {
            return ResponseEntity.ok().build()
        }

        val signup = EventSignupEntity(
            eventId = eventId,
            profileId = profileId,
            signedUpAt = OffsetDateTime.now(),
        )
        eventSignupRepository.save(signup)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping
    @Transactional
    fun unsignup(@PathVariable eventId: UUID): ResponseEntity<Void> {
        val profileId = getAuthenticatedUserId()
        eventSignupRepository.deleteByEventIdAndProfileId(eventId, profileId)
        return ResponseEntity.noContent().build()
    }

    private fun getAuthenticatedUserId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal as? String
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
        return try {
            UUID.fromString(principal)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id")
        }
    }
}
