package no.disckos.backend.api

import no.disckos.backend.api.dto.player.PlayerResponse
import no.disckos.backend.api.dto.player.toResponse
import no.disckos.backend.application.player.GetPlayerHandler
import no.disckos.backend.application.player.GetPlayersHandler
import no.disckos.backend.repository.ProfileRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/player")
class PlayerController(
    private val getPlayerHandler: GetPlayerHandler,
    private val getPlayersHandler: GetPlayersHandler,
    private val profileRepository: ProfileRepository,
) {
    // Gets all the players
    @GetMapping
    fun getAllPlayers(): List<PlayerResponse> =
        getPlayersHandler.handle().map { it.toResponse() }


    @GetMapping("/{id}")
    fun getPlayer(@PathVariable id: java.util.UUID): PlayerResponse{
        val player = getPlayerHandler.handle(id)
        if(player == null) throw Exception("Player not found")
        else return player.toResponse()
    }

    @GetMapping("/linked-ids")
    fun getLinkedPlayerIds(): List<UUID> =
        profileRepository.findAll()
            .mapNotNull { it.player?.id }
}