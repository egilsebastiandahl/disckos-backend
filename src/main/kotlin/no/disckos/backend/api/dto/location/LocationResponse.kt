package no.disckos.backend.api.dto.location

import java.util.UUID

data class LocationResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double
)
