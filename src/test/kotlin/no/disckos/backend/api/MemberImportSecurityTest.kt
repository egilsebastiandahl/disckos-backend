package no.disckos.backend.api

import no.disckos.backend.application.memberimport.MemberImportContextLoader
import no.disckos.backend.application.memberimport.MemberImportHandler
import no.disckos.backend.application.memberimport.MemberUploadService
import no.disckos.backend.application.memberimport.ScoreConfirmationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID

/** Member endpoints need a signed-in member; the admin side needs an admin. */
@SpringJUnitConfig(MemberImportSecurityTest.Config::class)
class MemberImportSecurityTest {

    @Configuration
    @EnableMethodSecurity
    class Config {
        @Bean fun uploadService() = mock<MemberUploadService>()
        @Bean fun confirmationService() = mock<ScoreConfirmationService>()

        @Bean
        fun memberController(uploads: MemberUploadService, confirmations: ScoreConfirmationService) =
            MemberImportController(mock<MemberImportHandler>(), uploads, confirmations, mock<MemberImportContextLoader>())

        @Bean
        fun adminController(uploads: MemberUploadService, confirmations: ScoreConfirmationService) =
            AdminMemberUploadController(uploads, confirmations)
    }

    @Autowired private lateinit var member: MemberImportController
    @Autowired private lateinit var admin: AdminMemberUploadController
    @Autowired private lateinit var uploads: MemberUploadService

    private val profileId = UUID.randomUUID()

    /** As SupabaseJwtAuthenticationFilter does it: the principal is the profile id. */
    private fun signIn(vararg roles: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(profileId.toString(), null, roles.map { SimpleGrantedAuthority("ROLE_$it") })
    }

    @AfterEach
    fun signOut() = SecurityContextHolder.clearContext()

    @Test
    fun `a signed-in member sees and undoes only through their own profile`() {
        signIn("USER")

        member.myUploads()
        member.undoUpload(UUID.randomUUID())

        verify(uploads).mine(profileId)
        verify(uploads).undo(any(), eq(profileId), eq(false))
    }

    @Test
    fun `without the member role nothing is allowed`() {
        signIn()

        assertThrows<AccessDeniedException> { member.myUploads() }
        assertThrows<AccessDeniedException> { member.confirm(UUID.randomUUID()) }
    }

    @Test
    fun `members can't use the admin side`() {
        signIn("USER")

        assertThrows<AccessDeniedException> { admin.recentUploads() }
        assertThrows<AccessDeniedException> { admin.confirm(UUID.randomUUID()) }
        assertThrows<AccessDeniedException> { admin.undoUpload(UUID.randomUUID()) }
    }

    @Test
    fun `admins act as admins`() {
        signIn("USER", "admin")

        admin.undoUpload(UUID.randomUUID())

        verify(uploads).undo(any(), eq(profileId), eq(true))
    }
}
