package no.disckos.backend.application.admin.player

import java.math.BigDecimal

data class CreatePlayerInput(
    val name: String,
    val gender: String,

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