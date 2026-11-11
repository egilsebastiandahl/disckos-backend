package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.round.DeleteRoundHandler
import no.disckos.backend.application.rating.RatingInputLoader
import no.disckos.backend.domain.ConfirmationStatus
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.MemberUploadEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.Profile
import no.disckos.backend.domain.ScoreConfirmationEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.MemberUploadRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.repository.ScoreConfirmationRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class MemberUploadServicesTest {

    private val kariProfile = UUID.randomUUID()
    private val kari = UUID.randomUUID()
    private val olaProfile = UUID.randomUUID()
    private val ola = UUID.randomUUID()
    private val adminProfile = UUID.randomUUID()
    private val round = UUID.randomUUID()
    private val holes = (1..3).map { HoleEntity(roundId = round, holeNumber = it, par = 3) }

    private val confirmationRepository = mock<ScoreConfirmationRepository>()
    private val uploadRepository = mock<MemberUploadRepository>()
    private val holeRepository = mock<HoleRepository> { on { findByRoundId(round) } doReturn holes.reversed() }
    private val playerScoreRepository = mock<PlayerScoreRepository>()
    private val playerRepository = mock<PlayerRepository>()
    private val profileRepository = mock<ProfileRepository> {
        on { findById(kariProfile) } doReturn Optional.of(profile(kariProfile, kari))
        on { findById(olaProfile) } doReturn Optional.of(profile(olaProfile, ola))
    }
    private val roundSummaries = mock<RoundSummaries> {
        on { load(any()) } doReturn mapOf(round to RoundSummary(round, "Holmenkollen", null, null, listOf(3, 3, 3)))
    }
    private val deleteRoundHandler = mock<DeleteRoundHandler>()

    private val confirmations = ScoreConfirmationService(
        confirmationRepository, uploadRepository, holeRepository, playerScoreRepository, playerRepository, profileRepository, roundSummaries,
    )
    private val uploads = MemberUploadService(
        uploadRepository, confirmationRepository, holeRepository, playerScoreRepository, playerRepository, profileRepository,
        roundSummaries, deleteRoundHandler, mock<RatingInputLoader>(),
    )

    private fun profile(id: UUID, playerId: UUID) = Profile().apply {
        this.id = id
        player = Player().apply { this.id = playerId }
    }

    private fun pendingFor(player: UUID, uploadId: UUID? = null) =
        ScoreConfirmationEntity(uploadId = uploadId, roundId = round, playerId = player, udiscName = "Kari", throws = "2,3,4").also {
            whenever(confirmationRepository.findById(it.id)).thenReturn(Optional.of(it))
        }

    private fun status(ex: ResponseStatusException) = HttpStatus.valueOf(ex.statusCode.value())

    // ---- Confirmations ------------------------------------------------------------------

    @Test
    fun `confirming your score adds it to the round, hole by hole`() {
        val pending = pendingFor(kari)

        val view = confirmations.decide(pending.id, confirm = true, actorProfileId = kariProfile, asAdmin = false)

        val saved = argumentCaptor<List<PlayerScoreEntity>>()
        verify(playerScoreRepository).saveAll(saved.capture())
        assertEquals(holes.map { it.id }, saved.firstValue.map { it.holeId })
        assertEquals(listOf(2, 3, 4), saved.firstValue.map { it.throws })
        assertEquals(setOf(kari), saved.firstValue.map { it.playerId }.toSet())
        assertEquals(ConfirmationStatus.confirmed, view.status)
        assertEquals(kariProfile, pending.decidedBy)
        assertEquals(0, view.toPar)
    }

    @Test
    fun `rejecting leaves the round alone`() {
        val pending = pendingFor(kari)

        confirmations.decide(pending.id, confirm = false, actorProfileId = kariProfile, asAdmin = false)

        verify(playerScoreRepository, never()).saveAll(any<List<PlayerScoreEntity>>())
        assertEquals(ConfirmationStatus.rejected, pending.status)
    }

    @Test
    fun `you can't confirm someone else's score`() {
        val pending = pendingFor(kari)

        val ex = assertThrows<ResponseStatusException> { confirmations.decide(pending.id, true, olaProfile, asAdmin = false) }

        assertEquals(HttpStatus.FORBIDDEN, status(ex))
        assertEquals(ConfirmationStatus.pending, pending.status)
        verify(playerScoreRepository, never()).saveAll(any<List<PlayerScoreEntity>>())
    }

    @Test
    fun `an admin can confirm for anyone`() {
        val pending = pendingFor(kari)

        confirmations.decide(pending.id, confirm = true, actorProfileId = adminProfile, asAdmin = true)

        assertEquals(ConfirmationStatus.confirmed, pending.status)
        assertEquals(adminProfile, pending.decidedBy)
    }

    @Test
    fun `a decision can't be changed afterwards`() {
        val pending = pendingFor(kari).apply { status = ConfirmationStatus.rejected }

        val ex = assertThrows<ResponseStatusException> { confirmations.decide(pending.id, true, kariProfile, asAdmin = false) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
    }

    @Test
    fun `confirming a score that's already in the round adds nothing`() {
        val pending = pendingFor(kari)
        whenever(playerScoreRepository.existsByRoundIdAndPlayerId(round, kari)).thenReturn(true)

        confirmations.decide(pending.id, confirm = true, actorProfileId = kariProfile, asAdmin = false)

        verify(playerScoreRepository, never()).saveAll(any<List<PlayerScoreEntity>>())
        assertEquals(ConfirmationStatus.confirmed, pending.status)
    }

    // ---- Undo ---------------------------------------------------------------------------

    private fun uploadBy(profileId: UUID, playerId: UUID) =
        MemberUploadEntity(roundId = round, profileId = profileId, playerId = playerId, cardKey = "k").also {
            whenever(uploadRepository.findById(it.id)).thenReturn(Optional.of(it))
            whenever(uploadRepository.findByRoundId(round)).thenReturn(listOf(it))
        }

    @Test
    fun `you can't undo someone else's upload`() {
        val upload = uploadBy(kariProfile, kari)

        val ex = assertThrows<ResponseStatusException> { uploads.undo(upload.id, olaProfile, asAdmin = false) }

        assertEquals(HttpStatus.FORBIDDEN, status(ex))
        verify(deleteRoundHandler, never()).handle(any())
    }

    @Test
    fun `undoing the only score in a round deletes the round`() {
        val upload = uploadBy(kariProfile, kari)
        whenever(playerScoreRepository.findRoundPlayers(listOf(round))).thenReturn(listOf(RoundPlayer(round, kari)))
        // Kari asked Ola, who hasn't answered: that request goes with her upload.
        val askedOla = pendingFor(ola, upload.id)
        whenever(confirmationRepository.findByRoundIdIn(listOf(round))).thenReturn(listOf(askedOla))

        uploads.undo(upload.id, kariProfile, asAdmin = false)

        verify(deleteRoundHandler).handle(round)
    }

    @Test
    fun `undoing keeps scores others have confirmed`() {
        val upload = uploadBy(kariProfile, kari)
        whenever(playerScoreRepository.findRoundPlayers(listOf(round))).thenReturn(listOf(RoundPlayer(round, kari), RoundPlayer(round, ola)))

        uploads.undo(upload.id, adminProfile, asAdmin = true)

        verify(deleteRoundHandler, never()).handle(any())
        verify(playerScoreRepository).deleteByHoleIdInAndPlayerId(holes.reversed().map { it.id }, kari)
        verify(uploadRepository).delete(upload)
    }
}
