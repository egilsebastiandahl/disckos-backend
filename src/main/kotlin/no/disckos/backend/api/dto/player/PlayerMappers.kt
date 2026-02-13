package no.disckos.backend.api.dto.player

import no.disckos.backend.domain.Player

fun Player.toResponse(): PlayerResponse =
    PlayerResponse(
        id = requireNotNull(id),
        name = requireNotNull(name),
        gender = gender,
        roundsPlayed = roundsPlayed,
        averageScore = averageScore,
        bestScore = bestScore,
        worstScore = worstScore,
        singleBogeyCount = singleBogeyCount,
        doubleBogeyCount = doubleBogeyCount,
        tripleBogeyCount = tripleBogeyCount,
        parCount = parCount,
        birdieCount = birdieCount,
        aceCount = aceCount,
        eagleCount = eagleCount,
        throws = throwsField,
        catchphrase = catchphrase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
