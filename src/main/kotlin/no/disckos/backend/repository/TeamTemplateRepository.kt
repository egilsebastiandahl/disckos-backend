package no.disckos.backend.repository

import no.disckos.backend.domain.TeamTemplateEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeamTemplateRepository : JpaRepository<TeamTemplateEntity, UUID>
