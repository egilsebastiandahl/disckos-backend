package no.disckos.backend.api

import no.disckos.backend.api.dto.admin.player.CreatePlayerRequest
import no.disckos.backend.api.dto.admin.player.UpdatePlayerRequest
import no.disckos.backend.application.admin.player.CreatePlayerInput
import no.disckos.backend.application.admin.player.UpdatePlayerInput

fun CreatePlayerRequest.toInput(): CreatePlayerInput =
    CreatePlayerInput(
        name = name,
        gender = gender,
        catchphrase = catchphrase
    )

fun UpdatePlayerRequest.toInput(id: java.util.UUID): UpdatePlayerInput =
    UpdatePlayerInput(
        id = id,
        name = name,
        gender = gender,
        catchphrase = catchphrase
    )
