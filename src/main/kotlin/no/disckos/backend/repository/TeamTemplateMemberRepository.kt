package no.disckos.backend.repository

import no.disckos.backend.domain.TeamTemplateMemberEntity
import no.disckos.backend.domain.TeamTemplateMemberId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeamTemplateMemberRepository : JpaRepository<TeamTemplateMemberEntity, TeamTemplateMemberId> {
    fun findByTeamTemplateId(teamTemplateId: UUID): List<TeamTemplateMemberEntity>
    fun deleteByTeamTemplateIdAndPlayerId(teamTemplateId: UUID, playerId: UUID)
    fun existsByTeamTemplateIdAndPlayerId(teamTemplateId: UUID, playerId: UUID): Boolean
}
