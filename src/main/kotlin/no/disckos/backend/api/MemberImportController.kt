package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.member.MemberImportCommitRequest
import no.disckos.backend.api.dto.member.MemberImportPreviewRequest
import no.disckos.backend.application.memberimport.ConfirmationView
import no.disckos.backend.application.memberimport.CourseRequestService
import no.disckos.backend.application.memberimport.HeldUploadView
import no.disckos.backend.application.memberimport.MemberImportCommitResult
import no.disckos.backend.application.memberimport.MemberImportContextLoader
import no.disckos.backend.application.memberimport.MemberImportHandler
import no.disckos.backend.application.memberimport.MemberImportPreview
import no.disckos.backend.application.memberimport.MemberUploadService
import no.disckos.backend.application.memberimport.MemberUploadView
import no.disckos.backend.application.memberimport.ScoreConfirmationService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * A signed-in member's UDisc uploads and the scores others uploaded for them.
 * Everything is scoped to the caller's own profile and player.
 */
@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasRole('USER')")
class MemberImportController(
    private val importHandler: MemberImportHandler,
    private val uploadService: MemberUploadService,
    private val confirmationService: ScoreConfirmationService,
    private val contextLoader: MemberImportContextLoader,
    private val courseRequestService: CourseRequestService,
) {
    @PostMapping("/import/udisc/preview")
    fun preview(@Valid @RequestBody request: MemberImportPreviewRequest): MemberImportPreview =
        importHandler.preview(me(), request.csv, request.uploaderName)

    @PostMapping("/import/udisc/commit")
    fun commit(@Valid @RequestBody request: MemberImportCommitRequest): MemberImportCommitResult =
        importHandler.commit(me(), request.csv, request.uploaderName, request.cardKeys.toSet())

    @GetMapping("/uploads")
    fun myUploads(): List<MemberUploadView> = uploadService.mine(me())

    @DeleteMapping("/uploads/{id}")
    fun undoUpload(@PathVariable id: UUID) = uploadService.undo(id, me(), asAdmin = false)

    /** The caller's rounds on courses waiting for an admin, and ones that were turned away. */
    @GetMapping("/held-uploads")
    fun myHeldUploads(): List<HeldUploadView> = courseRequestService.mine(me())

    @DeleteMapping("/held-uploads/{id}")
    fun removeHeldUpload(@PathVariable id: UUID) = courseRequestService.removeMine(id, me())

    /** Scores others uploaded for the caller, waiting for them to confirm. Empty without a linked player. */
    @GetMapping("/confirmations")
    fun myConfirmations(): List<ConfirmationView> =
        contextLoader.uploader(me())?.let { confirmationService.pendingFor(it.playerId) } ?: emptyList()

    @PostMapping("/confirmations/{id}/confirm")
    fun confirm(@PathVariable id: UUID): ConfirmationView = confirmationService.decide(id, true, me(), asAdmin = false)

    @PostMapping("/confirmations/{id}/reject")
    fun reject(@PathVariable id: UUID): ConfirmationView = confirmationService.decide(id, false, me(), asAdmin = false)

    private fun me(): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ikke innlogget.")
        return try {
            UUID.fromString(principal)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ikke innlogget som medlem.")
        }
    }
}
