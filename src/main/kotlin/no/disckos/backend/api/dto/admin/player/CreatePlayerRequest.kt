package no.disckos.backend.api.dto.admin.player

import jakarta.validation.constraints.NotNull

data class CreatePlayerRequest(
    @field:NotNull
    var name: String,

    @field:NotNull
    var gender: String,

    var catchphrase: String? = null
)
