package no.disckos.backend.api.dto.admin.location

data class UpdateLocationRequest(
    val name: String? = null,
    val description: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)
