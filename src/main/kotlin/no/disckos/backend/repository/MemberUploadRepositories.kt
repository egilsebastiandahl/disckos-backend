package no.disckos.backend.repository

import no.disckos.backend.domain.ConfirmationStatus
import no.disckos.backend.domain.MemberUploadEntity
import no.disckos.backend.domain.ScoreConfirmationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberUploadRepository : JpaRepository<MemberUploadEntity, UUID> {
    fun findByProfileIdOrderByCreatedAtDesc(profileId: UUID): List<MemberUploadEntity>
    fun findTop100ByOrderByCreatedAtDesc(): List<MemberUploadEntity>
    fun findByRoundId(roundId: UUID): List<MemberUploadEntity>
}

interface ScoreConfirmationRepository : JpaRepository<ScoreConfirmationEntity, UUID> {
    fun findByPlayerIdAndStatusOrderByCreatedAtAsc(playerId: UUID, status: ConfirmationStatus): List<ScoreConfirmationEntity>
    fun findByStatusOrderByCreatedAtAsc(status: ConfirmationStatus): List<ScoreConfirmationEntity>
    fun findByUploadIdIn(uploadIds: Collection<UUID>): List<ScoreConfirmationEntity>
    fun findByRoundIdIn(roundIds: Collection<UUID>): List<ScoreConfirmationEntity>
    fun findByRoundIdAndPlayerIdIn(roundId: UUID, playerIds: Collection<UUID>): List<ScoreConfirmationEntity>
}
