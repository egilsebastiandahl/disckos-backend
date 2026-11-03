package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UdiscImportPreviewHandlerTest {

    private val eventRepository = mock<EventRepository>()
    private val locationRepository = mock<LocationRepository>()
    private val playerRepository = mock<PlayerRepository>()
    private val roundRepository = mock<RoundRepository>()
    private val holeRepository = mock<HoleRepository>()
    private val playerScoreRepository = mock<PlayerScoreRepository>()
    private val playerAliasRepository = mock<UdiscPlayerAliasRepository>()
    private val courseAliasRepository = mock<UdiscCourseAliasRepository>()

    private val handler = UdiscImportPreviewHandler(
        eventRepository, locationRepository, playerRepository, roundRepository,
        holeRepository, playerScoreRepository, playerAliasRepository, courseAliasRepository,
    )

    private val ekeberg = LocationEntity(name = "Ekeberg Discgolfpark", description = "", lat = 0.0, lon = 0.0)
    private val vollen = LocationEntity(name = "Vollen", description = "", lat = 0.0, lon = 0.0)
    private val tuesday = EventEntity(
        date = OffsetDateTime.parse("2026-10-28T17:00:00Z"),
        title = "Tirsdagsrunde",
        description = "",
        locationId = ekeberg.id,
        teamEvent = false,
        rounds = 1,
    )
    private val seb = player("Sebastian Dahl")
    private val ola = player("Ola Nordmann")

    // One card on the event day, one casual round on a day with no event.
    private val csv = """
        PlayerName,CourseName,LayoutName,StartDate,EndDate,Total,+/-,RoundRating,Hole1,Hole2,Hole3
        Par,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,9,,,3,3,3
        Seb D,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,9,,,3,3,3
        ola nordmann,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,10,,,3,4,3
        Gjest,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,12,,,4,4,4
        Par,Ekebergsletta Discgolfpark,Main,2026-10-30 1200,2026-10-30 1300,9,,,3,3,3
        Seb D,Ekebergsletta Discgolfpark,Main,2026-10-30 1200,2026-10-30 1300,9,,,3,3,3
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        whenever(eventRepository.findAll()).thenReturn(listOf(tuesday))
        whenever(locationRepository.findAll()).thenReturn(listOf(ekeberg, vollen))
        whenever(playerRepository.findAll()).thenReturn(listOf(seb, ola))
    }

    @Test
    fun `keeps only cards played on an event day and suggests that event`() {
        val preview = handler.handle(csv)

        assertEquals(2, preview.cardsInFile)
        val card = preview.cards.single()
        assertEquals(tuesday.id, card.suggestedEventId)
        assertEquals(LocationMatch.NAME, card.eventOptions.single().locationMatch)
        assertEquals(1, card.suggestedRoundOrdinal)
        assertTrue(card.selected)
    }

    @Test
    fun `matches players by exact name and by nickname, leaves unknowns open`() {
        val players = handler.handle(csv).cards.single().players

        assertEquals(listOf(PlayerMatch.GUESS, PlayerMatch.EXACT, PlayerMatch.NONE), players.map { it.match })
        assertEquals(listOf(seb.id, ola.id, null), players.map { it.suggestedPlayerId })
    }

    @Test
    fun `remembered aliases win`() {
        whenever(playerAliasRepository.findAll()).thenReturn(listOf(UdiscPlayerAliasEntity(udiscName = "gjest", playerId = ola.id!!)))
        whenever(courseAliasRepository.findAll()).thenReturn(listOf(UdiscCourseAliasEntity(udiscCourse = "ekebergsletta discgolfpark", locationId = ekeberg.id)))

        val card = handler.handle(csv).cards.single()

        assertEquals(PlayerMatch.ALIAS, card.players[2].match)
        assertEquals(ola.id, card.players[2].suggestedPlayerId)
        assertEquals(LocationMatch.ALIAS, card.eventOptions.single().locationMatch)
    }

    @Test
    fun `a course linked to another location is flagged and not preselected`() {
        whenever(courseAliasRepository.findAll()).thenReturn(listOf(UdiscCourseAliasEntity(udiscCourse = "ekebergsletta discgolfpark", locationId = vollen.id)))

        val card = handler.handle(csv).cards.single()

        assertEquals(LocationMatch.OTHER_LOCATION, card.eventOptions.single().locationMatch)
        assertFalse(card.selected)
    }

    @Test
    fun `a card that is already imported is not preselected`() {
        val round = RoundEntity(eventId = tuesday.id, eventType = EventType.individual, scoringFormat = ScoringFormat.stroke)
        val holes = (1..3).map { HoleEntity(roundId = round.id, holeNumber = it, par = 3) }
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(tuesday.id)).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundIdIn(any())).thenReturn(holes)
        whenever(playerScoreRepository.findByHoleIdIn(any())).thenReturn(
            listOf(seb, ola).map { PlayerScoreEntity(holeId = holes[0].id, playerId = it.id!!, throws = 3) },
        )

        val card = handler.handle(csv).cards.single()

        assertEquals(setOf(seb.id, ola.id), card.eventOptions.single().rounds.single().playerIds)
        assertFalse(card.selected)
    }

    @Test
    fun `matches a real UDisc export to its event`() {
        val muselunden = LocationEntity(name = "Muselunden", description = "", lat = 0.0, lon = 0.0)
        val saturday = EventEntity(
            date = OffsetDateTime.parse("2026-10-31T13:00:00+01:00"),
            title = "Høstcupen",
            description = "",
            locationId = muselunden.id,
            teamEvent = false,
            rounds = 1,
        )
        whenever(eventRepository.findAll()).thenReturn(listOf(tuesday, saturday))
        whenever(locationRepository.findAll()).thenReturn(listOf(ekeberg, muselunden))

        val preview = handler.handle(
            """
            PlayerName,CourseName,LayoutName,StartDate,EndDate,Total,+/-,RoundRating,Hole1,Hole2,Hole3,Hole4,Hole5,Hole6,Hole7,Hole8,Hole9,Hole10,Hole11,Hole12
            Par,Muselunden DiscGolfPark,Muselunden 12 hull (2022),2026-10-31 1411+0100,2026-10-31 1435+0100,36,,,3,3,3,3,3,3,3,3,3,3,3,3
            Sebastian,Muselunden DiscGolfPark,Muselunden 12 hull (2022),2026-10-31 1411+0100,2026-10-31 1435+0100,29,-7,213,3,3,2,3,2,3,2,2,2,3,2,2
            """.trimIndent(),
        )

        val card = preview.cards.single()
        assertEquals(saturday.id, card.suggestedEventId)
        assertEquals(LocationMatch.NAME, card.eventOptions.single().locationMatch)
        assertEquals(12, card.pars.size)
        assertEquals(seb.id, card.players.single().suggestedPlayerId, "\"Sebastian\" → Sebastian Dahl")
        assertEquals(PlayerMatch.GUESS, card.players.single().match)
        assertTrue(card.selected)
    }

    @Test
    fun `a file with no event-day rounds returns no cards`() {
        whenever(eventRepository.findAll()).thenReturn(emptyList())

        val preview = handler.handle(csv)

        assertEquals(2, preview.cardsInFile)
        assertTrue(preview.cards.isEmpty())
    }

    private fun player(name: String) = Player().apply {
        id = UUID.randomUUID()
        this.name = name
    }
}
