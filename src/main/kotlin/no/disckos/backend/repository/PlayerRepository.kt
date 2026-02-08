package no.disckos.backend.repository
import no.disckos.backend.domain.Player
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlayerRepository : JpaRepository<Player, UUID>

