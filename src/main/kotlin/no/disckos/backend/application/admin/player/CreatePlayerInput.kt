package no.disckos.backend.application.admin.player

data class CreatePlayerInput(
    val name: String,
    val gender: String,
    var catchphrase: String? = null
)
