package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.event.AdminEventResponse
import no.disckos.backend.api.dto.admin.event.UpdateEventRequest
import no.disckos.backend.api.dto.admin.player.CreatePlayerRequest
import no.disckos.backend.api.dto.admin.player.PlayerResponse
import no.disckos.backend.api.dto.admin.player.UpdatePlayerRequest
import no.disckos.backend.api.dto.admin.event.CreateEventRequest
import no.disckos.backend.application.admin.event.DeleteEventHandler
import no.disckos.backend.application.admin.event.PublishEventHandler
import no.disckos.backend.application.admin.event.UnpublishEventHandler
import no.disckos.backend.application.admin.event.UpdateEventHandler
import no.disckos.backend.application.admin.event.UpdateEventInput
import no.disckos.backend.application.admin.player.CreatePlayerHandler
import no.disckos.backend.application.admin.player.CreatePlayerInput
import no.disckos.backend.application.admin.player.DeletePlayerHandler
import no.disckos.backend.application.admin.player.UpdatePlayerHandler
import no.disckos.backend.application.admin.player.UpdatePlayerInput
import no.disckos.backend.application.admin.event.CreateEventHandler
import no.disckos.backend.application.admin.event.CreateEventInput
import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.Player
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminProtectedController(
    private val createPlayerHandler: CreatePlayerHandler,
    private val updatePlayerHandler: UpdatePlayerHandler,
    private val deletePlayerHandler: DeletePlayerHandler,
    private val createEventHandler: CreateEventHandler,
    private val updateEventHandler: UpdateEventHandler,
    private val publishEventHandler: PublishEventHandler,
    private val unpublishEventHandler: UnpublishEventHandler,
    private val deleteEventHandler: DeleteEventHandler
) {

    @GetMapping("/players")
    fun getPlayers(): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(emptyList())
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlayer(@Valid @RequestBody request: CreatePlayerRequest): PlayerResponse {
        val created = createPlayerHandler.handle(request.toInput())
        return created.toResponse()
    }

    @PutMapping("/players/{id}")
    fun updatePlayer(
        @PathVariable id: java.util.UUID,
        @RequestBody request: UpdatePlayerRequest
    ): PlayerResponse {
        val updated = updatePlayerHandler.handle(request.toInput(id))
        return updated.toResponse()
    }

    @DeleteMapping("/players/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlayer(@PathVariable id: java.util.UUID) {
        deletePlayerHandler.handle(id)
    }

    @GetMapping("/events")
    fun getEvents(): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(emptyList())
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(@Valid @RequestBody request: CreateEventRequest): AdminEventResponse {
        val created = createEventHandler.handle(request.toInput())
        return created.toAdminResponse()
    }

    @PutMapping("/events/{id}")
    fun updateEvent(
        @PathVariable id: java.util.UUID,
        @RequestBody request: UpdateEventRequest
    ): AdminEventResponse {
        val updated = updateEventHandler.handle(request.toInput(id))
        return updated.toAdminResponse()
    }

    @PostMapping("/events/{id}/publish")
    fun publishEvent(@PathVariable id: java.util.UUID): AdminEventResponse =
        publishEventHandler.handle(id).toAdminResponse()

    @PostMapping("/events/{id}/unpublish")
    fun unpublishEvent(@PathVariable id: java.util.UUID): AdminEventResponse =
        unpublishEventHandler.handle(id).toAdminResponse()

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable id: java.util.UUID) {
        deleteEventHandler.handle(id)
    }

    private fun CreatePlayerRequest.toInput(): CreatePlayerInput =
        CreatePlayerInput(
            name = name,
            gender = gender,
            roundsPlayed = roundsPlayed,
            averageScore = averageScore,
            bestScore = bestScore,
            worstScore = worstScore,
            singleBogeyCount = singleBogeyCount,
            doubleBogeyCount = doubleBogeyCount,
            tripleBogeyCount = tripleBogeyCount,
            parCount = parCount,
            birdieCount = birdieCount,
            aceCount = aceCount,
            eagleCount = eagleCount,
            throws = throws,
            catchphrase = catchphrase
        )

    private fun UpdatePlayerRequest.toInput(id: java.util.UUID): UpdatePlayerInput =
        UpdatePlayerInput(
            id = id,
            name = name,
            gender = gender,
            roundsPlayed = roundsPlayed,
            averageScore = averageScore,
            bestScore = bestScore,
            worstScore = worstScore,
            singleBogeyCount = singleBogeyCount,
            doubleBogeyCount = doubleBogeyCount,
            tripleBogeyCount = tripleBogeyCount,
            parCount = parCount,
            birdieCount = birdieCount,
            aceCount = aceCount,
            eagleCount = eagleCount,
            throws = throws,
            catchphrase = catchphrase
        )

    private fun Player.toResponse(): PlayerResponse =
        PlayerResponse(
            id = requireNotNull(id),
            name = requireNotNull(name),
            gender = gender,
            roundsPlayed = roundsPlayed,
            averageScore = averageScore,
            bestScore = bestScore,
            worstScore = worstScore,
            singleBogeyCount = singleBogeyCount,
            doubleBogeyCount = doubleBogeyCount,
            tripleBogeyCount = tripleBogeyCount,
            parCount = parCount,
            birdieCount = birdieCount,
            aceCount = aceCount,
            eagleCount = eagleCount,
            throws = throwsField,
            catchphrase = catchphrase,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private fun CreateEventRequest.toInput(): CreateEventInput =
        CreateEventInput(
            date = date,
            title = title,
            description = description,
            location = location,
            teamEvent = teamEvent,
            rounds = rounds
        )

    private fun UpdateEventRequest.toInput(id: java.util.UUID): UpdateEventInput =
        UpdateEventInput(
            id = id,
            date = date,
            title = title,
            description = description,
            location = location,
            teamEvent = teamEvent,
            rounds = rounds
        )

    private fun EventEntity.toAdminResponse(): AdminEventResponse =
        AdminEventResponse(
            id = id,
            date = date,
            title = title,
            description = description,
            location = location,
            teamEvent = teamEvent,
            rounds = rounds,
            published = published
        )
}
