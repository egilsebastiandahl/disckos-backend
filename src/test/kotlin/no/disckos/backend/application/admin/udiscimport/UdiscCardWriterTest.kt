package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
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

    private val writer = UdiscCardWriter(
        eventRepository, roundRepository, holeRepository, playerRepository,
        playerScoreRepository, playerAliasRepository, courseAliasRepository,
    )

    private val locationId = UUID.randomUUID()
    private val event = event(teamEvent = false)
    private val seb = UUID.randomUUID()
    private val ola = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        whenever(eventRepository.findById(event.id)).thenReturn(Optional.of(event))
        whenever(playerRepository.findAllById(any())).thenAnswer { inv ->
            (inv.arguments[0] as Iterable<*>).map { id -> Player().apply { this.id = id as UUID; name = "x" } }
        }
        whenever(roundRepository.save(any<RoundEntity>())).thenAnswer { it.arguments[0] }
        whenever(holeRepository.saveAll(any<Iterable<HoleEntity>>())).thenAnswer { (it.arguments[0] as Iterable<*>).toList() }
    }

    @Test
    fun `creates the round with holes and scores when it doesn't exist yet`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val result = writer.write(
            card(
                ResolvedPlayer("Seb D", seb, listOf(3, 2, null)),
                ResolvedPlayer("Ola", ola, listOf(4, 3, 5)),
            ),
        )

        assertTrue(result.createdRound)
        assertNull(result.error)
        assertEquals(listOf("Seb D", "Ola"), result.importedPlayers)

        val holes = argumentCaptor<Iterable<HoleEntity>>().also { verify(holeRepository).saveAll(it.capture()) }.firstValue.toList()
        assertEquals(listOf(1, 2, 3), holes.map { it.holeNumber })
        assertEquals(listOf(3, 3, 4), holes.map { it.par })
        assertTrue(holes.all { it.roundId == result.roundId })

        val scores = argumentCaptor<Iterable<PlayerScoreEntity>>()
            .also { verify(playerScoreRepository).saveAll(it.capture()) }.firstValue.toList()
        assertEquals(5, scores.size, "Seb's unplayed hole 3 is left out")
        assertEquals(listOf(3, 2), scores.filter { it.playerId == seb }.map { it.throws })
    }

    @Test
    fun `remembers player and course links for next time`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        writer.write(card(ResolvedPlayer("  Seb  D ", seb, listOf(3, 3, 3))))

        verify(playerAliasRepository).save(argThat<UdiscPlayerAliasEntity> { udiscName == "seb d" && playerId == seb })
        verify(courseAliasRepository).save(
            argThat<UdiscCourseAliasEntity> { udiscCourse == "ekebergsletta discgolfpark" && locationId == this@UdiscCardWriterTest.locationId },
        )
    }

    @Test
    fun `appends to the existing round and skips players already in it`() {
        val round = RoundEntity(eventId = event.id, eventType = EventType.individual, scoringFormat = ScoringFormat.stroke)
        val holes = (1..3).map { HoleEntity(roundId = round.id, holeNumber = it, par = 3) }
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundId(round.id)).thenReturn(holes.reversed())
        whenever(playerScoreRepository.findByHoleIdIn(any()))
            .thenReturn(listOf(PlayerScoreEntity(holeId = holes[0].id, playerId = seb, throws = 3)))

        val result = writer.write(
            card(
                ResolvedPlayer("Seb D", seb, listOf(3, 3, 3)),
                ResolvedPlayer("Ola", ola, listOf(4, 3, 5)),
            ),
        )

        assertFalse(result.createdRound)
        assertEquals(round.id, result.roundId)
        assertEquals(listOf("Ola"), result.importedPlayers)
        assertEquals(listOf("Seb D (allerede i runden)"), result.skippedPlayers)
        verify(roundRepository, never()).save(any<RoundEntity>())

        val scores = argumentCaptor<Iterable<PlayerScoreEntity>>()
            .also { verify(playerScoreRepository).saveAll(it.capture()) }.firstValue.toList()
        assertEquals(holes.map { it.id }, scores.map { it.holeId }, "scores land on holes 1..3 in order")
        assertTrue(scores.all { it.playerId == ola })
    }

    @Test
    fun `does not create an empty round when nobody has throws`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val result = writer.write(card(ResolvedPlayer("Ola", ola, listOf(null, null, null))))

        assertFalse(result.createdRound)
        assertNull(result.roundId)
        verify(roundRepository, never()).save(any<RoundEntity>())
    }

    @Test
    fun `rejects a card whose hole count differs from the existing round`() {
        val round = RoundEntity(eventId = event.id, eventType = EventType.individual, scoringFormat = ScoringFormat.stroke)
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundId(round.id)).thenReturn((1..2).map { HoleEntity(roundId = round.id, holeNumber = it, par = 3) })

        val ex = assertThrows<ResponseStatusException> { writer.write(card(ResolvedPlayer("Ola", ola, listOf(3, 3, 3)))) }
        assertEquals("Runde 1 har 2 hull, men UDisc-kortet har 3.", ex.reason)
    }

    @Test
    fun `rejects skipping a round number`() {
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)).thenReturn(emptyList())

        val ex = assertThrows<ResponseStatusException> {
            writer.write(card(ResolvedPlayer("Ola", ola, listOf(3, 3, 3)), ordinal = 2))
        }
        assertEquals("Runde 2 finnes ikke ennå – importer runde 1 først.", ex.reason)
    }

    @Test
    fun `rejects team events`() {
        val teamEvent = event(teamEvent = true)
        whenever(eventRepository.findById(teamEvent.id)).thenReturn(Optional.of(teamEvent))

        assertThrows<ResponseStatusException> {
            writer.write(card(ResolvedPlayer("Ola", ola, listOf(3, 3, 3))).copy(eventId = teamEvent.id))
        }
    }

    private fun event(teamEvent: Boolean) = EventEntity(
        date = OffsetDateTime.parse("2026-10-28T18:00:00+01:00"),
        title = "Tirsdagsrunde",
        description = "",
        locationId = locationId,
        teamEvent = teamEvent,
        rounds = 1,
    )

    private fun card(vararg players: ResolvedPlayer, ordinal: Int = 1) = ResolvedCard(
        key = "Ekebergsletta Discgolfpark|Main|2026-10-28 1804",
        eventId = event.id,
        roundOrdinal = ordinal,
        courseName = "Ekebergsletta Discgolfpark",
        pars = listOf(3, 3, 4),
        players = players.toList(),
    )
}
