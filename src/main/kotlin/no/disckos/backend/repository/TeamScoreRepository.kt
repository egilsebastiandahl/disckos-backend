package no.disckos.backend.repository

import no.disckos.backend.domain.TeamScoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeamScoreRepository : JpaRepository<TeamScoreEntity, UUID> {
    fun findByHoleId(holeId: UUID): List<TeamScoreEntity>
    fun findByHoleIdIn(holeIds: Collection<UUID>): List<TeamScoreEntity>
    fun deleteByHoleIdIn(holeIds: Collection<UUID>)
    fun findByHoleIdAndTeamId(holeId: UUID, teamId: UUID): TeamScoreEntity?
}
