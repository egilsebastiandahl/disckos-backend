package no.disckos.backend.application.admin.location

data class UpdateLocationInput(
    val name: String? = null,
    val description: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)
