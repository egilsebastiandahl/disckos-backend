package no.disckos.backend.application.admin.player

import java.util.UUID

data class UpdatePlayerInput(
    val id: UUID,
    val name: String? = null,
    val gender: String? = null,
    val catchphrase: String? = null
)
