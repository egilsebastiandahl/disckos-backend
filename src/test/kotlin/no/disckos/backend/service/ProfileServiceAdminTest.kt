package no.disckos.backend.service

import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.ProfileRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileServiceAdminTest {

    private fun profile(admin: Boolean, owner: Boolean = false) = Profile().apply {
        id = UUID.randomUUID()
        isAdmin = admin
        isOwner = owner
    }

    private fun serviceWith(vararg profiles: Profile): Pair<ProfileService, ProfileRepository> {
        val byId = profiles.associateBy { it.id }
        val repo = mock<ProfileRepository> {
            on { findById(any()) } doAnswer { Optional.ofNullable(byId[it.arguments[0]]) }
            on { save(any<Profile>()) } doAnswer { it.arguments[0] as Profile }
        }
        return ProfileService(repo, mock<PlayerRepository>()) to repo
    }

    @Test
    fun `grants the admin role`() {
        val member = profile(admin = false)
        val (service, _) = serviceWith(member)

        assertEquals(true, service.setAdmin(member.id!!, true).isAdmin)
    }

    @Test
    fun `revokes the admin role`() {
        val admin = profile(admin = true)
        val (service, _) = serviceWith(admin)

        assertEquals(false, service.setAdmin(admin.id!!, false).isAdmin)
    }

    @Test
    fun `an owner's admin role can't be revoked`() {
        val owner = profile(admin = true, owner = true)
        val (service, repo) = serviceWith(owner)

        assertThrows<IllegalStateException> { service.setAdmin(owner.id!!, false) }
        assertTrue(owner.isAdmin == true)
        verify(repo, never()).save(any<Profile>())
    }

    @Test
    fun `granting an existing admin changes nothing`() {
        val admin = profile(admin = true)
        val (service, repo) = serviceWith(admin)

        service.setAdmin(admin.id!!, true)

        verify(repo, never()).save(any<Profile>())
    }

    @Test
    fun `an unknown profile is reported as not found`() {
        val (service, _) = serviceWith()

        assertThrows<NoSuchElementException> { service.setAdmin(UUID.randomUUID(), true) }
    }

    @Test
    fun `a new profile is not an owner`() {
        assertFalse(Profile().isOwner)
    }
}
