package no.disckos.backend.repository

import no.disckos.backend.domain.TeamMemberScoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeamMemberScoreRepository : JpaRepository<TeamMemberScoreEntity, UUID> {
    fun findByTeamScoreId(teamScoreId: UUID): List<TeamMemberScoreEntity>
    fun findByTeamScoreIdIn(teamScoreIds: Collection<UUID>): List<TeamMemberScoreEntity>
    fun deleteByTeamScoreIdIn(teamScoreIds: Collection<UUID>)
    fun findByTeamScoreIdAndPlayerId(teamScoreId: UUID, playerId: UUID): TeamMemberScoreEntity?
}
