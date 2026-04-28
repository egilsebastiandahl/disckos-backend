package no.disckos.backend.repository

import no.disckos.backend.domain.Profile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileRepository : JpaRepository<Profile, UUID> {
    fun findByPlayerId(playerId: UUID): Profile?
}
