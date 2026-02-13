package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.player.CreatePlayerRequest
import no.disckos.backend.api.dto.player.PlayerResponse
import no.disckos.backend.api.dto.admin.player.UpdatePlayerRequest
import no.disckos.backend.api.dto.player.toResponse
import no.disckos.backend.application.admin.player.CreatePlayerHandler
import no.disckos.backend.application.admin.player.DeletePlayerHandler
import no.disckos.backend.application.admin.player.UpdatePlayerHandler
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/players")
@PreAuthorize("hasRole('admin')")
class AdminPlayerController(
    private val createPlayerHandler: CreatePlayerHandler,
    private val updatePlayerHandler: UpdatePlayerHandler,
    private val deletePlayerHandler: DeletePlayerHandler
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlayer(@Valid @RequestBody request: CreatePlayerRequest): PlayerResponse {
        val created = createPlayerHandler.handle(request.toInput())
        return created.toResponse()
    }

    @PutMapping("/{id}")
    fun updatePlayer(
        @PathVariable id: java.util.UUID,
        @RequestBody request: UpdatePlayerRequest
    ): PlayerResponse {
        val updated = updatePlayerHandler.handle(request.toInput(id))
        return updated.toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePlayer(@PathVariable id: java.util.UUID) {
        deletePlayerHandler.handle(id)
    }
}
