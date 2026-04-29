package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.domain.TeamTemplateMemberEntity
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.TeamTemplateMemberRepository
import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class AddTeamTemplateMemberHandler(
    private val teamTemplateRepository: TeamTemplateRepository,
    private val teamTemplateMemberRepository: TeamTemplateMemberRepository,
    private val playerRepository: PlayerRepository
) {
    @Transactional
    fun handle(teamTemplateId: UUID, playerId: UUID): TeamTemplateMemberEntity {
        if (!teamTemplateRepository.existsById(teamTemplateId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Team template not found")
        }
        if (!playerRepository.existsById(playerId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Player not found")
        }
        if (teamTemplateMemberRepository.existsByTeamTemplateIdAndPlayerId(teamTemplateId, playerId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Player is already a member of this team")
        }

        return teamTemplateMemberRepository.save(
            TeamTemplateMemberEntity(
                teamTemplateId = teamTemplateId,
                playerId = playerId
            )
        )
    }
}
