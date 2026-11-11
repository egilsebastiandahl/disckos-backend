package no.disckos.backend.repository

import no.disckos.backend.domain.CourseRequestEntity
import no.disckos.backend.domain.CourseRequestStatus
import no.disckos.backend.domain.HeldUploadEntity
import no.disckos.backend.domain.HeldUploadScoreEntity
import no.disckos.backend.domain.HeldUploadStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CourseRequestRepository : JpaRepository<CourseRequestEntity, UUID> {
    fun findByCourseKeyAndStatus(courseKey: String, status: CourseRequestStatus): CourseRequestEntity?
    fun findByStatusOrderByCreatedAtAsc(status: CourseRequestStatus): List<CourseRequestEntity>
}

interface HeldUploadRepository : JpaRepository<HeldUploadEntity, UUID> {
    fun findByCourseRequestIdAndStatus(courseRequestId: UUID, status: HeldUploadStatus): List<HeldUploadEntity>
    fun findByCourseRequestIdIn(courseRequestIds: Collection<UUID>): List<HeldUploadEntity>
    fun findByProfileIdOrderByCreatedAtDesc(profileId: UUID): List<HeldUploadEntity>
    fun findByCardKeyIn(cardKeys: Collection<String>): List<HeldUploadEntity>
    fun findByCardKeyAndPlayerId(cardKey: String, playerId: UUID): HeldUploadEntity?
}

interface HeldUploadScoreRepository : JpaRepository<HeldUploadScoreEntity, UUID> {
    fun findByHeldUploadIdIn(heldUploadIds: Collection<UUID>): List<HeldUploadScoreEntity>
}
