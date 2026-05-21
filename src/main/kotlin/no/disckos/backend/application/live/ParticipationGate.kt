package no.disckos.backend.application.live

import no.disckos.backend.repository.EventSignupRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.TeamMemberScoreRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class ParticipationGate(
    private val roundRepository: RoundRepository,
    private val profileRepository: ProfileRepository,
    private val eventSignupRepository: EventSignupRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository,
) {

    fun verifyCanScore(roundId: UUID) {
        val profileId = currentProfileId()
        val round = roundRepository.findById(roundId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found") }

        if (eventSignupRepository.existsByEventIdAndProfileId(round.eventId, profileId)) {
            return
        }

        val profile = profileRepository.findById(profileId).orElse(null)
        val playerId = profile?.player?.id
        if (playerId != null) {
            val inIndividual = playerScoreRepository.existsByRoundIdAndPlayerId(roundId, playerId)
            val inTeam = teamMemberScoreRepository.existsByRoundIdAndPlayerId(roundId, playerId)
            if (inIndividual || inTeam) return
        }

        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Only event participants may update scores"
        )
    }

    private fun currentProfileId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")
        val principal = auth.principal?.toString()
        if (principal.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")
        }
        return try {
            UUID.fromString(principal)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal")
        }
    }
}
