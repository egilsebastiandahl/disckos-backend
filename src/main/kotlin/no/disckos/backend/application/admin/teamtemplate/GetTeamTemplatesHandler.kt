package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.api.dto.admin.teamtemplate.AdminTeamTemplateResponse
import no.disckos.backend.api.dto.admin.teamtemplate.TeamTemplateMemberResponse
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.TeamTemplateMemberRepository
import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetTeamTemplatesHandler(
    private val teamTemplateRepository: TeamTemplateRepository,
    private val teamTemplateMemberRepository: TeamTemplateMemberRepository,
    private val playerRepository: PlayerRepository
) {
    fun handle(): List<AdminTeamTemplateResponse> {
        val templates = teamTemplateRepository.findAll()
        val allMembers = templates.flatMap { t ->
            teamTemplateMemberRepository.findByTeamTemplateId(t.id)
        }
        val playerIds = allMembers.map { it.playerId }.distinct()
        val playerMap = if (playerIds.isEmpty()) {
            emptyMap()
        } else {
            playerRepository.findAllById(playerIds).associateBy { it.id }
        }

        return templates.map { template ->
            val members = allMembers
                .filter { it.teamTemplateId == template.id }
                .mapNotNull { member ->
                    val player = playerMap[member.playerId]
                    if (player != null) {
                        TeamTemplateMemberResponse(
                            playerId = member.playerId,
                            playerName = player.name ?: "Ukjent"
                        )
                    } else null
                }

            AdminTeamTemplateResponse(
                id = template.id,
                name = template.name,
                members = members
            )
        }
    }

    fun handleSingle(id: UUID): AdminTeamTemplateResponse? {
        val template = teamTemplateRepository.findById(id).orElse(null) ?: return null
        val members = teamTemplateMemberRepository.findByTeamTemplateId(template.id)
        val playerIds = members.map { it.playerId }
        val playerMap = if (playerIds.isEmpty()) {
            emptyMap()
        } else {
            playerRepository.findAllById(playerIds).associateBy { it.id }
        }

        return AdminTeamTemplateResponse(
            id = template.id,
            name = template.name,
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
