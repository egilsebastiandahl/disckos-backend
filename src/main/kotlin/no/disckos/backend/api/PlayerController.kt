package no.disckos.backend.api

import no.disckos.backend.api.dto.player.PlayerResponse
import no.disckos.backend.api.dto.player.PlayerRoundSummaryResponse
import no.disckos.backend.api.dto.player.PlayerStatsByLocationResponse
import no.disckos.backend.api.dto.player.PlayerStatsResponse
import no.disckos.backend.api.dto.player.toResponse
import no.disckos.backend.application.player.GetPlayerHandler
import no.disckos.backend.application.player.GetPlayerRoundsHandler
import no.disckos.backend.application.player.GetPlayerStatsByLocationHandler
import no.disckos.backend.application.player.GetPlayerStatsHandler
import no.disckos.backend.application.player.GetPlayersHandler
import no.disckos.backend.repository.ProfileRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/player")
class PlayerController(
    private val getPlayerHandler: GetPlayerHandler,
    private val getPlayersHandler: GetPlayersHandler,
    private val profileRepository: ProfileRepository,
    private val getPlayerStatsHandler: GetPlayerStatsHandler,
    private val getPlayerStatsByLocationHandler: GetPlayerStatsByLocationHandler,
    private val getPlayerRoundsHandler: GetPlayerRoundsHandler,
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

    @GetMapping("/{id}/stats")
    fun getPlayerStats(@PathVariable id: UUID): PlayerStatsResponse =
        getPlayerStatsHandler.handle(id)

    @GetMapping("/{id}/stats/by-location")
    fun getPlayerStatsByLocation(@PathVariable id: UUID): PlayerStatsByLocationResponse =
        getPlayerStatsByLocationHandler.handle(id)

    @GetMapping("/{id}/rounds")
    fun getPlayerRounds(
        @PathVariable id: UUID,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): List<PlayerRoundSummaryResponse> =
        getPlayerRoundsHandler.handle(id, limit)
}