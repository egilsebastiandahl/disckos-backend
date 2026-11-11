package no.disckos.backend.application.memberimport

import no.disckos.backend.domain.ConfirmationStatus
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.ScoreConfirmationEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.MemberUploadRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.repository.ScoreConfirmationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/** A score waiting for its player (or an admin) to confirm it. */
data class ConfirmationView(
    val id: UUID,
    val roundId: UUID,
    val playerId: UUID,
    val playerName: String?,
    /** As written on the UDisc card. */
    val udiscName: String,
    val locationName: String?,
    val layoutName: String?,
    val playedAt: OffsetDateTime?,
    val pars: List<Int>,
    val throws: List<Int>,
    val strokes: Int,
    val toPar: Int,
    /** The player whose upload asked for this. */
    val uploadedBy: String?,
    val status: ConfirmationStatus,
    val createdAt: OffsetDateTime?,
)

/**
 * Confirming or rejecting a score someone else uploaded for you. Confirming copies the throws
 * into the round, from then on it counts like any other score. Members decide for themselves;
 * admins can decide for anyone, e.g. players without an account.
 */
@Component
class ScoreConfirmationService(
    private val confirmationRepository: ScoreConfirmationRepository,
    private val uploadRepository: MemberUploadRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val playerRepository: PlayerRepository,
    private val profileRepository: ProfileRepository,
    private val roundSummaries: RoundSummaries,
) {
    @Transactional(readOnly = true)
    fun pendingFor(playerId: UUID): List<ConfirmationView> =
        views(confirmationRepository.findByPlayerIdAndStatusOrderByCreatedAtAsc(playerId, ConfirmationStatus.pending))

    @Transactional(readOnly = true)
    fun pendingForEveryone(): List<ConfirmationView> =
        views(confirmationRepository.findByStatusOrderByCreatedAtAsc(ConfirmationStatus.pending))

    /**
     * @param actorProfileId who decides; recorded on the confirmation.
     * @param asAdmin may decide for any player; otherwise only for the actor's own player.
     */
    @Transactional
    fun decide(confirmationId: UUID, confirm: Boolean, actorProfileId: UUID, asAdmin: Boolean): ConfirmationView {
        val confirmation = confirmationRepository.findById(confirmationId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke runden som skulle bekreftes.") }
        if (!asAdmin) {
            val myPlayer = profileRepository.findById(actorProfileId).orElse(null)?.player?.id
            if (myPlayer == null || myPlayer != confirmation.playerId) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Du kan bare bekrefte dine egne runder.")
            }
        }
        if (confirmation.status != ConfirmationStatus.pending) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Denne runden er allerede ${confirmation.status.norwegian()}.")
        }

        if (confirm) addScores(confirmation)
        confirmation.status = if (confirm) ConfirmationStatus.confirmed else ConfirmationStatus.rejected
        confirmation.decidedBy = actorProfileId
        confirmation.decidedAt = OffsetDateTime.now()
        confirmationRepository.save(confirmation)
        return views(listOf(confirmation)).single()
    }

    private fun addScores(confirmation: ScoreConfirmationEntity) {
        // Already there, e.g. an admin imported the same card: nothing to add.
        if (playerScoreRepository.existsByRoundIdAndPlayerId(confirmation.roundId, confirmation.playerId)) return
        val holes = holeRepository.findByRoundId(confirmation.roundId).sortedBy { it.holeNumber }
        val throws = confirmation.throwsList
        if (holes.size != throws.size) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Runden har endret seg siden den ble lastet opp. Be en admin se på den.")
        }
        playerScoreRepository.saveAll(
            holes.zip(throws).map { (hole, t) -> PlayerScoreEntity(holeId = hole.id, playerId = confirmation.playerId, throws = t) },
        )
    }

    private fun views(confirmations: List<ScoreConfirmationEntity>): List<ConfirmationView> {
        if (confirmations.isEmpty()) return emptyList()
        val rounds = roundSummaries.load(confirmations.map { it.roundId })
        val uploads = uploadRepository.findAllById(confirmations.mapNotNull { it.uploadId }.toSet()).associateBy { it.id }
        val playerNames = playerRepository.findAllById(
            (confirmations.map { it.playerId } + uploads.values.map { it.playerId }).toSet(),
        ).associate { it.id to it.name }

        return confirmations.mapNotNull { c ->
            val round = rounds[c.roundId] ?: return@mapNotNull null
            val throws = c.throwsList
            ConfirmationView(
                id = c.id,
                roundId = c.roundId,
                playerId = c.playerId,
                playerName = playerNames[c.playerId],
                udiscName = c.udiscName,
                locationName = round.locationName,
                layoutName = round.layoutName,
                playedAt = round.playedAt,
                pars = round.pars,
                throws = throws,
                strokes = throws.sum(),
                toPar = throws.sum() - round.pars.sum(),
                uploadedBy = c.uploadId?.let { uploads[it] }?.let { playerNames[it.playerId] },
                status = c.status,
                createdAt = c.createdAt,
            )
        }
    }

    private fun ConfirmationStatus.norwegian() = when (this) {
        ConfirmationStatus.pending -> "til behandling"
        ConfirmationStatus.confirmed -> "bekreftet"
        ConfirmationStatus.rejected -> "avvist"
    }
}
