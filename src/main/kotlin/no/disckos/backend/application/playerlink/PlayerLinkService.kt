package no.disckos.backend.application.playerlink

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.domain.LinkRequestStatus
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerLinkRequestEntity
import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.PlayerLinkRequestRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.ProfileRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/** An existing player a member might be. */
data class PlayerOption(
    val id: UUID,
    val name: String,
    /** Linked to another account already, so it can't be claimed. */
    val claimed: Boolean,
    /** Rounds the player has: what claiming it would hand over. */
    val rounds: Int,
)

/** Players with the name a member wants, or one much like it. */
data class NameCheck(val exact: PlayerOption?, val similar: List<PlayerOption>)

data class PendingLink(val id: UUID, val playerId: UUID, val playerName: String?, val createdAt: OffsetDateTime?)

/** Who a member is in Disckos, and what they've asked for. */
data class LinkStatus(val playerId: UUID?, val playerName: String?, val pendingRequest: PendingLink?)

data class LinkRequestView(
    val id: UUID,
    val profileId: UUID,
    /** The account asking: display name or username. */
    val profileName: String?,
    /** The player the account is linked to now, if any. */
    val currentPlayerName: String?,
    val playerId: UUID,
    val playerName: String?,
    val playerRounds: Int,
    val createdAt: OffsetDateTime?,
)

/**
 * Who a member is in Disckos. That decides whose stats they see and whose score their UDisc
 * uploads add to, so:
 * - creating a brand-new player for yourself is instant (it has no history to take), unless a
 *   player with that exact name exists: then you're sent to claim that one instead;
 * - claiming an existing player is a request an admin approves, since it hands over the
 *   player's rounds (admins can claim directly);
 * - members can't unlink or switch on their own; that goes through an admin too.
 */
@Component
class PlayerLinkService(
    private val profileRepository: ProfileRepository,
    private val playerRepository: PlayerRepository,
    private val requestRepository: PlayerLinkRequestRepository,
    private val playerScoreRepository: PlayerScoreRepository,
) {
    @Transactional(readOnly = true)
    fun status(profileId: UUID): LinkStatus {
        val profile = profile(profileId)
        val pending = requestRepository.findByProfileIdAndStatus(profileId, LinkRequestStatus.pending)
        return LinkStatus(
            playerId = profile.player?.id,
            playerName = profile.player?.name,
            pendingRequest = pending?.let {
                PendingLink(it.id, it.playerId, playerRepository.findById(it.playerId).orElse(null)?.name, it.createdAt)
            },
        )
    }

    /** Existing players with this name, or a name that looks like it either way ("Kari" / "Kari Hansen"). */
    @Transactional(readOnly = true)
    fun checkName(name: String): NameCheck {
        val wanted = validName(name)
        val key = ImportNames.key(wanted)
        val claimed = claimedPlayerIds()
        val players = playerRepository.findAll().filter { it.id != null && !it.name.isNullOrBlank() }
        fun option(p: Player) = PlayerOption(p.id!!, p.name!!, p.id in claimed, playerScoreRepository.countRoundsByPlayerId(p.id!!).toInt())

        val exact = players.firstOrNull { ImportNames.key(it.name!!) == key }
        val similar = players
            .filter { it !== exact && (ImportNames.looksLikePlayer(wanted, it.name!!) || ImportNames.looksLikePlayer(it.name!!, wanted)) }
            .take(MAX_SIMILAR)
        return NameCheck(exact?.let(::option), similar.map(::option))
    }

    /** Creates a player called [name] and links the member to it at once. */
    @Transactional
    fun createOwnPlayer(profileId: UUID, name: String, gender: String?): LinkStatus {
        val profile = profile(profileId)
        profile.player?.let { throw conflict("Du er allerede koblet til ${it.name}.") }
        val wanted = validName(name)
        val taken = playerRepository.findAll().firstOrNull { !it.name.isNullOrBlank() && ImportNames.key(it.name!!) == ImportNames.key(wanted) }
        if (taken != null) {
            throw conflict("Det finnes allerede en spiller som heter «${taken.name}». Er det deg, be om å bli koblet til den.")
        }

        val now = OffsetDateTime.now()
        val player = playerRepository.save(
            Player().apply {
                id = UUID.randomUUID()
                this.name = wanted
                this.gender = gender?.takeIf { it in GENDERS } ?: "other"
                createdAt = now
                updatedAt = now
            },
        )
        profile.player = player
        profile.updatedAt = now
        profileRepository.save(profile)
        // Whatever they asked for before is moot now.
        requestRepository.findByProfileIdAndStatus(profileId, LinkRequestStatus.pending)?.let(requestRepository::delete)
        return status(profileId)
    }

    /**
     * Asks to be linked to an existing player. Replaces an earlier open request. Admins are
     * linked straight away, as they could do from the admin panel anyway.
     */
    @Transactional
    fun requestExisting(profileId: UUID, playerId: UUID, asAdmin: Boolean): LinkStatus {
        val profile = profile(profileId)
        val player = playerRepository.findById(playerId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke spilleren.") }
        if (profile.player?.id == playerId) return status(profileId)
        claimedBy(playerId)?.takeIf { it.id != profileId }?.let {
            throw conflict("${player.name} er allerede koblet til en annen konto. Er det feil, si fra til en admin.")
        }

        if (asAdmin) {
            link(profile, player)
            requestRepository.findByProfileIdAndStatus(profileId, LinkRequestStatus.pending)?.let(requestRepository::delete)
            return status(profileId)
        }
        val pending = requestRepository.findByProfileIdAndStatus(profileId, LinkRequestStatus.pending)
        if (pending != null) {
            pending.playerId = playerId
            requestRepository.save(pending)
        } else {
            requestRepository.save(PlayerLinkRequestEntity(profileId = profileId, playerId = playerId))
        }
        return status(profileId)
    }

    @Transactional
    fun cancelRequest(profileId: UUID): LinkStatus {
        requestRepository.findByProfileIdAndStatus(profileId, LinkRequestStatus.pending)?.let(requestRepository::delete)
        return status(profileId)
    }

    /** After an admin links or unlinks an account directly, its open request is settled. */
    @Transactional
    fun dropPending(profileId: UUID) {
        requestRepository.findByProfileIdAndStatus(profileId, LinkRequestStatus.pending)?.let(requestRepository::delete)
    }

    @Transactional(readOnly = true)
    fun pendingRequests(): List<LinkRequestView> {
        val requests = requestRepository.findByStatusOrderByCreatedAtAsc(LinkRequestStatus.pending)
        if (requests.isEmpty()) return emptyList()
        val profiles = profileRepository.findAllById(requests.map { it.profileId }.toSet()).associateBy { it.id }
        val players = playerRepository.findAllById(requests.map { it.playerId }.toSet()).associateBy { it.id }
        return requests.map { r ->
            val profile = profiles[r.profileId]
            LinkRequestView(
                id = r.id,
                profileId = r.profileId,
                profileName = profile?.let { it.displayName ?: it.username },
                currentPlayerName = profile?.player?.name,
                playerId = r.playerId,
                playerName = players[r.playerId]?.name,
                playerRounds = playerScoreRepository.countRoundsByPlayerId(r.playerId).toInt(),
                createdAt = r.createdAt,
            )
        }
    }

    @Transactional
    fun decide(requestId: UUID, approve: Boolean, adminProfileId: UUID?) {
        val request = requestRepository.findById(requestId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke forespørselen.") }
        if (request.status != LinkRequestStatus.pending) throw conflict("Forespørselen er allerede behandlet.")

        if (approve) {
            val profile = profile(request.profileId)
            val player = playerRepository.findById(request.playerId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Spilleren finnes ikke lenger.") }
            claimedBy(player.id!!)?.takeIf { it.id != profile.id }?.let {
                throw conflict("${player.name} er allerede koblet til en annen konto. Fjern den koblingen først.")
            }
            link(profile, player)
        }
        request.status = if (approve) LinkRequestStatus.approved else LinkRequestStatus.rejected
        request.decidedBy = adminProfileId
        request.decidedAt = OffsetDateTime.now()
        requestRepository.save(request)
    }

    private fun link(profile: Profile, player: Player) {
        profile.player = player
        profile.updatedAt = OffsetDateTime.now()
        profileRepository.save(profile)
    }

    private fun profile(profileId: UUID): Profile =
        profileRepository.findById(profileId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke profilen.") }

    private fun claimedBy(playerId: UUID): Profile? = profileRepository.findByPlayerId(playerId)

    private fun claimedPlayerIds(): Set<UUID> = profileRepository.findAll().mapNotNull { it.player?.id }.toSet()

    private fun validName(name: String): String {
        val trimmed = name.trim().replace(Regex("""\s+"""), " ")
        if (trimmed.length !in 2..MAX_NAME) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Navnet må være mellom 2 og $MAX_NAME tegn.")
        return trimmed
    }

    private fun conflict(message: String) = ResponseStatusException(HttpStatus.CONFLICT, message)

    companion object {
        const val MAX_NAME = 60
        const val MAX_SIMILAR = 8
        val GENDERS = setOf("male", "female", "other")
    }
}
