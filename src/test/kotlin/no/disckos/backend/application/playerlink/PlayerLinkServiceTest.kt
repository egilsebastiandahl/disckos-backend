package no.disckos.backend.application.playerlink

import no.disckos.backend.domain.LinkRequestStatus
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerLinkRequestEntity
import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.PlayerLinkRequestRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.ProfileRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlayerLinkServiceTest {

    private val kari = player("Kari Hansen")
    private val ola = player("Ola Nordmann")
    private val players = mutableListOf(kari, ola)

    private val newcomer = profile()
    private val olaAccount = profile().apply { player = ola }
    private val profiles = mutableListOf(newcomer, olaAccount)
    private var pending: PlayerLinkRequestEntity? = null

    private val profileRepository = mock<ProfileRepository> {
        on { findById(any()) } doAnswer { inv -> Optional.ofNullable(profiles.firstOrNull { it.id == inv.arguments[0] }) }
        on { findByPlayerId(any()) } doAnswer { inv -> profiles.firstOrNull { it.player?.id == inv.arguments[0] } }
        on { findAll() } doAnswer { profiles.toList() }
        on { save(any<Profile>()) } doAnswer { it.arguments[0] as Profile }
    }
    private val playerRepository = mock<PlayerRepository> {
        on { findAll() } doAnswer { players.toList() }
        on { findById(any()) } doAnswer { inv -> Optional.ofNullable(players.firstOrNull { it.id == inv.arguments[0] }) }
        on { save(any<Player>()) } doAnswer { (it.arguments[0] as Player).also(players::add) }
    }
    private val requestRepository = mock<PlayerLinkRequestRepository> {
        on { findByProfileIdAndStatus(any(), any()) } doAnswer { inv ->
            pending?.takeIf { it.profileId == inv.arguments[0] && it.status == inv.arguments[1] }
        }
        on { save(any<PlayerLinkRequestEntity>()) } doAnswer { (it.arguments[0] as PlayerLinkRequestEntity).also { r -> pending = r } }
        on { findById(any()) } doAnswer { inv -> Optional.ofNullable(pending?.takeIf { it.id == inv.arguments[0] }) }
    }
    private val service = PlayerLinkService(profileRepository, playerRepository, requestRepository, mock<PlayerScoreRepository>())

    private fun player(name: String) = Player().apply {
        id = UUID.randomUUID()
        this.name = name
    }

    private fun profile() = Profile().apply { id = UUID.randomUUID() }

    private fun status(ex: ResponseStatusException) = HttpStatus.valueOf(ex.statusCode.value())

    // ---- Creating your own player -------------------------------------------------------

    @Test
    fun `a new member creates their own player and is linked at once`() {
        val result = service.createOwnPlayer(newcomer.id!!, "  Per   Olsen ", "male")

        assertEquals("Per Olsen", result.playerName)
        assertEquals("Per Olsen", newcomer.player?.name)
        assertEquals("male", newcomer.player?.gender)
    }

    @Test
    fun `you can't create a player that already exists, you have to claim it`() {
        val ex = assertThrows<ResponseStatusException> { service.createOwnPlayer(newcomer.id!!, "kari hansen", null) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
        assertNull(newcomer.player)
        verify(playerRepository, never()).save(any<Player>())
    }

    @Test
    fun `you can't create a second player once you have one`() {
        val ex = assertThrows<ResponseStatusException> { service.createOwnPlayer(olaAccount.id!!, "Ola Two", null) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
    }

    @Test
    fun `an unknown gender is stored as other`() {
        service.createOwnPlayer(newcomer.id!!, "Per Olsen", "<script>")

        assertEquals("other", newcomer.player?.gender)
    }

    // ---- Claiming an existing player ----------------------------------------------------

    @Test
    fun `claiming an existing player waits for an admin`() {
        val result = service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)

        assertNull(newcomer.player)
        assertEquals(kari.id, result.pendingRequest?.playerId)
    }

    @Test
    fun `asking for another player replaces the open request`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)
        val first = pending

        val unclaimed = player("Per Olsen").also(players::add)
        service.requestExisting(newcomer.id!!, unclaimed.id!!, asAdmin = false)

        assertEquals(first?.id, pending?.id)
        assertEquals(unclaimed.id, pending?.playerId)
    }

    @Test
    fun `a player linked to someone else can't be claimed`() {
        val ex = assertThrows<ResponseStatusException> { service.requestExisting(newcomer.id!!, ola.id!!, asAdmin = false) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
        assertNull(pending)
    }

    @Test
    fun `an admin is linked straight away`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = true)

        assertEquals(kari.id, newcomer.player?.id)
        assertNull(pending)
    }

    // ---- The admin's decision -----------------------------------------------------------

    @Test
    fun `approving links the member`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)
        val admin = UUID.randomUUID()

        service.decide(pending!!.id, approve = true, adminProfileId = admin)

        assertEquals(kari.id, newcomer.player?.id)
        assertEquals(LinkRequestStatus.approved, pending?.status)
        assertEquals(admin, pending?.decidedBy)
    }

    @Test
    fun `rejecting leaves the member unlinked`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)

        service.decide(pending!!.id, approve = false, adminProfileId = UUID.randomUUID())

        assertNull(newcomer.player)
        assertEquals(LinkRequestStatus.rejected, pending?.status)
    }

    @Test
    fun `a player someone else got in the meantime can't be approved`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)
        profiles += profile().apply { player = kari }

        val ex = assertThrows<ResponseStatusException> { service.decide(pending!!.id, approve = true, adminProfileId = null) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
        assertNull(newcomer.player)
    }

    @Test
    fun `a decided request can't be decided again`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)
        service.decide(pending!!.id, approve = false, adminProfileId = null)

        val ex = assertThrows<ResponseStatusException> { service.decide(pending!!.id, approve = true, adminProfileId = null) }

        assertEquals(HttpStatus.CONFLICT, status(ex))
    }

    // ---- Name check ---------------------------------------------------------------------

    @Test
    fun `the name check finds the same name and names that look alike either way`() {
        val check = service.checkName("kari")

        assertNull(check.exact)
        assertEquals(listOf("Kari Hansen"), check.similar.map { it.name })

        val longer = service.checkName("Ola Nordmann Jr")
        assertEquals(listOf("Ola Nordmann"), longer.similar.map { it.name })
        assertEquals(true, longer.similar.single().claimed)

        assertNotNull(service.checkName("Kari  Hansen").exact)
    }

    @Test
    fun `a request is saved for the asking member`() {
        service.requestExisting(newcomer.id!!, kari.id!!, asAdmin = false)

        val saved = argumentCaptor<PlayerLinkRequestEntity>().also { verify(requestRepository).save(it.capture()) }.firstValue
        assertEquals(newcomer.id, saved.profileId)
        assertEquals(LinkRequestStatus.pending, saved.status)
    }
}
