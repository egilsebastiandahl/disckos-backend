package no.disckos.backend.repository

import no.disckos.backend.application.player.PlayerScoreRow
import no.disckos.backend.domain.PlayerScoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PlayerScoreRepository : JpaRepository<PlayerScoreEntity, UUID> {
    fun findByHoleId(holeId: UUID): List<PlayerScoreEntity>
    fun findByHoleIdIn(holeIds: Collection<UUID>): List<PlayerScoreEntity>
    fun deleteByHoleIdIn(holeIds: Collection<UUID>)
    fun findByHoleIdAndPlayerId(holeId: UUID, playerId: UUID): PlayerScoreEntity?

    @Query(
        """
        SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END
        FROM PlayerScoreEntity ps
        JOIN HoleEntity h ON h.id = ps.holeId
        WHERE h.roundId = :roundId AND ps.playerId = :playerId
        """
    )
    fun existsByRoundIdAndPlayerId(
        @Param("roundId") roundId: UUID,
        @Param("playerId") playerId: UUID
    ): Boolean

    @Query(
        """
        SELECT new no.disckos.backend.application.player.PlayerScoreRow(
            ps.throws, h.par, h.roundId, r.eventId, e.date, e.locationId, l.name
        )
        FROM PlayerScoreEntity ps
        JOIN HoleEntity h ON h.id = ps.holeId
        JOIN RoundEntity r ON r.id = h.roundId
        JOIN EventEntity e ON e.id = r.eventId
        LEFT JOIN LocationEntity l ON l.id = e.locationId
        WHERE ps.playerId = :playerId
        """
    )
    fun findScoreRowsByPlayerId(@Param("playerId") playerId: UUID): List<PlayerScoreRow>
}
