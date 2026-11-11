package no.disckos.backend.api

import no.disckos.backend.application.memberimport.ConfirmationView
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** Admins oversee member uploads: undo any upload, and confirm or reject scores on a player's behalf. */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('admin')")
class AdminMemberUploadController(
    private val uploadService: MemberUploadService,
    private val confirmationService: ScoreConfirmationService,
) {
    /** Recent member uploads, newest first, with unusually good rounds flagged. */
    @GetMapping("/member-uploads")
    fun recentUploads(): List<MemberUploadView> = uploadService.recent()

    @DeleteMapping("/member-uploads/{id}")
    fun undoUpload(@PathVariable id: UUID) = uploadService.undo(id, me(), asAdmin = true)

    /** Every score waiting for confirmation, oldest first. */
    @GetMapping("/score-confirmations")
    fun pendingConfirmations(): List<ConfirmationView> = confirmationService.pendingForEveryone()

    @PostMapping("/score-confirmations/{id}/confirm")
    fun confirm(@PathVariable id: UUID): ConfirmationView = confirmationService.decide(id, true, me(), asAdmin = true)

    @PostMapping("/score-confirmations/{id}/reject")
    fun reject(@PathVariable id: UUID): ConfirmationView = confirmationService.decide(id, false, me(), asAdmin = true)

    /** Admins sign in with Supabase, so the principal is their profile id. */
    private fun me(): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
        return principal?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Logg inn med Disckos-kontoen din for å gjøre dette.")
    }
}
