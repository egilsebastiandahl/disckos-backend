package no.disckos.backend.api.dto.player

import no.disckos.backend.domain.Player

fun Player.toResponse(): PlayerResponse =
    PlayerResponse(
        id = requireNotNull(id),
        name = requireNotNull(name),
        gender = gender,
        catchphrase = catchphrase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
