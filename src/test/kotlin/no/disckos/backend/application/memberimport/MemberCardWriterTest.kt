package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.UdiscCard
import no.disckos.backend.application.admin.udiscimport.UdiscPlayerRow
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.MemberUploadEntity
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoreConfirmationEntity
import no.disckos.backend.domain.UdiscImportedCardEntity
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.MemberUploadRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.ScoreConfirmationRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberCardWriterTest {

    private val me = Uploader(UUID.randomUUID(), UUID.randomUUID(), "Sebastian", null)
    private val kari = UUID.randomUUID()
    private val location = UUID.randomUUID()

    private val roundRepository = mock<RoundRepository> { on { save(any<RoundEntity>()) } doAnswer { it.arguments[0] as RoundEntity } }
    @Suppress("UNCHECKED_CAST")
    private val holeRepository = mock<HoleRepository> {
        on { saveAll(any<List<HoleEntity>>()) } doAnswer { it.arguments[0] as List<HoleEntity> }
    }
    private val playerScoreRepository = mock<PlayerScoreRepository>()
    private val importedCardRepository = mock<UdiscImportedCardRepository>()
    private val uploadRepository = mock<MemberUploadRepository> { on { save(any<MemberUploadEntity>()) } doAnswer { it.arguments[0] as MemberUploadEntity } }
    private val confirmationRepository = mock<ScoreConfirmationRepository>()

    private val writer = MemberCardWriter(
        roundRepository, holeRepository, playerScoreRepository, importedCardRepository, uploadRepository, confirmationRepository,
    )

    private val card = UdiscCard(
        key = "Holmenkollen|Main|2026-10-04 1400+0200",
        courseName = "Holmenkollen",
        layoutName = "Main",
        rawDate = "2026-10-04 1400+0200",
        startedAt = LocalDateTime.of(2026, 10, 4, 14, 0),
        pars = listOf(3, 3, 4),
        players = listOf(UdiscPlayerRow("Seb D", listOf(2, 3, 4)), UdiscPlayerRow("Kari", listOf(3, 3, 5))),
    )

    private val plan = MemberCardPlan(
        card = card,
        status = MemberCardStatus.READY,
        locationId = location,
        ownThrows = listOf(2, 3, 4),
        others = listOf(
            OtherPlayerPlan("Kari", kari, "Kari Hansen", listOf(3, 3, 5), OtherPlayerStatus.ASK),
            OtherPlayerPlan("Tante Grete", null, null, listOf(5, 5, 5), OtherPlayerStatus.GUEST),
        ),
    )

    @Test
    fun `a new card becomes a standalone round with your score and a request for Kari's`() {
        val result = writer.write(plan, me)

        val round = argumentCaptor<RoundEntity>().also { verify(roundRepository).save(it.capture()) }.firstValue
        assertEquals(null, round.eventId)
        assertEquals(location, round.locationId)
        assertEquals("Main", round.layoutName)
        assertTrue(result.createdRound)

        val scores = argumentCaptor<List<PlayerScoreEntity>>().also { verify(playerScoreRepository).saveAll(it.capture()) }.firstValue
        assertEquals(listOf(2, 3, 4), scores.map { it.throws })
        assertEquals(setOf(me.playerId), scores.map { it.playerId }.toSet())

        val card = argumentCaptor<UdiscImportedCardEntity>().also { verify(importedCardRepository).save(it.capture()) }.firstValue
        assertEquals(round.id, card.roundId)

        val asked = argumentCaptor<List<ScoreConfirmationEntity>>().also { verify(confirmationRepository).saveAll(it.capture()) }.firstValue
        // The guest is never stored.
        assertEquals(listOf(kari), asked.map { it.playerId })
        assertEquals(listOf(3, 3, 5), asked.single().throwsList)
        assertEquals(listOf("Kari Hansen"), result.askedPlayers)
    }

    @Test
    fun `a card someone uploaded a moment ago isn't turned into a second round`() {
        val importedCardRepository = mock<UdiscImportedCardRepository> { on { existsById(card.key) } doReturn true }
        val writer = MemberCardWriter(
            roundRepository, holeRepository, playerScoreRepository, importedCardRepository, uploadRepository, confirmationRepository,
        )

        assertThrows<ResponseStatusException> { writer.write(plan, me) }

        verify(roundRepository, never()).save(any<RoundEntity>())
    }

    @Test
    fun `a card that isn't ready is never written`() {
        assertThrows<IllegalArgumentException> { writer.write(plan.copy(status = MemberCardStatus.EVENT_DAY), me) }
    }
}
