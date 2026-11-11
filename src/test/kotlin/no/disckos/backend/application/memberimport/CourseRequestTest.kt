package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.UdiscCard
import no.disckos.backend.application.admin.udiscimport.UdiscPlayerRow
import no.disckos.backend.domain.CourseRequestEntity
import no.disckos.backend.domain.CourseRequestStatus
import no.disckos.backend.domain.HeldUploadEntity
import no.disckos.backend.domain.HeldUploadScoreEntity
import no.disckos.backend.domain.HeldUploadStatus
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.repository.CourseRequestRepository
import no.disckos.backend.repository.HeldUploadRepository
import no.disckos.backend.repository.HeldUploadScoreRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseRequestTest {

    private val me = Uploader(UUID.randomUUID(), UUID.randomUUID(), "Sebastian", null)
    private val kari = UUID.randomUUID()
    private val start = LocalDateTime.of(2026, 10, 4, 14, 0)

    // In-memory tables behind the mocked repositories.
    private val requests = mutableListOf<CourseRequestEntity>()
    private val held = mutableListOf<HeldUploadEntity>()
    private val scores = mutableListOf<HeldUploadScoreEntity>()
    private val locations = mutableListOf(LocationEntity(name = "Holmenkollen DiscGolfpark", description = "", lat = 0.0, lon = 0.0))
    private val aliases = mutableListOf<UdiscCourseAliasEntity>()

    private val requestRepository = mock<CourseRequestRepository> {
        on { findByCourseKeyAndStatus(any(), any()) } doAnswer { inv -> requests.firstOrNull { it.courseKey == inv.arguments[0] && it.status == inv.arguments[1] } }
        on { save(any<CourseRequestEntity>()) } doAnswer { inv -> (inv.arguments[0] as CourseRequestEntity).also { r -> if (r !in requests) requests += r } }
        on { findById(any()) } doAnswer { inv -> Optional.ofNullable(requests.firstOrNull { it.id == inv.arguments[0] }) }
        on { delete(any<CourseRequestEntity>()) } doAnswer { inv -> requests.remove(inv.arguments[0]); Unit }
    }
    private val heldRepository = mock<HeldUploadRepository> {
        on { findByCardKeyAndPlayerId(any(), any()) } doAnswer { inv -> held.firstOrNull { it.cardKey == inv.arguments[0] && it.playerId == inv.arguments[1] } }
        on { save(any<HeldUploadEntity>()) } doAnswer { inv -> (inv.arguments[0] as HeldUploadEntity).also { h -> if (h !in held) held += h } }
        on { findById(any()) } doAnswer { inv -> Optional.ofNullable(held.firstOrNull { it.id == inv.arguments[0] }) }
        on { delete(any<HeldUploadEntity>()) } doAnswer { inv -> held.remove(inv.arguments[0]); Unit }
        on { findByCourseRequestIdAndStatus(any(), any()) } doAnswer { inv -> held.filter { it.courseRequestId == inv.arguments[0] && it.status == inv.arguments[1] } }
    }
    private val scoreRepository = mock<HeldUploadScoreRepository> {
        on { saveAll(any<List<HeldUploadScoreEntity>>()) } doAnswer { inv -> @Suppress("UNCHECKED_CAST") (inv.arguments[0] as List<HeldUploadScoreEntity>).also(scores::addAll) }
    }
    private val locationRepository = mock<LocationRepository> {
        on { findAll() } doAnswer { locations.toList() }
        on { save(any<LocationEntity>()) } doAnswer { inv -> (inv.arguments[0] as LocationEntity).also(locations::add) }
        on { findById(any()) } doAnswer { inv -> Optional.ofNullable(locations.firstOrNull { it.id == inv.arguments[0] }) }
    }
    private val aliasRepository = mock<UdiscCourseAliasRepository> {
        on { findByUdiscCourse(any()) } doAnswer { inv -> aliases.firstOrNull { it.udiscCourse == inv.arguments[0] } }
        on { save(any<UdiscCourseAliasEntity>()) } doAnswer { inv -> (inv.arguments[0] as UdiscCourseAliasEntity).also { a -> if (a !in aliases) aliases += a } }
    }

    private val service = CourseRequestService(
        requestRepository, heldRepository, scoreRepository, locationRepository, aliasRepository, mock<ProfileRepository>(), mock<MemberCardWriter>(),
    )

    private fun card(course: String = "Ny Bane DGP", key: String = "$course|Main|$start") = UdiscCard(
        key = key, courseName = course, layoutName = "Main", rawDate = start.toString(), startedAt = start,
        pars = listOf(3, 3, 4),
        players = listOf(UdiscPlayerRow("Seb D", listOf(2, 3, 4)), UdiscPlayerRow("Kari", listOf(3, 3, 5)), UdiscPlayerRow("Tante Grete", listOf(5, 5, 5))),
    )

    private fun newCoursePlan(card: UdiscCard = card()) = MemberCardPlan(
        card = card,
        status = MemberCardStatus.NEW_COURSE,
        ownThrows = listOf(2, 3, 4),
        others = listOf(
            OtherPlayerPlan("Kari", kari, "Kari Hansen", listOf(3, 3, 5), OtherPlayerStatus.ASK),
            OtherPlayerPlan("Tante Grete", null, null, listOf(5, 5, 5), OtherPlayerStatus.GUEST),
        ),
    )

    private fun status(ex: ResponseStatusException) = HttpStatus.valueOf(ex.statusCode.value())

    // ---- Holding ------------------------------------------------------------------------

    @Test
    fun `a round on a new course is held with the uploader's and club players' scores, never a guest's`() {
        val result = service.hold(newCoursePlan(), me, "Seb D")

        assertEquals("Ny Bane DGP", result.heldForCourse)
        assertNull(result.error)
        val request = requests.single()
        assertEquals(CourseRequestStatus.pending, request.status)
        assertEquals(me.profileId, request.requestedBy)
        val h = held.single()
        assertEquals(request.id, h.courseRequestId)
        assertEquals("3,3,4", h.pars)
        assertEquals(listOf("Seb D" to "2,3,4", "Kari" to "3,3,5"), scores.map { it.udiscName to it.throws })
    }

    @Test
    fun `more rounds on the same course join the same request`() {
        service.hold(newCoursePlan(card(key = "a")), me, "Seb D")
        service.hold(newCoursePlan(card(course = "  ny bane dgp ", key = "b")), me, "Seb D")

        assertEquals(1, requests.size)
        assertEquals(2, held.size)
    }

    @Test
    fun `the same round can't be held twice`() {
        service.hold(newCoursePlan(), me, "Seb D")

        val ex = assertThrows<ResponseStatusException> { service.hold(newCoursePlan(), me, "Seb D") }

        assertEquals(HttpStatus.CONFLICT, status(ex))
        assertEquals(1, held.size)
    }

    // ---- Deciding -----------------------------------------------------------------------

    @Test
    fun `creating the course makes a location and remembers the UDisc name for it`() {
        service.hold(newCoursePlan(), me, "Seb D")
        val request = requests.single()
        val admin = UUID.randomUUID()

        val location = service.createCourse(request.id, "  Ny   Bane ", admin)

        assertEquals("Ny Bane", location.name)
        assertEquals(CourseRequestStatus.created, request.status)
        assertEquals(location.id, request.locationId)
        assertEquals(admin, request.decidedBy)
        assertEquals(location.id, aliases.single { it.udiscCourse == "ny bane dgp" }.locationId)
    }

    @Test
    fun `a course that already exists can't be created again, only linked`() {
        service.hold(newCoursePlan(), me, "Seb D")

        val ex = assertThrows<ResponseStatusException> {
            service.createCourse(requests.single().id, "holmenkollen discgolfpark", null)
        }

        assertEquals(HttpStatus.CONFLICT, status(ex))
        assertEquals(CourseRequestStatus.pending, requests.single().status)
    }

    @Test
    fun `linking points the UDisc name at the existing course`() {
        service.hold(newCoursePlan(), me, "Seb D")
        val holmenkollen = locations.first()

        service.linkCourse(requests.single().id, holmenkollen.id, null)

        assertEquals(CourseRequestStatus.linked, requests.single().status)
        assertEquals(holmenkollen.id, aliases.single().locationId)
    }

    @Test
    fun `rejecting turns the waiting rounds away with a reason`() {
        service.hold(newCoursePlan(), me, "Seb D")

        val result = service.reject(requests.single().id, "Finnes ikke", null)

        assertEquals(CourseRequestStatus.rejected, requests.single().status)
        assertEquals(HeldUploadStatus.discarded, held.single().status)
        assertEquals("Banen «Ny Bane DGP» ble ikke godkjent: Finnes ikke", held.single().reason)
        assertEquals(0, result.released)
        assertTrue(aliases.isEmpty())
    }

    @Test
    fun `a decided course can't be decided again`() {
        service.hold(newCoursePlan(), me, "Seb D")
        service.reject(requests.single().id, null, null)

        val ex = assertThrows<ResponseStatusException> { service.linkCourse(requests.single().id, locations.first().id, null) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
    }

    // ---- Members taking rounds back -----------------------------------------------------

    @Test
    fun `you can only take back your own waiting rounds`() {
        service.hold(newCoursePlan(), me, "Seb D")

        val ex = assertThrows<ResponseStatusException> { service.removeMine(held.single().id, UUID.randomUUID()) }

        assertEquals(HttpStatus.FORBIDDEN, status(ex))
        assertEquals(1, held.size)
    }

    @Test
    fun `taking back the last round on a course drops the request`() {
        service.hold(newCoursePlan(), me, "Seb D")

        service.removeMine(held.single().id, me.profileId)

        assertTrue(held.isEmpty())
        assertTrue(requests.isEmpty())
    }

    // ---- Releasing ----------------------------------------------------------------------

    private val holmenkollen = UUID.randomUUID()
    private val heldCard = HeldCard(
        heldId = UUID.randomUUID(), profileId = me.profileId, playerId = me.playerId, uploaderName = "Seb D",
        card = card(course = "Holmenkollen DiscGolfpark").copy(players = card().players.take(2)),
    )

    private fun context(nearby: List<NearbyRound> = emptyList()) = MemberImportContext(
        uploaderPlayerId = me.playerId,
        players = mapOf(me.playerId to "Sebastian", kari to "Kari Hansen"),
        playerAliases = mapOf("kari" to kari),
        memberUdiscNames = emptyMap(),
        courseAliases = mapOf("holmenkollen discgolfpark" to holmenkollen),
        eventLocationsByDate = emptyMap(),
        importedCards = emptyMap(),
        nearbyRounds = nearby,
    )

    private fun releaser(uploader: Uploader?, ctx: MemberImportContext): Pair<HeldUploadReleaser, CourseRequestService> {
        val requestService = mock<CourseRequestService> { on { heldCards(any()) } doReturn listOf(heldCard) }
        val loader = mock<MemberImportContextLoader> {
            on { uploader(me.profileId) } doReturn uploader
            on { load(any(), any()) } doReturn ctx
        }
        return HeldUploadReleaser(requestService, loader) to requestService
    }

    @Test
    fun `once the course exists, a held round goes through the normal rules`() {
        val (releaser, requestService) = releaser(me, context())

        val result = releaser.release(UUID.randomUUID())

        assertEquals(1, result.released)
        verify(requestService).writeReleased(any(), eq(me), eq(heldCard.heldId))
        verify(requestService, never()).discard(any(), any())
    }

    @Test
    fun `a held round that reached Disckos another way is turned away with a reason`() {
        val sameRound = NearbyRound(UUID.randomUUID(), holmenkollen, start.plusMinutes(10), setOf(me.playerId))
        val (releaser, requestService) = releaser(me, context(nearby = listOf(sameRound)))

        val result = releaser.release(UUID.randomUUID())

        assertEquals(0, result.released)
        verify(requestService).discard(heldCard.heldId, "Runden var allerede i Disckos.")
        verify(requestService, never()).writeReleased(any(), any(), any())
    }

    @Test
    fun `a round from an account no longer linked to its player is turned away`() {
        val (releaser, requestService) = releaser(me.copy(playerId = UUID.randomUUID()), context())

        releaser.release(UUID.randomUUID())

        verify(requestService).discard(eq(heldCard.heldId), any())
        verify(requestService, never()).writeReleased(any(), any(), any())
    }
}
