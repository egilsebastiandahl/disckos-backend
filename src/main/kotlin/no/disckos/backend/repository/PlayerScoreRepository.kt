package no.disckos.backend.repository

import no.disckos.backend.application.player.PlayerScoreRow
import no.disckos.backend.application.rank.RankScoreRow
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

    // Event rounds take date and location from the event; standalone rounds
    // (event_id null) carry their own — see migration 015. Only real scores:
    // the live scorecard stores 0 for holes not scored yet.
    @Query(
        """
        SELECT new no.disckos.backend.application.player.PlayerScoreRow(
            ps.throws, h.par, h.roundId, r.eventId,
            COALESCE(r.playedAt, e.date), COALESCE(r.locationId, e.locationId), l.name
        )
        FROM PlayerScoreEntity ps
        JOIN HoleEntity h ON h.id = ps.holeId
        JOIN RoundEntity r ON r.id = h.roundId
        LEFT JOIN EventEntity e ON e.id = r.eventId
        LEFT JOIN LocationEntity l ON l.id = COALESCE(r.locationId, e.locationId)
        WHERE ps.playerId = :playerId AND ps.throws > 0
        """
    )
    fun findScoreRowsByPlayerId(@Param("playerId") playerId: UUID): List<PlayerScoreRow>

    // Ranks: individual stroke-play rounds only, and only real scores (the live
    // scorecard stores 0 for holes not scored yet). Completeness is checked in
    // CompletedRoundLoader against the round's hole count.
    @Query(
        """
        SELECT new no.disckos.backend.application.rank.RankScoreRow(
            ps.playerId, ps.throws, h.par, h.roundId,
            COALESCE(r.playedAt, e.date), COALESCE(r.locationId, e.locationId)
        )
        FROM PlayerScoreEntity ps
        JOIN HoleEntity h ON h.id = ps.holeId
        JOIN RoundEntity r ON r.id = h.roundId
        LEFT JOIN EventEntity e ON e.id = r.eventId
        WHERE r.eventType = no.disckos.backend.domain.EventType.individual
          AND r.scoringFormat = no.disckos.backend.domain.ScoringFormat.stroke
          AND ps.throws > 0
        """
    )
    fun findRankRows(): List<RankScoreRow>

    @Query(
        """
        SELECT new no.disckos.backend.application.rank.RankScoreRow(
            ps.playerId, ps.throws, h.par, h.roundId,
            COALESCE(r.playedAt, e.date), COALESCE(r.locationId, e.locationId)
        )
        FROM PlayerScoreEntity ps
        JOIN HoleEntity h ON h.id = ps.holeId
        JOIN RoundEntity r ON r.id = h.roundId
        LEFT JOIN EventEntity e ON e.id = r.eventId
        WHERE ps.playerId = :playerId
          AND r.eventType = no.disckos.backend.domain.EventType.individual
          AND r.scoringFormat = no.disckos.backend.domain.ScoringFormat.stroke
          AND ps.throws > 0
        """
    )
    fun findRankRowsByPlayerId(@Param("playerId") playerId: UUID): List<RankScoreRow>
}
