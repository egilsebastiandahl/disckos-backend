package no.disckos.backend.api.dto.admin.location

import java.util.UUID

data class AdminLocationResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double
)
