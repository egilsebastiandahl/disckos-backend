package no.disckos.backend.repository

import no.disckos.backend.domain.PlayerScoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerScoreRepository : JpaRepository<PlayerScoreEntity, UUID> {
    fun findByHoleId(holeId: UUID): List<PlayerScoreEntity>
    fun findByHoleIdIn(holeIds: Collection<UUID>): List<PlayerScoreEntity>
    fun deleteByHoleIdIn(holeIds: Collection<UUID>)
    fun findByHoleIdAndPlayerId(holeId: UUID, playerId: UUID): PlayerScoreEntity?
}
