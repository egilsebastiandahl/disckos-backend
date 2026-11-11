package no.disckos.backend.application.memberimport

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.MemberUploadEntity
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoreConfirmationEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscImportedCardEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.MemberUploadRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.ScoreConfirmationRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId
import java.util.UUID

data class MemberCardResult(
    val key: String,
    val roundId: UUID?,
    val createdRound: Boolean,
    /** Club players asked to confirm their score. */
    val askedPlayers: List<String>,
    val error: String?,
)

/**
 * Writes one planned card, in its own transaction: the uploader's score, a record of the
 * upload, and a confirmation request for each club player the plan says to ask. A double
 * submit fails on the database's unique keys (card, upload per player per round, score per
 * hole per player) instead of writing anything twice.
 */
@Component
class MemberCardWriter(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val importedCardRepository: UdiscImportedCardRepository,
    private val uploadRepository: MemberUploadRepository,
    private val confirmationRepository: ScoreConfirmationRepository,
) {
    private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

    @Transactional
    fun write(plan: MemberCardPlan, uploader: Uploader): MemberCardResult {
        require(plan.status == MemberCardStatus.READY) { "Card ${plan.card.key} is not ready: ${plan.status}" }
        val card = plan.card
        val ownThrows = plan.ownThrows!!

        val (round, holes, created) = if (plan.existingRoundId != null) {
            val round = roundRepository.findById(plan.existingRoundId).orElseThrow { stale() }
            val holes = holeRepository.findByRoundId(round.id).sortedBy { it.holeNumber }
            if (holes.size != card.holeCount) throw stale()
            Triple(round, holes, false)
        } else {
            // Someone else may have uploaded the same card since the plan was made.
            if (importedCardRepository.existsById(card.key)) throw stale()
            val round = roundRepository.save(
                RoundEntity(
                    eventId = null,
                    eventType = EventType.individual,
                    scoringFormat = ScoringFormat.stroke,
                    locationId = plan.locationId!!,
                    playedAt = card.startedAt!!.atZone(oslo).toOffsetDateTime(),
                    layoutName = card.layoutName.trim().takeIf(String::isNotEmpty),
                ),
            )
            val holes = holeRepository.saveAll(
                card.pars.mapIndexed { i, par -> HoleEntity(roundId = round.id, holeNumber = i + 1, par = par!!) },
            ).sortedBy { it.holeNumber }
            importedCardRepository.save(UdiscImportedCardEntity(cardKey = card.key, roundId = round.id))
            Triple(round, holes, true)
        }

        playerScoreRepository.saveAll(
            holes.mapIndexed { i, hole -> PlayerScoreEntity(holeId = hole.id, playerId = uploader.playerId, throws = ownThrows[i]) },
        )
        val upload = uploadRepository.save(
            MemberUploadEntity(roundId = round.id, profileId = uploader.profileId, playerId = uploader.playerId, cardKey = card.key),
        )

        val asked = plan.others.filter { it.status == OtherPlayerStatus.ASK }
        confirmationRepository.saveAll(
            asked.map {
                ScoreConfirmationEntity(
                    uploadId = upload.id,
                    roundId = round.id,
                    playerId = it.playerId!!,
                    udiscName = it.udiscName,
                    throws = ScoreConfirmationEntity.encodeThrows(it.throws.map { t -> t!! }),
                )
            },
        )

        return MemberCardResult(card.key, round.id, created, asked.map { it.playerName ?: it.udiscName }, error = null)
    }

    private fun stale() =
        ResponseStatusException(HttpStatus.CONFLICT, "Runden er endret siden du lastet opp filen. Last den opp på nytt.")
}
