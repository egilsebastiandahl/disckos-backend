package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscImportedCardEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UdiscCardWriterTest {

    private val eventRepository = mock<EventRepository>()
    private val roundRepository = mock<RoundRepository>()
    private val holeRepository = mock<HoleRepository>()
    private val playerRepository = mock<PlayerRepository>()
    private val playerScoreRepository = mock<PlayerScoreRepository>()
    private val playerAliasRepository = mock<UdiscPlayerAliasRepository>()
    private val courseAliasRepository = mock<UdiscCourseAliasRepository>()
    private val importedCardRepository = mock<UdiscImportedCardRepository>()

    private val writer = UdiscCardWriter(
        eventRepository, roundRepository, holeRepository, playerRepository,
        playerScoreRepository, playerAliasRepository, courseAliasRepository, importedCardRepository,
    )

    private val locationId = UUID.randomUUID()
    private val event = event(teamEvent = false)
    private val seb = UUID.randomUUID()
    private val ola = UUID.randomUUID()
    private val playedAt = OffsetDateTime.parse("2026-09-04T17:09:00+02:00")

    @BeforeEach
    fun setUp() {
        whenever(eventRepository.findById(event.id)).thenReturn(Optional.of(event))
        whenever(playerRepository.findAllById(any())).thenAnswer { inv ->
            (inv.arguments[0] as Iterable<*>).map { id -> Player().apply { this.id = id as UUID; name = "x" } }
        }
        whenever(roundRepository.save(any<RoundEntity>())).thenAnswer { it.arguments[0] }
        whenever(holeRepository.saveAll(any<Iterable<HoleEntity>>())).thenAnswer { (it.arguments[0] as Iterable<*>).toList() }
    }

    // ---- Event rounds ---------------------------------------------------------------

    @Test
    fun `creates the event round with holes and scores when it doesn't exist yet`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val result = writer.write(
            eventCard(
                ResolvedPlayer("Seb D", seb, listOf(3, 2, null)),
                ResolvedPlayer("Ola", ola, listOf(4, 3, 5)),
            ),
        )

        assertTrue(result.createdRound)
        assertNull(result.error)
        assertEquals(listOf("Seb D", "Ola"), result.importedPlayers)

        val round = argumentCaptor<RoundEntity>().also { verify(roundRepository).save(it.capture()) }.firstValue
        assertEquals(event.id, round.eventId)
        assertNull(round.locationId, "event rounds take their location from the event")

        val holes = argumentCaptor<Iterable<HoleEntity>>().also { verify(holeRepository).saveAll(it.capture()) }.firstValue.toList()
        assertEquals(listOf(1, 2, 3), holes.map { it.holeNumber })
        assertEquals(listOf(3, 3, 4), holes.map { it.par })

        val scores = savedScores()
        assertEquals(5, scores.size, "Seb's unplayed hole 3 is left out")
        assertEquals(listOf(3, 2), scores.filter { it.playerId == seb }.map { it.throws })

        verify(importedCardRepository).save(argThat<UdiscImportedCardEntity> { cardKey == KEY && roundId == result.roundId })
    }

    @Test
    fun `remembers player and course links for next time`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        writer.write(eventCard(ResolvedPlayer("  Seb  D ", seb, listOf(3, 3, 3))))

        verify(playerAliasRepository).save(argThat<UdiscPlayerAliasEntity> { udiscName == "seb d" && playerId == seb })
        verify(courseAliasRepository).save(
            argThat<UdiscCourseAliasEntity> { udiscCourse == "ekebergsletta discgolfpark" && locationId == this@UdiscCardWriterTest.locationId },
        )
    }

    @Test
    fun `appends to the existing event round and skips players already in it`() {
        val round = individualRound(eventId = event.id)
        val holes = holes(round, 3)
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundId(round.id)).thenReturn(holes.reversed())
        whenever(playerScoreRepository.findByHoleIdIn(any()))
            .thenReturn(listOf(PlayerScoreEntity(holeId = holes[0].id, playerId = seb, throws = 3)))

        val result = writer.write(
            eventCard(
                ResolvedPlayer("Seb D", seb, listOf(3, 3, 3)),
                ResolvedPlayer("Ola", ola, listOf(4, 3, 5)),
            ),
        )

        assertFalse(result.createdRound)
        assertEquals(round.id, result.roundId)
        assertEquals(listOf("Ola"), result.importedPlayers)
        assertEquals(listOf("Seb D (allerede i runden)"), result.skippedPlayers)
        verify(roundRepository, never()).save(any<RoundEntity>())

        val scores = savedScores()
        assertEquals(holes.map { it.id }, scores.map { it.holeId }, "scores land on holes 1..3 in order")
        assertTrue(scores.all { it.playerId == ola })
    }

    @Test
    fun `does not create an empty round when nobody has throws`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val result = writer.write(eventCard(ResolvedPlayer("Ola", ola, listOf(null, null, null))))

        assertFalse(result.createdRound)
        assertNull(result.roundId)
        verify(roundRepository, never()).save(any<RoundEntity>())
        verify(importedCardRepository, never()).save(any<UdiscImportedCardEntity>())
    }

    @Test
    fun `rejects a card whose hole count differs from the existing round`() {
        val round = individualRound(eventId = event.id)
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundId(round.id)).thenReturn(holes(round, 2))

        val ex = assertThrows<ResponseStatusException> { writer.write(eventCard(ResolvedPlayer("Ola", ola, listOf(3, 3, 3)))) }
        assertEquals("Runde 1 har 2 hull, men UDisc-kortet har 3.", ex.reason)
    }

    @Test
    fun `rejects skipping a round number`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val ex = assertThrows<ResponseStatusException> {
            writer.write(eventCard(ResolvedPlayer("Ola", ola, listOf(3, 3, 3)), ordinal = 2))
        }
        assertEquals("Runde 2 finnes ikke ennå – importer runde 1 først.", ex.reason)
    }

    @Test
    fun `rejects team events`() {
        val teamEvent = event(teamEvent = true)
        whenever(eventRepository.findById(teamEvent.id)).thenReturn(Optional.of(teamEvent))

        assertThrows<ResponseStatusException> {
            writer.write(eventCard(ResolvedPlayer("Ola", ola, listOf(3, 3, 3))).copy(eventId = teamEvent.id))
        }
    }

    @Test
    fun `an event card that was already imported as a standalone round is refused`() {
        val standalone = individualRound(eventId = null)
        whenever(importedCardRepository.findById(KEY)).thenReturn(Optional.of(UdiscImportedCardEntity(KEY, standalone.id)))
        whenever(roundRepository.findById(standalone.id)).thenReturn(Optional.of(standalone))
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val ex = assertThrows<ResponseStatusException> { writer.write(eventCard(ResolvedPlayer("Ola", ola, listOf(3, 3, 3)))) }
        assertTrue(ex.reason!!.contains("frittstående"))
        verify(playerScoreRepository, never()).saveAll(any<Iterable<PlayerScoreEntity>>())
    }

    // ---- Standalone rounds ----------------------------------------------------------

    @Test
    fun `creates a standalone round with its own location and date`() {
        val result = writer.write(standaloneCard(ResolvedPlayer("Ola", ola, listOf(4, 3, 5))))

        assertTrue(result.createdRound)
        assertNull(result.eventId)
        val round = argumentCaptor<RoundEntity>().also { verify(roundRepository).save(it.capture()) }.firstValue
        assertNull(round.eventId)
        assertEquals(locationId, round.locationId)
        assertEquals(playedAt, round.playedAt)
        assertEquals(3, savedScores().size)
        verify(importedCardRepository).save(argThat<UdiscImportedCardEntity> { cardKey == KEY && roundId == round.id })
        verify(courseAliasRepository).save(argThat<UdiscCourseAliasEntity> { locationId == this@UdiscCardWriterTest.locationId })
    }

    @Test
    fun `re-importing a standalone card adds new players to the same round`() {
        val round = individualRound(eventId = null)
        val holes = holes(round, 3)
        whenever(importedCardRepository.findById(KEY)).thenReturn(Optional.of(UdiscImportedCardEntity(KEY, round.id)))
        whenever(roundRepository.findById(round.id)).thenReturn(Optional.of(round))
        whenever(holeRepository.findByRoundId(round.id)).thenReturn(holes)
        whenever(playerScoreRepository.findByHoleIdIn(any()))
            .thenReturn(holes.map { PlayerScoreEntity(holeId = it.id, playerId = seb, throws = 3) })

        val result = writer.write(
            standaloneCard(
                ResolvedPlayer("Sebastian", seb, listOf(3, 3, 3)),
                ResolvedPlayer("EB-maskin", ola, listOf(4, 4, 4)),
            ),
        )

        assertFalse(result.createdRound)
        assertEquals(round.id, result.roundId)
        assertEquals(listOf("EB-maskin"), result.importedPlayers)
        verify(roundRepository, never()).save(any<RoundEntity>())
        verify(importedCardRepository, never()).save(any<UdiscImportedCardEntity>())
    }

    @Test
    fun `a standalone card that was already imported into an event is refused`() {
        val eventRound = individualRound(eventId = event.id)
        whenever(importedCardRepository.findById(KEY)).thenReturn(Optional.of(UdiscImportedCardEntity(KEY, eventRound.id)))
        whenever(roundRepository.findById(eventRound.id)).thenReturn(Optional.of(eventRound))

        val ex = assertThrows<ResponseStatusException> { writer.write(standaloneCard(ResolvedPlayer("Ola", ola, listOf(3, 3, 3)))) }
        assertEquals("Kortet er allerede importert til «Tirsdagsrunde». Slett den runden først hvis det skal flyttes.", ex.reason)
    }

    // ---- Helpers ----------------------------------------------------------------------

    private fun savedScores() = argumentCaptor<Iterable<PlayerScoreEntity>>()
        .also { verify(playerScoreRepository).saveAll(it.capture()) }.firstValue.toList()

    private fun event(teamEvent: Boolean) = EventEntity(
        date = OffsetDateTime.parse("2026-10-28T18:00:00+01:00"),
        title = "Tirsdagsrunde",
        description = "",
        locationId = locationId,
        teamEvent = teamEvent,
        rounds = 1,
    )

    private fun individualRound(eventId: UUID?) = RoundEntity(
        eventId = eventId,
        eventType = EventType.individual,
        scoringFormat = ScoringFormat.stroke,
        locationId = if (eventId == null) locationId else null,
        playedAt = if (eventId == null) playedAt else null,
    )

    private fun holes(round: RoundEntity, count: Int) = (1..count).map { HoleEntity(roundId = round.id, holeNumber = it, par = 3) }

    private fun eventCard(vararg players: ResolvedPlayer, ordinal: Int = 1) = ResolvedCard(
        key = KEY,
        eventId = event.id,
        roundOrdinal = ordinal,
        locationId = null,
        playedAt = null,
        courseName = "Ekebergsletta Discgolfpark",
        pars = listOf(3, 3, 4),
        players = players.toList(),
    )

    private fun standaloneCard(vararg players: ResolvedPlayer) = ResolvedCard(
        key = KEY,
        eventId = null,
        roundOrdinal = null,
        locationId = locationId,
        playedAt = playedAt,
        courseName = "Ekebergsletta Discgolfpark",
        pars = listOf(3, 3, 4),
        players = players.toList(),
    )

    companion object {
        const val KEY = "Ekebergsletta Discgolfpark|Main|2026-10-28 1804+0100"
    }
}
