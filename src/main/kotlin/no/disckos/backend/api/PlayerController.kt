package no.disckos.backend.api

import no.disckos.backend.api.dto.player.PlayerResponse
import no.disckos.backend.api.dto.player.toResponse
import no.disckos.backend.application.player.GetPlayerHandler
import no.disckos.backend.application.player.GetPlayersHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/player")
class PlayerController(
    private val getPlayerHandler: GetPlayerHandler,
    private val getPlayersHandler: GetPlayersHandler
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





}