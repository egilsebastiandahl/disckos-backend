package no.disckos.backend.api.dto.admin.player

import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreatePlayerRequest(
    @field:NotNull
    var name: String,

    @field:NotNull
    var gender: String,

    var roundsPlayed: Int? = null,
    var averageScore: BigDecimal? = null,
    var bestScore: Int? = null,
    var worstScore: Int? = null,
    var singleBogeyCount: Int? = null,
    var doubleBogeyCount: Int? = null,
    var tripleBogeyCount: Int? = null,
    var parCount: Int? = null,
    var birdieCount: Int? = null,
    var aceCount: Int? = null,
    var eagleCount: Int? = null,
    var throws: Int? = null,
    var catchphrase: String? = null
)
