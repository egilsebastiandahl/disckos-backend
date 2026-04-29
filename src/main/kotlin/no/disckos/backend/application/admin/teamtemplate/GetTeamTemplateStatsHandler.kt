package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.api.dto.admin.teamtemplate.TeamTemplateMemberResponse
import no.disckos.backend.api.dto.admin.teamtemplate.TeamTemplateStatsResponse
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.TeamRepository
import no.disckos.backend.repository.TeamTemplateMemberRepository
import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class GetTeamTemplateStatsHandler(
    private val teamTemplateRepository: TeamTemplateRepository,
    private val teamTemplateMemberRepository: TeamTemplateMemberRepository,
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository
) {
    fun handle(id: UUID): TeamTemplateStatsResponse {
        val template = teamTemplateRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team template not found") }

        val members = teamTemplateMemberRepository.findByTeamTemplateId(template.id)
        val playerIds = members.map { it.playerId }
        val playerMap = if (playerIds.isEmpty()) {
            emptyMap()
        } else {
            playerRepository.findAllById(playerIds).associateBy { it.id }
        }

        val eventsPlayed = teamRepository.countByTeamTemplateId(template.id)

        return TeamTemplateStatsResponse(
            teamTemplateId = template.id,
            teamTemplateName = template.name,
            eventsPlayed = eventsPlayed.toInt(),
            members = members.mapNotNull { member ->
                val player = playerMap[member.playerId]
                if (player != null) {
                    TeamTemplateMemberResponse(
                        playerId = member.playerId,
                        playerName = player.name ?: "Ukjent"
                    )
                } else null
            }
        )
    }
}
