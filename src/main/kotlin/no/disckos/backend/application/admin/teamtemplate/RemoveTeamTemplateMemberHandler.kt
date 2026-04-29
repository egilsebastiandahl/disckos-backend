package no.disckos.backend.application.admin.teamtemplate

import no.disckos.backend.repository.TeamTemplateMemberRepository
import no.disckos.backend.repository.TeamTemplateRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class RemoveTeamTemplateMemberHandler(
    private val teamTemplateRepository: TeamTemplateRepository,
    private val teamTemplateMemberRepository: TeamTemplateMemberRepository
) {
    @Transactional
    fun handle(teamTemplateId: UUID, playerId: UUID) {
        if (!teamTemplateRepository.existsById(teamTemplateId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Team template not found")
        }
        if (!teamTemplateMemberRepository.existsByTeamTemplateIdAndPlayerId(teamTemplateId, playerId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Player is not a member of this team")
        }
        teamTemplateMemberRepository.deleteByTeamTemplateIdAndPlayerId(teamTemplateId, playerId)
    }
}
