package no.disckos.backend.api

import no.disckos.backend.application.playerlink.LinkRequestView
import no.disckos.backend.application.playerlink.PlayerLinkService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Members asking to be linked to an existing player, for an admin to approve or reject. */
@RestController
@RequestMapping("/admin/player-link-requests")
@PreAuthorize("hasRole('admin')")
class AdminPlayerLinkController(private val playerLinkService: PlayerLinkService) {

    @GetMapping
    fun pending(): List<LinkRequestView> = playerLinkService.pendingRequests()

    @PostMapping("/{id}/approve")
    fun approve(@PathVariable id: UUID) = playerLinkService.decide(id, approve = true, adminProfileId = me())

    @PostMapping("/{id}/reject")
    fun reject(@PathVariable id: UUID) = playerLinkService.decide(id, approve = false, adminProfileId = me())

    /** The deciding admin's profile; null for the legacy username/password admin login. */
    private fun me(): UUID? =
        (SecurityContextHolder.getContext().authentication?.principal as? String)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
