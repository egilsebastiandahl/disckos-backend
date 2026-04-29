package no.disckos.backend.api

import no.disckos.backend.api.dto.event.EventResponse
import no.disckos.backend.api.dto.event.EventSignupResponse
import no.disckos.backend.repository.EventSignupRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.application.event.GetEventsHandler
import no.disckos.backend.domain.EventEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/event")
class EventController(
    private val getEventsHandler: GetEventsHandler,
    private val locationRepository: LocationRepository,
    private val eventSignupRepository: EventSignupRepository,
    private val profileRepository: ProfileRepository,
) {
    // Gets all the events
    @GetMapping
    fun getAll(): List<EventResponse> =
        getEventsHandler.handle().let { events ->
            val locationIds = events.mapNotNull { it.locationId }.distinct()
            val locationMap = if (locationIds.isEmpty()) {
                emptyMap()
            } else {
                locationRepository.findAllById(locationIds).associateBy { it.id }
            }

            val eventIds = events.map { it.id }
            val allSignups = if (eventIds.isEmpty()) {
                emptyList()
            } else {
                eventSignupRepository.findAllByEventIdIn(eventIds)
            }
            val signupsByEvent = allSignups.groupBy { it.eventId }

            // Resolve profile info for all signed-up users
            val profileIds = allSignups.map { it.profileId }.distinct()
            val profileMap = if (profileIds.isEmpty()) {
                emptyMap()
            } else {
                profileRepository.findAllById(profileIds).associateBy { it.id }
            }

            events.map { event ->
                val eventSignups = signupsByEvent[event.id].orEmpty().map { signup ->
                    val profile = profileMap[signup.profileId]
                    EventSignupResponse(
                        profileId = signup.profileId,
                        displayName = profile?.displayName ?: profile?.username,
                        avatarUrl = profile?.avatarUrl,
                    )
                }
                event.toResponse(locationMap[event.locationId], eventSignups)
            }
        }


    private fun EventEntity.toResponse(location: no.disckos.backend.domain.LocationEntity?, signups: List<EventSignupResponse> = emptyList()): EventResponse =
        EventResponse(
            id = id,
            date = date,
            title = title,
            description = description,
            location = location?.toResponse(),
            teamEvent = teamEvent,
            rounds = rounds,
            major = major,
            signups = signups,
        )
}
