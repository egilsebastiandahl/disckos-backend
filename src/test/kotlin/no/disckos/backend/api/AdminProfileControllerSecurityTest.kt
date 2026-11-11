package no.disckos.backend.api

import no.disckos.backend.domain.Profile
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.service.ProfileService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID

/**
 * Only owners may give or revoke the admin role. The controller is admin-only at class level,
 * so this checks that the owner rule on the two methods wins over it.
 */
@SpringJUnitConfig(AdminProfileControllerSecurityTest.Config::class)
class AdminProfileControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    class Config {
        @Bean
        fun profileService(): ProfileService = mock {
            on { setAdmin(any(), any()) } doReturn Profile().apply { id = UUID.randomUUID() }
        }

        @Bean
        fun controller(profileService: ProfileService) = AdminProfileController(mock<ProfileRepository>(), profileService)
    }

    @Autowired
    private lateinit var controller: AdminProfileController

    private val someone = UUID.randomUUID()

    @Test
    @WithMockUser(roles = ["admin"])
    fun `an admin who is not an owner can't grant or revoke admin`() {
        assertThrows<AccessDeniedException> { controller.grantAdmin(someone) }
        assertThrows<AccessDeniedException> { controller.revokeAdmin(someone) }
    }

    @Test
    @WithMockUser(roles = ["admin", "owner"])
    fun `an owner can grant and revoke admin`() {
        controller.grantAdmin(someone)
        controller.revokeAdmin(someone)
    }

    @Test
    @WithMockUser(roles = ["admin"])
    fun `an admin still sees the profiles`() {
        controller.getAllProfiles()
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `a signed-in member can't see the profiles`() {
        assertThrows<AccessDeniedException> { controller.getAllProfiles() }
    }
}
