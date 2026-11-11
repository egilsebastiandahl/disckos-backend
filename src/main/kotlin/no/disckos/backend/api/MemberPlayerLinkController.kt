package no.disckos.backend.api

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import no.disckos.backend.application.playerlink.LinkStatus
import no.disckos.backend.application.playerlink.NameCheck
import no.disckos.backend.application.playerlink.PlayerLinkService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class PlayerNameRequest(
    @field:NotBlank @field:Size(max = 200) val name: String,
    /** "male", "female" or "other"; only used when creating. */
    val gender: String? = null,
)

data class ClaimPlayerRequest(val playerId: UUID)

/** A member choosing who they are in Disckos: create a new player, or ask to be linked to an existing one. */
@RestController
@RequestMapping("/api/member/player-link")
@PreAuthorize("hasRole('USER')")
class MemberPlayerLinkController(private val playerLinkService: PlayerLinkService) {

    @GetMapping
    fun status(): LinkStatus = playerLinkService.status(me())

    /** Players with this name or one like it, so the member can claim instead of creating a duplicate. */
    @PostMapping("/check")
    fun check(@Valid @RequestBody body: PlayerNameRequest): NameCheck = playerLinkService.checkName(body.name)

    @PostMapping("/create")
    fun create(@Valid @RequestBody body: PlayerNameRequest): LinkStatus =
        playerLinkService.createOwnPlayer(me(), body.name, body.gender)

    @PostMapping("/request")
    fun request(@RequestBody body: ClaimPlayerRequest): LinkStatus =
        playerLinkService.requestExisting(me(), body.playerId, asAdmin = hasRole("ROLE_admin"))

    @DeleteMapping("/request")
    fun cancel(): LinkStatus = playerLinkService.cancelRequest(me())

    private fun hasRole(role: String) =
        SecurityContextHolder.getContext().authentication?.authorities?.any { it.authority == role } == true

    private fun me(): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? String
        return principal?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ikke innlogget som medlem.")
    }
}
