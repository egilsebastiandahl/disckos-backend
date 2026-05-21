package no.disckos.backend.repository

import no.disckos.backend.domain.TeamMemberScoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface TeamMemberScoreRepository : JpaRepository<TeamMemberScoreEntity, UUID> {
    fun findByTeamScoreId(teamScoreId: UUID): List<TeamMemberScoreEntity>
    fun findByTeamScoreIdIn(teamScoreIds: Collection<UUID>): List<TeamMemberScoreEntity>
    fun deleteByTeamScoreIdIn(teamScoreIds: Collection<UUID>)
    fun findByTeamScoreIdAndPlayerId(teamScoreId: UUID, playerId: UUID): TeamMemberScoreEntity?

    @Query(
        """
        SELECT CASE WHEN COUNT(tms) > 0 THEN true ELSE false END
        FROM TeamMemberScoreEntity tms
        JOIN TeamScoreEntity ts ON ts.id = tms.teamScoreId
        JOIN HoleEntity h ON h.id = ts.holeId
        WHERE h.roundId = :roundId AND tms.playerId = :playerId
        """
    )
    fun existsByRoundIdAndPlayerId(
        @Param("roundId") roundId: UUID,
        @Param("playerId") playerId: UUID
    ): Boolean
}
