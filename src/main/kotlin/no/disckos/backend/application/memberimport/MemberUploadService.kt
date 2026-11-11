package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.round.DeleteRoundHandler
import no.disckos.backend.application.rating.RatingInputLoader
import no.disckos.backend.application.rating.RatingModel
import no.disckos.backend.domain.ConfirmationStatus
import no.disckos.backend.domain.MemberUploadEntity
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

data class UploadConfirmationView(val playerName: String?, val udiscName: String, val status: ConfirmationStatus)

/** A round much better than the uploader usually plays, for an admin to glance at. */
data class UnlikelyRound(
    /** What the rating model expected, strokes to par per 18 holes. */
    val expectedToParPer18: Double,
    val actualToParPer18: Double,
)

data class MemberUploadView(
    val id: UUID,
    val roundId: UUID,
    val createdAt: OffsetDateTime?,
    /** The uploader's account name, when the profile still exists. */
    val uploaderProfileName: String?,
    val playerId: UUID,
    val playerName: String?,
    val locationName: String?,
    val layoutName: String?,
    val playedAt: OffsetDateTime?,
    val holes: Int,
    /** The uploader's score; null if it has been removed since (e.g. an admin edited the round). */
    val strokes: Int?,
    val toPar: Int?,
    val confirmations: List<UploadConfirmationView>,
    /** Admin list only. */
    val unlikely: UnlikelyRound? = null,
)

/**
 * Member uploads after the fact: the member's own list, the admin's list of recent uploads
 * (with unlikely rounds flagged), and undo.
 */
@Component
class MemberUploadService(
    private val uploadRepository: MemberUploadRepository,
    private val confirmationRepository: ScoreConfirmationRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val playerRepository: PlayerRepository,
    private val profileRepository: ProfileRepository,
    private val roundSummaries: RoundSummaries,
    private val deleteRoundHandler: DeleteRoundHandler,
    private val ratingInputLoader: RatingInputLoader,
) {
    @Transactional(readOnly = true)
    fun mine(profileId: UUID): List<MemberUploadView> =
        views(uploadRepository.findByProfileIdOrderByCreatedAtDesc(profileId))

    /** The latest uploads by anyone; the newest [FLAGGED_UPLOADS] are checked against the rating model. */
    @Transactional(readOnly = true)
    fun recent(): List<MemberUploadView> {
        val uploads = views(uploadRepository.findTop100ByOrderByCreatedAtDesc())
        if (uploads.isEmpty()) return uploads
        val input = ratingInputLoader.load()
        return uploads.mapIndexed { i, upload ->
            if (i >= FLAGGED_UPLOADS) return@mapIndexed upload
            val round = input.rounds.firstOrNull { it.roundId == upload.roundId && it.playerId == upload.playerId }
                ?: return@mapIndexed upload
            // What the model would have expected without this round.
            val fit = RatingModel.fit(input.rounds.filter { it.roundId != upload.roundId }, input.locationPriors)
            val expected = fit.expectedToParPer18(upload.playerId, round.course)
            if (round.toParPer18 <= expected - UNLIKELY_STROKES_PER_18) {
                upload.copy(unlikely = UnlikelyRound(expected, round.toParPer18))
            } else {
                upload
            }
        }
    }

    /**
     * Takes the upload back: removes the uploader's score and the confirmations it asked for.
     * Scores others have already confirmed stay. A round with nothing left in it is deleted.
     */
    @Transactional
    fun undo(uploadId: UUID, actorProfileId: UUID, asAdmin: Boolean) {
        val upload = uploadRepository.findById(uploadId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke opplastingen.") }
        if (!asAdmin && upload.profileId != actorProfileId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Du kan bare angre dine egne opplastinger.")
        }

        val roundId = upload.roundId
        val othersWithScores = playerScoreRepository.findRoundPlayers(listOf(roundId)).any { it.playerId != upload.playerId }
        val otherUploads = uploadRepository.findByRoundId(roundId).any { it.id != upload.id }
        val othersPending = confirmationRepository.findByRoundIdIn(listOf(roundId))
            .any { it.uploadId != upload.id && it.status == ConfirmationStatus.pending }

        if (!othersWithScores && !otherUploads && !othersPending) {
            // Nothing else in the round: delete it all (uploads, confirmations and the card record go with it).
            deleteRoundHandler.handle(roundId)
            return
        }
        val holeIds = holeRepository.findByRoundId(roundId).map { it.id }
        if (holeIds.isNotEmpty()) playerScoreRepository.deleteByHoleIdInAndPlayerId(holeIds, upload.playerId)
        confirmationRepository.deleteAll(confirmationRepository.findByUploadIdIn(listOf(upload.id)))
        uploadRepository.delete(upload)
    }

    private fun views(uploads: List<MemberUploadEntity>): List<MemberUploadView> {
        if (uploads.isEmpty()) return emptyList()
        val rounds = roundSummaries.load(uploads.map { it.roundId })
        val confirmations = confirmationRepository.findByUploadIdIn(uploads.map { it.id }).groupBy { it.uploadId }
        val playerNames = playerRepository.findAllById(
            (uploads.map { it.playerId } + confirmations.values.flatten().map { it.playerId }).toSet(),
        ).associate { it.id to it.name }
        val profileNames = profileRepository.findAllById(uploads.mapNotNull { it.profileId }.toSet())
            .associate { it.id to (it.displayName ?: it.username) }
        val strokes = ownStrokes(uploads)

        return uploads.mapNotNull { u ->
            val round = rounds[u.roundId] ?: return@mapNotNull null
            val own = strokes[u.roundId to u.playerId]
            MemberUploadView(
                id = u.id,
                roundId = u.roundId,
                createdAt = u.createdAt,
                uploaderProfileName = u.profileId?.let { profileNames[it] },
                playerId = u.playerId,
                playerName = playerNames[u.playerId],
                locationName = round.locationName,
                layoutName = round.layoutName,
                playedAt = round.playedAt,
                holes = round.pars.size,
                strokes = own,
                toPar = own?.let { it - round.pars.sum() },
                confirmations = confirmations[u.id].orEmpty().map {
                    UploadConfirmationView(playerNames[it.playerId], it.udiscName, it.status)
                },
            )
        }
    }

    /** Each uploader's total strokes in their uploaded round, when complete. */
    private fun ownStrokes(uploads: List<MemberUploadEntity>): Map<Pair<UUID, UUID>, Int> {
        val holes = holeRepository.findByRoundIdIn(uploads.map { it.roundId })
        val roundOfHole = holes.associate { it.id to it.roundId }
        val holeCount = holes.groupingBy { it.roundId }.eachCount()
        val uploaders = uploads.map { it.roundId to it.playerId }.toSet()
        return playerScoreRepository.findByHoleIdIn(holes.map { it.id })
            .filter { it.throws > 0 && (roundOfHole.getValue(it.holeId) to it.playerId) in uploaders }
            .groupBy { roundOfHole.getValue(it.holeId) to it.playerId }
            .filter { (key, scores) -> scores.size == holeCount[key.first] }
            .mapValues { (_, scores) -> scores.sumOf { it.throws } }
    }

    companion object {
        /** Better than expected by this many strokes per 18 holes: roughly 1 honest round in 50. */
        const val UNLIKELY_STROKES_PER_18 = 10.0

        /** Each check refits the rating model, so only the newest uploads are checked. */
        const val FLAGGED_UPLOADS = 50
    }
}
