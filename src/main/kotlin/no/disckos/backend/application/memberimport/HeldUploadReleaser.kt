package no.disckos.backend.application.memberimport

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Once an admin has created or linked a course, its held rounds go through the normal member
 * upload rules, planned again from scratch: the round may have reached Disckos another way in
 * the meantime, or turn out to be an event round. Each round is written (or turned away, with
 * a reason the uploader sees) in its own transaction.
 */
@Component
class HeldUploadReleaser(
    private val requestService: CourseRequestService,
    private val contextLoader: MemberImportContextLoader,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class Released(val released: Int, val notAdded: List<String>)

    fun release(requestId: UUID): Released {
        var released = 0
        val notAdded = mutableListOf<String>()
        for (held in requestService.heldCards(requestId)) {
            val why = try {
                releaseOne(held)
            } catch (ex: Exception) {
                log.error("Releasing held upload {} failed", held.heldId, ex)
                "Runden kunne ikke legges inn. Prøv å laste den opp på nytt."
            }
            if (why == null) {
                released++
            } else {
                requestService.discard(held.heldId, why)
                notAdded += "${held.card.startedAt?.toLocalDate()}: $why"
            }
        }
        return Released(released, notAdded)
    }

    /** Null when the round was added; otherwise why not. */
    private fun releaseOne(held: HeldCard): String? {
        val uploader = contextLoader.uploader(held.profileId)
        if (uploader == null || uploader.playerId != held.playerId) {
            return "Kontoen er ikke lenger koblet til spilleren runden ble lastet opp for."
        }
        val cards = listOf(held.card)
        val ctx = contextLoader.load(uploader, cards)
        MemberImportPlanner.uploaderNameProblem(held.uploaderName, cards, ctx)?.let { return it }
        val plan = MemberImportPlanner.plan(cards, held.uploaderName, ctx).single()
        if (plan.status != MemberCardStatus.READY) return reasonFor(plan.status)
        requestService.writeReleased(plan, uploader, held.heldId)
        return null
    }

    private fun reasonFor(status: MemberCardStatus): String = when (status) {
        MemberCardStatus.ALREADY_UPLOADED -> "Runden var allerede i Disckos."
        MemberCardStatus.EVENT_DAY -> "Runden ble spilt på en Disckos-dag på denne banen. En admin legger den inn i eventet."
        MemberCardStatus.IN_EVENT -> "Runden er allerede lagt inn i et event."
        MemberCardStatus.HOLE_MISMATCH -> "Runden finnes i Disckos med et annet antall hull."
        else -> "Runden kunne ikke legges inn ($status)."
    }
}
