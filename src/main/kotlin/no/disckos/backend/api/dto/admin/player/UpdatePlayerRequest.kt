package no.disckos.backend.api.dto.admin.player

data class UpdatePlayerRequest(
    val name: String? = null,
    val gender: String? = null,
    val catchphrase: String? = null
)
