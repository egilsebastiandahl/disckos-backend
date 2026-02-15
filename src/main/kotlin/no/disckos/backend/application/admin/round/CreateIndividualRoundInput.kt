package no.disckos.backend.application.admin.round

import no.disckos.backend.domain.ScoringFormat
import java.util.UUID

data class CreateIndividualRoundInput(
    val eventId: UUID,
    val scoringFormat: ScoringFormat,
    val holes: List<IndividualHoleInput>
)

data class UpdateIndividualRoundInput(
    val scoringFormat: ScoringFormat,
    val holes: List<IndividualHoleInput>
)

data class IndividualHoleInput(
    val holeNumber: Int,
    val par: Int,
    val scoringFormatOverride: ScoringFormat?,
    val playerScores: List<PlayerScoreInput>
)

data class PlayerScoreInput(
    val playerId: UUID,
    val throws: Int
)
