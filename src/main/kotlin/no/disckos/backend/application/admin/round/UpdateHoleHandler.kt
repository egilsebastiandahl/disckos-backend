package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.repository.HoleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Component
class UpdateHoleHandler(
    private val holeRepository: HoleRepository
) {
    @Transactional
    fun handle(
        roundId: UUID,
        holeNumber: Int,
        par: Int?,
        scoringFormatOverride: ScoringFormat?,
        clearScoringFormatOverride: Boolean?
    ): HoleEntity {
        val hole = holeRepository.findByRoundIdAndHoleNumber(roundId, holeNumber)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Hole not found")

        par?.let { hole.par = it }
        if (clearScoringFormatOverride == true) {
            hole.scoringFormatOverride = null
        } else if (scoringFormatOverride != null) {
            hole.scoringFormatOverride = scoringFormatOverride
        }

        return holeRepository.save(hole)
    }
}
