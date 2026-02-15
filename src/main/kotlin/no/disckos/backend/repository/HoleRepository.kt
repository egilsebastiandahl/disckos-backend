package no.disckos.backend.repository

import no.disckos.backend.domain.HoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HoleRepository : JpaRepository<HoleEntity, UUID> {
    fun findByRoundId(roundId: UUID): List<HoleEntity>
    fun findByRoundIdIn(roundIds: Collection<UUID>): List<HoleEntity>
    fun deleteByRoundId(roundId: UUID)
    fun findByRoundIdAndHoleNumber(roundId: UUID, holeNumber: Int): HoleEntity?
}
