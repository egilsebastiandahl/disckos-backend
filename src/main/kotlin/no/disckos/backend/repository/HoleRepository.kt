package no.disckos.backend.repository

import no.disckos.backend.application.rank.RoundHoleCount
import no.disckos.backend.domain.HoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface HoleRepository : JpaRepository<HoleEntity, UUID> {
    fun findByRoundId(roundId: UUID): List<HoleEntity>
    fun findByRoundIdIn(roundIds: Collection<UUID>): List<HoleEntity>
    fun deleteByRoundId(roundId: UUID)
    fun findByRoundIdAndHoleNumber(roundId: UUID, holeNumber: Int): HoleEntity?

    @Query(
        """
        SELECT new no.disckos.backend.application.rank.RoundHoleCount(h.roundId, COUNT(h))
        FROM HoleEntity h
        WHERE h.roundId IN :roundIds
        GROUP BY h.roundId
        """
    )
    fun countHolesByRoundIds(@Param("roundIds") roundIds: Collection<UUID>): List<RoundHoleCount>
}
